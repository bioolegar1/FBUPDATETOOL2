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

    public ScriptExecutor() {
        this.parser = new ScriptParser();
        this.historyService = new HistoryService();
        this.tracker = new DatabaseChangeTracker();
        this.errorTranslator = new FirebirdErrorTranslator();
    }

    public DatabaseChangeTracker getTracker() { return tracker; }

    public boolean executeScript(Connection conn, Path scriptPath) {
        String fileName = scriptPath.getFileName().toString();

        try {
            // Verifica histórico (mas não bloqueia, permite reprocessar para Smart Merge)
            boolean jaRodou = historyService.isScriptExecuted(conn, fileName);
            if (jaRodou) {
                logger.info("ℹ️ Script {} consta no histórico, mas será reavaliado.", fileName);
            }

            logger.info("Executing script {}", fileName);
            List<String> commands = parser.parse(scriptPath);

            if (commands.isEmpty()) {
                logger.warn("Arquivo vazio: {}", fileName);
                return true;
            }

            boolean success = executeCommandByCommand(conn, commands, fileName);

            if (success) {
                if (!jaRodou) {
                    historyService.markAsExecuted(conn, fileName);
                }
                logger.info("Script {} executed/verified", fileName);
                return true;
            } else {
                logger.error("Error executing script {}", fileName);
                return false;
            }
        } catch (Exception e) {
            logger.error("Error executing script {}", fileName, e);
            return false;
        }
    }

    private boolean executeCommandByCommand(Connection conn, List<String> commands, String fileName) {
        for (String cmd : commands) {

            // --- CORREÇÃO: IGNORAR COMANDOS DE TRANSAÇÃO EXPLÍCITOS ---
            // O JDBC gerencia a transação. Se mandarmos COMMIT manual, dá erro de handle.
            if (isTransactionControl(cmd)) {
                logger.info("ℹ️ Controle de Transação JDBC: Ignorando comando '{}'", cmd.trim());
                tracker.track(cmd, fileName, "SKIPPED_JDBC_AUTO");
                continue;
            }

            // --- LÓGICA SMART MERGE ---
            if (isCreateTable(cmd)) {
                String tableName = extractTableName(cmd);
                if (!tableName.isEmpty() && tableExists(conn, tableName)) {
                    boolean mergeSuccess = performSmartMerge(conn, tableName, cmd, fileName);
                    if (mergeSuccess) {
                        continue;
                    }
                }
            }

            try (Statement stmt = conn.createStatement()) {
                stmt.execute(cmd);
                tracker.track(cmd, fileName, "SUCCESS");

            } catch (SQLException e) {
                FriendlyError friendlyError = errorTranslator.translate(e);

                boolean isIgnorable =
                        friendlyError.getTitulo().equals("Dados Duplicados") ||
                                friendlyError.getTitulo().equals("Objeto Já Existe") ||
                                (friendlyError.getTitulo().equals("Objeto Não Encontrado") && cmd.toUpperCase().contains("DROP")) ||
                                e.getMessage().toLowerCase().contains("already exists") ||
                                e.getMessage().toLowerCase().contains("unsuccessful metadata update");

                if (isIgnorable) {
                    logger.info("ℹ️ Ignorado: {} (Item já processado anteriormente).", friendlyError.getTitulo());
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

    // --- MÉTODOS AUXILIARES ---

    // NOVO MÉTODO PARA DETECTAR COMMIT/ROLLBACK
    private boolean isTransactionControl(String sql) {
        String upper = sql.trim().toUpperCase();
        return upper.startsWith("COMMIT") || upper.startsWith("ROLLBACK");
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
                    logger.info("🛠️ [SMART MERGE] Adicionando coluna faltante: {}", colName);

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