package com.fbupdatetool.service;

import com.fbupdatetool.model.FriendlyError;
import com.fbupdatetool.util.ChecksumUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ScriptExecutor {

    private static final Logger logger = LoggerFactory.getLogger(ScriptExecutor.class);

    // --- Componentes Especialistas ---
    private final ScriptParser parser;
    private final ScriptIdentity identity;
    private final ScriptSanitizer sanitizer;
    private final MigrationStrategy strategy;

    // --- Serviços de Suporte ---
    private final HistoryService historyService;
    private final DatabaseChangeTracker tracker;
    private final FirebirdErrorTranslator errorTranslator;
    private final SecurityCallback securityCallback;

    private final List<DeferredCommand> deferredCommands = new ArrayList<>();

    private static class DeferredCommand {
        String sql;
        String fileName;
        public DeferredCommand(String sql, String fileName) { this.sql = sql; this.fileName = fileName; }
    }

    public ScriptExecutor(SecurityCallback securityCallback) {
        // Inicializa os especialistas
        this.parser = new ScriptParser();
        this.identity = new ScriptIdentity();
        this.sanitizer = new ScriptSanitizer();
        this.strategy = new MigrationStrategy(); // O Strategy já instancia o Introspector internamente

        // Inicializa serviços
        this.historyService = new HistoryService();
        this.tracker = new DatabaseChangeTracker();
        this.errorTranslator = new FirebirdErrorTranslator();
        this.securityCallback = securityCallback;
    }

    public ScriptExecutor() { this(command -> false); }

    public DatabaseChangeTracker getTracker() { return tracker; }

    /**
     * FLUXO PRINCIPAL:
     * 1. Lê Arquivo
     * 2. Identifica Tipo
     * 3. Quebra em Comandos
     * 4. Executa cada comando (Sanitizando e Planejando)
     */
    public boolean executeScript(Connection conn, Path scriptPath) {
        String fileName = scriptPath.getFileName().toString();

        try {
            // 1. Leitura Física (Encoding) - Responsabilidade do Parser
            String rawContent = parser.readContent(scriptPath);
            if (rawContent.trim().isEmpty()) {
                logger.warn("Arquivo vazio: {}", fileName);
                return true;
            }

            // 2. Análise de Identidade (Precisa de SET TERM?) - Responsabilidade do Identity
            ScriptIdentity.ScriptAnalysis analysis = identity.analyze(rawContent, fileName);
            logger.info(">>> [EXEC] Iniciando: {} (Tipo: {})", fileName, analysis.getType());

            // 3. Cálculo de Hash (para histórico)
            String fileHash = null;
            try { fileHash = ChecksumUtil.calculateHash(scriptPath); } catch (Exception e) {}

            // 4. Quebra em Comandos SQL (Usando o conteúdo seguro) - Responsabilidade do Parser
            List<String> commands = parser.parsePreparedContent(analysis.getContentSafe());

            if (commands.isEmpty()) {
                logger.warn("Nenhum comando executável encontrado em: {}", fileName);
                return true;
            }

            // 5. Execução Lógica
            boolean mainExecutionSuccess = executeCommandList(conn, commands, fileName);

            // 6. Finalização e Histórico
            if (mainExecutionSuccess) {
                try {
                    if (!historyService.isScriptExecuted(conn, fileName, fileHash)) {
                        historyService.markAsExecuted(conn, fileName, fileHash);
                    }
                    if (!conn.getAutoCommit()) conn.commit();
                } catch (Exception e) {
                    logger.error("❌ Erro ao salvar histórico: {}", e.getMessage());
                    try { conn.rollback(); } catch (SQLException ex) {}
                    return false;
                }
                logger.info("Script {} finalizado com SUCESSO.", fileName);
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            logger.error("Erro critico em {}: {}", fileName, e.getMessage());
            return false;
        }
    }

    public void processDeferredQueue(Connection conn) {
        if (deferredCommands.isEmpty()) return;
        logger.info("=== PROCESSANDO FILA DE DEPENDENCIAS ({} itens) ===", deferredCommands.size());
        boolean progress;
        for (int i = 1; i <= 20; i++) {
            if (deferredCommands.isEmpty()) break;
            progress = false;
            Iterator<DeferredCommand> it = deferredCommands.iterator();
            while (it.hasNext()) {
                DeferredCommand def = it.next();

                // Na fila, também passamos pelo fluxo de Sanitização -> Planejamento -> Execução
                // Mas de forma simplificada, assumindo que se está na fila, é execução direta
                String sqlLimpo = sanitizer.sanitize(def.sql);
                if (tentaRodarDireto(conn, sqlLimpo, true)) {
                    it.remove(); progress = true;
                }
            }
            try { if(!conn.getAutoCommit()) conn.commit(); } catch(Exception e) {}
            if (!progress) break;
        }
    }

    private boolean executeCommandList(Connection conn, List<String> commands, String fileName) {
        int cmdIndex = 0;
        for (String cmd : commands) {
            cmdIndex++;
            String cmdRaw = cmd.trim();
            if (cmdRaw.isEmpty()) continue;

            // Filtros de Segurança / Controle (Ainda cabe ao Executor decidir se roda ou não)
            if (isConfigurationCommand(cmdRaw)) continue;
            if (isTransactionControl(cmdRaw)) continue;
            if (isForbidden(cmdRaw)) {
                if (!securityCallback.requestAdminPermission(cmdRaw)) return false;
            }

            logger.info("  -> Cmd #{}: Analisando...", cmdIndex);

            // PASSO A: Sanitização (Responsabilidade do Sanitizer)
            String cmdSanitizado = sanitizer.sanitize(cmdRaw);

            // PASSO B: Planejamento (Responsabilidade do Strategy + Introspector)
            // O Strategy decide: Retorna vazio (Skip), o próprio comando, ou lista de Alters (Smart Merge)
            List<String> comandosReais = strategy.planExecution(conn, cmdSanitizado);

            if (comandosReais.isEmpty()) {
                // Se a lista veio vazia, o Strategy decidiu que não precisa fazer nada (Objeto já existe)
                // O log detalhado já foi feito dentro do Strategy
                continue;
            }

            // PASSO C: Execução Real (Responsabilidade do Executor + JDBC)
            for (String sqlReal : comandosReais) {
                if (!tentaRodar(conn, sqlReal, fileName, false)) {
                    return false; // Falha interrompe o script
                }
            }
        }
        return true;
    }

    /**
     * Tenta executar um comando SQL no banco e trata erros.
     */
    private boolean tentaRodar(Connection conn, String sql, String fileName, boolean isRetry) {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            if(!isRetry) logger.info("     ✅ Executado.");
            return true;
        } catch (SQLException e) {
            FriendlyError friendly = errorTranslator.translate(e);
            String msg = e.getMessage().toLowerCase();

            // 1. Erros Ignoráveis (Duplicidade segura)
            boolean isIgnorable = friendly.getTitulo().equalsIgnoreCase("Dados Duplicados") ||
                    friendly.getTitulo().contains("Objeto Ja Existe") ||
                    msg.contains("already exists");

            if (isIgnorable) {
                logger.info("     [IGNORADO] {} (Seguro).", friendly.getTitulo());
                return true;
            }

            // 2. Dependências (Joga para Fila)
            boolean isDependency = msg.contains("column unknown") || msg.contains("table unknown") || msg.contains("object not found");

            if (isDependency && !isRetry) {
                logger.warn("     [ADIADO] Dependência faltando.");
                deferredCommands.add(new DeferredCommand(sql, fileName));
                return true;
            }

            // 3. Erro Real
            logger.error("     ❌ ERRO FATAL: {}", friendly.toString());
            return false;
        }
    }

    // Método simplificado para usar na Fila de Dependências (menos log)
    private boolean tentaRodarDireto(Connection conn, String sql, boolean logSuccess) {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            if(logSuccess) logger.info("     ✅ [FILA] Sucesso.");
            return true;
        } catch (SQLException e) { return false; }
    }

    // --- Helpers de Controle de Fluxo (Mantidos aqui pois são regras de execução) ---

    private boolean isConfigurationCommand(String s) {
        return s.toUpperCase().startsWith("SET SQL") ||
                s.toUpperCase().startsWith("SET NAMES") ||
                s.toUpperCase().startsWith("SET CLIENTLIB");
    }

    private boolean isTransactionControl(String s) {
        return s.toUpperCase().startsWith("COMMIT") ||
                s.toUpperCase().startsWith("ROLLBACK");
    }

    private boolean isForbidden(String s) {
        return s.toUpperCase().startsWith("DROP DATABASE") ||
                s.toUpperCase().startsWith("CONNECT") ||
                s.toUpperCase().startsWith("CREATE DATABASE");
    }

    // Helper visual para log curto
    private String resumirComando(String cmd) {
        return cmd.length() > 80 ? cmd.substring(0, 80) + "..." : cmd;
    }
}