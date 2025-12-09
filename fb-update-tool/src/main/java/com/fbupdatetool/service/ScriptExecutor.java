package com.fbupdatetool.service;

import com.fbupdatetool.model.FriendlyError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScriptExecutor {

    private static final Logger logger = LoggerFactory.getLogger(ScriptExecutor.class);

    private final ScriptParser parser;
    private final HistoryService historyService;
    private final DatabaseChangeTracker tracker;
    private final FirebirdErrorTranslator errorTranslator;

    // O "Rádio" para pedir permissão ao usuário (Callback)
    private final SecurityCallback securityCallback;

    // CONSTRUTOR PRINCIPAL (Com Segurança)
    public ScriptExecutor(SecurityCallback securityCallback) {
        this.parser = new ScriptParser();
        this.historyService = new HistoryService();
        this.tracker = new DatabaseChangeTracker();
        this.errorTranslator = new FirebirdErrorTranslator();
        this.securityCallback = securityCallback;
    }

    // CONSTRUTOR PADRÃO (Segurança Máxima: Se não passar callback, bloqueia tudo)
    public ScriptExecutor() {
        this(command -> false);
    }

    public DatabaseChangeTracker getTracker() { return tracker; }

    public boolean executeScript(Connection conn, Path scriptPath) {
        String fileName = scriptPath.getFileName().toString();

        try {
            // Verifica histórico apenas para logar, mas NÃO PARA a execução (para permitir Smart Merge)
            if (historyService.isScriptExecuted(conn, fileName)) {
                logger.info("Script {} consta no histórico, mas será reavaliado.", fileName);
            }

            logger.info("Processando script: {}", fileName);
            List<String> commands = parser.parse(scriptPath);

            if (commands.isEmpty()) {
                logger.warn("Arquivo vazio: {}", fileName);
                return true;
            }

            boolean success = executeCommandByCommand(conn, commands, fileName);

            if (success) {
                try {
                    // Grava no histórico se ainda não existir
                    if (!historyService.isScriptExecuted(conn, fileName)) {
                        historyService.markAsExecuted(conn, fileName);
                    }
                } catch (Exception e) { /* Ignora erro de histórico duplicado */ }

                logger.info("Script {} finalizado.", fileName);
                return true;
            } else {
                logger.error("Erro ao executar script {}", fileName);
                return false;
            }
        } catch (Exception e) {
            logger.error("Erro crítico em {}", fileName, e);
            return false;
        }
    }

    private boolean executeCommandByCommand(Connection conn, List<String> commands, String fileName) {
        for (String cmd : commands) {

            // --- 1. FILTRO DE TRANSAÇÃO (JDBC já faz isso) ---
            if (isTransactionControl(cmd)) {
                logger.info("Controle de Transação JDBC: Ignorando comando '{}'", cmd.trim());
                tracker.track(cmd, fileName, "SKIPPED_JDBC_AUTO");
                continue;
            }

            // --- 2. FILTRO DE SEGURANÇA (BLACKLIST) ---
            if (isForbidden(cmd)) {
                logger.warn("⛔ COMANDO PROIBIDO DETECTADO: {}", cmd.trim());
                logger.info("Solicitando autorização administrativa...");

                // CHAMA O CALLBACK (Pede a senha na Tela)
                boolean autorizado = securityCallback.requestAdminPermission(cmd);

                if (autorizado) {
                    logger.warn("🔓 ACESSO ADMINISTRATIVO CONCEDIDO. Executando comando perigoso...");
                    tracker.track(cmd, fileName, "ADMIN_OVERRIDE");
                    // O código continua abaixo para executar o comando
                } else {
                    logger.error("🔒 ACESSO NEGADO. Comando bloqueado.");
                    tracker.track(cmd, fileName, "BLOCKED_SECURITY");
                    return false; // Para o script imediatamente
                }
            }

            // --- 3. LÓGICA SMART MERGE (Para CREATE TABLE) ---
            if (isCreateTable(cmd)) {
                String tableName = extractTableName(cmd);
                if (!tableName.isEmpty() && tableExists(conn, tableName)) {
                    boolean mergeSuccess = performSmartMerge(conn, tableName, cmd, fileName);
                    if (mergeSuccess) continue; // Se resolveu via merge, pula o comando original
                }
            }

            // --- 4. EXECUÇÃO NO BANCO ---
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(cmd);

                if (isForbidden(cmd)) {
                    tracker.track(cmd, fileName, "SUCCESS_OVERRIDE");
                } else {
                    tracker.track(cmd, fileName, "SUCCESS");
                }

            } catch (SQLException e) {
                FriendlyError friendlyError = errorTranslator.translate(e);

                // Lista de erros que podemos ignorar (Idempotência)
                boolean isIgnorable =
                        friendlyError.getTitulo().equals("Dados Duplicados") ||
                                friendlyError.getTitulo().equals("Objeto Já Existe") ||
                                (friendlyError.getTitulo().equals("Objeto Não Encontrado") && cmd.toUpperCase().contains("DROP")) ||
                                e.getMessage().toLowerCase().contains("already exists") ||
                                e.getMessage().toLowerCase().contains("unsuccessful metadata update");

                if (isIgnorable) {
                    logger.info("Ignorado: {} (Item já processado).", friendlyError.getTitulo());
                    tracker.track(cmd, fileName, "IGNORED_EXISTING");
                    continue;
                }

                logger.error("FATAL ERROR:\n{}\nDETALHE: {}", cmd, friendlyError.toString());
                tracker.track(cmd, fileName, "FAIL");
                return false;
            }
        }
        return true;
    }

    // ==================================================================================
    // MÉTODOS AUXILIARES
    // ==================================================================================

    private boolean isTransactionControl(String sql) {
        String upper = sql.trim().toUpperCase();
        return upper.startsWith("COMMIT") || upper.startsWith("ROLLBACK");
    }

    private boolean isForbidden(String sql) {
        String upper = sql.trim().toUpperCase();
        return upper.startsWith("DROP DATABASE") ||
                upper.startsWith("CONNECT ") ||
                upper.startsWith("CREATE DATABASE");
    }

    private boolean isCreateTable(String sql) {
        return sql.trim().toUpperCase().startsWith("CREATE TABLE");
    }

    private String extractTableName(String sql) {
        Pattern p = Pattern.compile("CREATE\\s+TABLE\\s+(\\w+)", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(sql);
        return m.find() ? m.group(1).toUpperCase() : "";
    }

    private boolean tableExists(Connection conn, String tableName) {
        try (Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT 1 FROM RDB$RELATIONS WHERE RDB$RELATION_NAME = '" + tableName + "'");
            return rs.next();
        } catch (SQLException e) {
            return false;
        }
    }

    private boolean performSmartMerge(Connection conn, String tableName, String createCmd, String fileName) {
        try {
            Set<String> dbColumns = new HashSet<>();
            try (Statement stmt = conn.createStatement()) {
                ResultSet rs = stmt.executeQuery("SELECT RDB$FIELD_NAME FROM RDB$RELATION_FIELDS WHERE RDB$RELATION_NAME = '" + tableName + "'");
                while (rs.next()) {
                    dbColumns.add(rs.getString(1).trim().toUpperCase());
                }
            }

            int start = createCmd.indexOf('(');
            int end = createCmd.lastIndexOf(')');
            if (start == -1 || end == -1) return false;

            String body = createCmd.substring(start + 1, end);
            List<String> definitions = splitSqlDefinitions(body);

            for (String def : definitions) {
                String cleanDef = def.trim();
                if (cleanDef.isEmpty() || cleanDef.toUpperCase().startsWith("CONSTRAINT") || cleanDef.toUpperCase().startsWith("PRIMARY KEY")) continue;

                String colName = cleanDef.split("\\s+")[0].toUpperCase().replace("\"", "");

                if (!dbColumns.contains(colName)) {
                    String alterSql = "ALTER TABLE " + tableName + " ADD " + cleanDef;
                    logger.info("[SMART MERGE] Adicionando coluna faltante: {}", colName);

                    try (Statement stmt = conn.createStatement()) {
                        stmt.execute(alterSql);
                        tracker.track(alterSql, fileName, "SMART_ADDED");
                    }
                }
            }
            return true;
        } catch (Exception e) {
            logger.error("Erro no Smart Merge da tabela " + tableName, e);
            return false;
        }
    }

    private List<String> splitSqlDefinitions(String body) {
        List<String> list = new ArrayList<>();
        int parenCount = 0;
        StringBuilder current = new StringBuilder();

        for (char c : body.toCharArray()) {
            if (c == '(') parenCount++;
            else if (c == ')') parenCount--;

            if (c == ',' && parenCount == 0) {
                list.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        if (current.length() > 0) list.add(current.toString().trim());
        return list;
    }
}