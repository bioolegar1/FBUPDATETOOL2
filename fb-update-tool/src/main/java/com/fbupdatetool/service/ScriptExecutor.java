package com.fbupdatetool.service;

import com.fbupdatetool.model.FriendlyError;
import com.fbupdatetool.util.ChecksumUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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

                // Identifica o tipo do comando adiado
                ScriptIdentity.ScriptType tipo = identity.identifyCommand(def.sql);

                // Sanitiza com contexto correto
                String sqlLimpo = sanitizer.sanitize(def.sql, tipo);

                if (tentaRodar(conn, sqlLimpo, def.fileName, true)) {
                    it.remove();
                    progress = true;
                }
            }

            try {
                if(!conn.getAutoCommit()) conn.commit();
            } catch(Exception e) {}

            if (!progress) break;
        }
    }

    // TRECHO ATUALIZADO DO ScriptExecutor.java
    // Apenas a parte que muda no método executeCommandList

    private boolean executeCommandList(Connection conn, List<String> commands, String fileName) {
        int cmdIndex = 0;
        for (String cmd : commands) {
            cmdIndex++;
            String cmdRaw = cmd.trim();
            if (cmdRaw.isEmpty()) continue;

            // Filtros de Segurança / Controle
            if (isConfigurationCommand(cmdRaw)) continue;
            if (isTransactionControl(cmdRaw)) continue;
            if (isForbidden(cmdRaw)) {
                if (!securityCallback.requestAdminPermission(cmdRaw)) return false;
            }

            logger.info("  -> Cmd #{}: Analisando...", cmdIndex);

            // NOVO: Identifica o tipo do comando ANTES de sanitizar
            ScriptIdentity.ScriptType tipoComando = identity.identifyCommand(cmdRaw);

            // PASSO A: Sanitização (Agora passa o tipo como contexto)
            String cmdSanitizado = sanitizer.sanitize(cmdRaw, tipoComando);

            // PASSO B: Planejamento (Responsabilidade do Strategy + Introspector)
            List<String> comandosReais = strategy.planExecution(conn, cmdSanitizado);

            if (comandosReais.isEmpty()) {
                continue;
            }

            // PASSO C: Execução Real
            for (String sqlReal : comandosReais) {
                if (!tentaRodar(conn, sqlReal, fileName, false)) {
                    return false;
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
            int errorCode = e.getErrorCode();

            // 1. Erros Ignoráveis (Duplicidade segura)
            boolean isIgnorable = friendly.getTitulo().equalsIgnoreCase("Dados Duplicados") ||
                    friendly.getTitulo().contains("Objeto Ja Existe") ||
                    friendly.getTitulo().equalsIgnoreCase("Coluna Já Existe") ||  // Nova condição
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

            // 3. Novo: Handle erro de NOT NULL em tabela populada (código -607 / 335544351)
            if (errorCode == 335544351 && msg.contains("validation error") && msg.contains("not null")) {
                logger.warn("     [AUTO-FIX] Erro de NOT NULL em tabela populada. Tentando corrigir automaticamente.");

                // Extrai tableName e columnName do SQL (assume ALTER TABLE ADD)
                String tableName = extractTableNameFromSql(sql);
                String columnName = extractColumnNameFromSql(sql);

                if (tableName == null || columnName == null) {
                    logger.error("     [AUTO-FIX] Falha ao extrair tabela/coluna do SQL.");
                    return false;
                }

                // Passo 1: Adiciona coluna como NULLABLE
                String tempSql = sql.replaceAll("(?i)\\s+NOT\\s+NULL", "");
                if (!tentaRodarDireto(conn, tempSql, false)) {
                    logger.error("     [AUTO-FIX] Falha ao adicionar coluna como NULLABLE.");
                    return false;
                }

                // Passo 2: Seta valor default para NULLs existentes
                String defaultValue = getDefaultValueForColumn(sql); // Helper para determinar default baseado no tipo
                String updateSql = String.format("UPDATE %s SET %s = %s WHERE %s IS NULL;", tableName, columnName, defaultValue, columnName);
                if (!tentaRodarDireto(conn, updateSql, false)) {
                    logger.error("     [AUTO-FIX] Falha ao atualizar valores NULL.");
                    return false;
                }

                // Passo 3: Altera para NOT NULL
                String setNotNullSql = String.format("ALTER TABLE %s ALTER %s SET NOT NULL;", tableName, columnName);
                if (!tentaRodarDireto(conn, setNotNullSql, false)) {
                    logger.error("     [AUTO-FIX] Falha ao setar NOT NULL.");
                    return false;
                }

                logger.info("     [AUTO-FIX] Correção aplicada com sucesso.");
                return true;
            }

            // 4. Erro Real
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
        } catch (SQLException e) {
            logger.error("     ❌ Erro ao executar direto: {}", e.getMessage());
            return false;
        }
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

    // Novo helper: Extrai tableName de SQL como ALTER TABLE
    private String extractTableNameFromSql(String sql) {
        try {
            String upper = sql.toUpperCase();
            int start = upper.indexOf("ALTER TABLE") + 11;
            String resto = sql.substring(start).trim();
            int end = resto.indexOf(' ');
            return resto.substring(0, end).trim();
        } catch (Exception e) {
            return null;
        }
    }

    // Novo helper: Extrai columnName de ALTER TABLE ADD coluna tipo
    private String extractColumnNameFromSql(String sql) {
        try {
            String upper = sql.toUpperCase();
            int addIdx = upper.indexOf("ADD ");
            String resto = sql.substring(addIdx + 4).trim();
            int spaceIdx = resto.indexOf(' ');
            return resto.substring(0, spaceIdx).trim();
        } catch (Exception e) {
            return null;
        }
    }

    // Novo helper: Determina valor default baseado no tipo da coluna
    private String getDefaultValueForColumn(String sql) {
        String upper = sql.toUpperCase();
        if (upper.contains("INTEGER") || upper.contains("SMALLINT") || upper.contains("BIGINT") ||
                upper.contains("NUMERIC") || upper.contains("DECIMAL") || upper.contains("DOUBLE") ||
                upper.contains("FLOAT")) {
            return "0";
        } else if (upper.contains("VARCHAR") || upper.contains("CHAR") || upper.contains("BLOB SUB_TYPE TEXT")) {
            return "''";
        } else if (upper.contains("DATE") || upper.contains("TIMESTAMP")) {
            return "'1900-01-01'";
        } else if (upper.contains("BOOLEAN")) {
            return "FALSE";
        } else {
            logger.warn("Tipo desconhecido. Usando NULL como default (pode falhar).");
            return "NULL";
        }
    }
}