package com.fbupdatetool.service;

import com.fbupdatetool.model.FriendlyError;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScriptExecutor {

    private static final Logger logger = LoggerFactory.getLogger(ScriptExecutor.class);

    private final ScriptParser parser;
    private final HistoryService historyService;
    private final DatabaseChangeTracker tracker;
    private final FirebirdErrorTranslator errorTranslator;
    private final SecurityCallback securityCallback;

    // Fila de comandos adiados (para dependencias cruzadas)
    private final List<DeferredCommand> deferredCommands = new ArrayList<>();

    // Classe interna para guardar o comando adiado
    private static class DeferredCommand {
        String sql;
        String fileName;

        public DeferredCommand(String sql, String fileName) {
            this.sql = sql;
            this.fileName = fileName;
        }
    }

    public ScriptExecutor(SecurityCallback securityCallback) {
        this.parser = new ScriptParser();
        this.historyService = new HistoryService();
        this.tracker = new DatabaseChangeTracker();
        this.errorTranslator = new FirebirdErrorTranslator();
        this.securityCallback = securityCallback;
    }

    public ScriptExecutor() {
        this(command -> false);
    }

    public DatabaseChangeTracker getTracker() { return tracker; }

    public boolean executeScript(Connection conn, Path scriptPath) {
        String fileName = scriptPath.getFileName().toString();

        try {
            logger.info("Analisando script: {}", fileName);
            List<String> commands = parser.parse(scriptPath);

            if (commands.isEmpty()) {
                logger.warn("Arquivo vazio: {}", fileName);
                return true;
            }

            // Executa linha a linha. Se der erro de dependencia, joga para a fila.
            boolean mainExecutionSuccess = executeCommandByCommand(conn, commands, fileName);

            if (mainExecutionSuccess) {
                try {
                    // Marca como executado (mesmo que tenha ficado algo pendente na fila)
                    if (!historyService.isScriptExecuted(conn, fileName)) {
                        historyService.markAsExecuted(conn, fileName);
                    }
                    if (!conn.getAutoCommit()) conn.commit();
                } catch (Exception e) { /* ignora */ }

                logger.info("Script {} processado (comandos complexos podem estar na fila de espera).", fileName);

                // REMOVIDO: Health check movido para o nível superior (UpdateService) para otimização

                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            logger.error("Erro critico em {}", fileName, e);
            return false;
        }
    }

    // --- ESTE É O MÉTODO QUE ESTAVA FALTANDO PARA O SIMULATION SERVICE ---
    public void processDeferredQueue(Connection conn) {
        if (deferredCommands.isEmpty()) return;

        logger.info("=== PROCESSANDO FILA DE DEPENDENCIAS ({} itens) ===", deferredCommands.size());

        boolean progress;
        int maxPasses = 20; // Tenta rodar a fila 20 vezes para resolver dependencias em cadeia

        for (int i = 1; i <= maxPasses; i++) {
            if (deferredCommands.isEmpty()) break;

            logger.info("--- Tentativa de Resolucao #{} ---", i);
            progress = false;
            Iterator<DeferredCommand> it = deferredCommands.iterator();

            while (it.hasNext()) {
                DeferredCommand def = it.next();
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute(def.sql);
                    logger.info("✅ SUCESSO TARDIO: O comando de '{}' foi resolvido!", def.fileName);
                    tracker.track(def.sql, def.fileName, "SUCCESS_LATE");
                    it.remove(); // Remove da fila
                    progress = true;
                } catch (SQLException e) {
                    // Ainda falhou, mantem na fila para a proxima passada
                    if (i == maxPasses) {
                        logger.error("❌ FALHA DEFINITIVA em '{}': {}", def.fileName, e.getMessage());
                    }
                }
            }

            try { if(!conn.getAutoCommit()) conn.commit(); } catch(Exception e) {}

            if (!progress) {
                logger.warn("Nenhum progresso na tentativa #{}. Dependencias podem estar faltando.", i);
                break;
            }
        }

        if (!deferredCommands.isEmpty()) {
            logger.error("ALERTA: {} comandos nao puderam ser resolvidos.", deferredCommands.size());
        } else {
            logger.info("Fila de dependencias limpa com sucesso!");
        }
    }

    private boolean executeCommandByCommand(Connection conn, List<String> commands, String fileName) {
        for (String cmd : commands) {
            String cmdLimpo = cmd.trim();
            if (cmdLimpo.isEmpty()) continue;

            if (isConfigurationCommand(cmdLimpo)) continue;
            if (isTransactionControl(cmdLimpo)) {
                tracker.track(cmd, fileName, "SKIPPED_JDBC");
                continue;
            }
            if (isForbidden(cmdLimpo)) {
                if (!securityCallback.requestAdminPermission(cmd)) {
                    tracker.track(cmd, fileName, "BLOCKED");
                    return false;
                }
            }

            // 1. INTELIGENCIA DE BANCO
            if (!precisaExecutarComando(conn, cmdLimpo)) {
                logger.info("Ignorado (Ja esta atualizado no banco): {}", resumirComando(cmdLimpo));
                tracker.track(cmd, fileName, "SMART_SKIP");
                continue;
            }

            // 2. SMART MERGE
            if (isCreateTable(cmdLimpo)) {
                String tableName = extractTableName(cmdLimpo);
                if (!tableName.isEmpty() && tableExists(conn, tableName)) {
                    boolean mergeSuccess = performSmartMerge(conn, tableName, cmd, fileName);
                    if (mergeSuccess) continue;
                }
            }

            // 3. EXECUCAO REAL COM TOLERANCIA DE DEPENDENCIA
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(cmd);
                tracker.track(cmd, fileName, "SUCCESS");
            } catch (SQLException e) {
                FriendlyError friendlyError = errorTranslator.translate(e);
                String msg = e.getMessage().toLowerCase();

                // Amplie a detecção de erros de dependência
                boolean isDependencyError =
                        msg.contains("column unknown") ||
                                msg.contains("object not found") ||
                                msg.contains("table unknown") ||
                                msg.contains("column not found") ||
                                msg.contains("field not found") ||
                                msg.contains("not defined as not null");

                String upperCmd = cmdLimpo.toUpperCase();
                boolean isDeferrableCommand =
                        upperCmd.startsWith("CREATE OR ALTER VIEW") ||
                                upperCmd.startsWith("CREATE VIEW") ||
                                upperCmd.startsWith("CREATE OR ALTER PROCEDURE") ||
                                upperCmd.startsWith("CREATE PROCEDURE") ||
                                upperCmd.startsWith("CREATE OR ALTER TRIGGER") ||
                                upperCmd.startsWith("CREATE TRIGGER") ||
                                upperCmd.startsWith("ALTER TABLE") ||
                                upperCmd.startsWith("UPDATE") ||
                                upperCmd.startsWith("INSERT INTO") ||
                                upperCmd.startsWith("CREATE INDEX") ||
                                upperCmd.startsWith("CREATE UNIQUE INDEX") ||
                                upperCmd.contains("ADD CONSTRAINT");

                if (isDependencyError && isDeferrableCommand) {
                    logger.warn("⚠️ ADIADO (Dependencia): Comando em '{}' precisa de algo que ainda nao existe. Colocando na fila...", fileName);
                    deferredCommands.add(new DeferredCommand(cmd, fileName));
                    tracker.track(cmd, fileName, "DEFERRED");
                    continue; // NAO FALHA! Segue para o proximo comando.
                }

                // Erros Ignoraveis Padrao
                boolean isIgnorable =
                        friendlyError.getTitulo().equalsIgnoreCase("Dados Duplicados") ||
                                friendlyError.getTitulo().equalsIgnoreCase("Objeto Ja Existe") ||
                                friendlyError.getTitulo().equalsIgnoreCase("Objeto Já Existe") ||
                                msg.contains("violation of primary or unique key") ||
                                msg.contains("attempt to store duplicate value") ||
                                (msg.contains("column") && msg.contains("exists")) ||
                                msg.contains("already exists");

                if (isIgnorable) {
                    logger.info("Ignorado: {} (Item ja processado/existente).", friendlyError.getTitulo());
                    tracker.track(cmd, fileName, "IGNORED_EXISTING");
                    continue;
                }

                logger.error("ERRO FATAL:\n{}\nDETALHE: {}", cmd, friendlyError.toString());
                tracker.track(cmd, fileName, "FAIL");
                return false;
            }
        }
        return true;
    }

    private boolean precisaExecutarComando(Connection conn, String sql) {
        String upperSql = sql.toUpperCase().replaceAll("\\s+", " ");
        if (upperSql.startsWith("CREATE OR ALTER") || upperSql.startsWith("RECREATE")) return true;
        try {
            if (upperSql.startsWith("CREATE VIEW")) {
                Pattern p = Pattern.compile("CREATE\\s+VIEW\\s+(\\w+)", Pattern.CASE_INSENSITIVE);
                Matcher m = p.matcher(upperSql);
                if (m.find() && viewExiste(conn, m.group(1))) return false;
            }
            if (upperSql.startsWith("CREATE TRIGGER")) {
                Pattern p = Pattern.compile("CREATE\\s+TRIGGER\\s+(\\w+)", Pattern.CASE_INSENSITIVE);
                Matcher m = p.matcher(upperSql);
                if (m.find() && triggerExiste(conn, m.group(1))) return false;
            }
            Pattern pAdd = Pattern.compile("ALTER TABLE (\\w+) ADD (\\w+)", Pattern.CASE_INSENSITIVE);
            Matcher mAdd = pAdd.matcher(upperSql);
            if (mAdd.find() && colunaExiste(conn, mAdd.group(1), mAdd.group(2))) return false;

            Pattern pAlter = Pattern.compile("ALTER TABLE (\\w+) ALTER (?:COLUMN )?(\\w+) TYPE (\\w+)", Pattern.CASE_INSENSITIVE);
            Matcher mAlter = pAlter.matcher(upperSql);
            if (mAlter.find() && colunaJaEhDesseTipo(conn, mAlter.group(1), mAlter.group(2), mAlter.group(3))) return false;

            if (upperSql.startsWith("CREATE GENERATOR") || upperSql.startsWith("CREATE SEQUENCE")) {
                String genName = upperSql.split(" ")[2].replace(";", "");
                if (generatorExiste(conn, genName)) return false;
            }
            if (upperSql.startsWith("CREATE INDEX") || upperSql.startsWith("CREATE UNIQUE INDEX")) {
                String[] parts = upperSql.split(" ");
                String indexName = parts[1].equals("UNIQUE") ? parts[3] : parts[2];
                if (indexExiste(conn, indexName)) return false;
            }
        } catch (Exception e) { return true; }
        return true;
    }

    private boolean performSmartMerge(Connection conn, String tableName, String createCmd, String fileName) {
        try {
            String cleanCmd = createCmd.replaceAll("/\\*[\\s\\S]*?\\*/", " ").replaceAll("\\s+", " ");
            int start = cleanCmd.indexOf('(');
            int end = cleanCmd.lastIndexOf(')');
            if (start == -1 || end == -1) return false;

            String body = cleanCmd.substring(start + 1, end);
            List<String> definitions = splitSqlDefinitions(body);

            for (String def : definitions) {
                String cleanDef = def.trim();
                if (cleanDef.isEmpty() || cleanDef.toUpperCase().startsWith("CONSTRAINT")
                        || cleanDef.toUpperCase().startsWith("PRIMARY KEY") || cleanDef.toUpperCase().startsWith("FOREIGN KEY")) continue;

                String[] parts = cleanDef.split("\\s+");
                String colName = parts[0].toUpperCase().replace("\"", "");
                String colDefinition = cleanDef.substring(colName.length()).trim();

                if (!colunaExiste(conn, tableName, colName)) {
                    String alterSql = "ALTER TABLE " + tableName + " ADD " + cleanDef;
                    logger.info("[SMART MERGE] Adicionando coluna faltante: {}", colName);
                    try (Statement stmt = conn.createStatement()) {
                        stmt.execute(alterSql);
                        tracker.track(alterSql, fileName, "SMART_ADDED");
                    }
                    continue;
                }

                if (colDefinition.toUpperCase().contains("NOT NULL")) {
                    if (isColumnNullableInDb(conn, tableName, colName)) {
                        logger.warn("[AUTO-CORRECT] Corrigindo NOT NULL em {}.{}...", tableName, colName);
                        String defaultValue = (colDefinition.toUpperCase().contains("CHAR") || colDefinition.toUpperCase().contains("VC")) ? "''" : "0";
                        String updateNulls = "UPDATE " + tableName + " SET " + colName + " = " + defaultValue + " WHERE " + colName + " IS NULL";
                        String typeOnly = colDefinition.replaceAll("(?i)NOT NULL", "").trim();
                        String alterType = "ALTER TABLE " + tableName + " ALTER COLUMN " + colName + " TYPE " + typeOnly;
                        String setNotNull = "UPDATE RDB$RELATION_FIELDS SET RDB$NULL_FLAG = 1 WHERE RDB$FIELD_NAME = '" + colName + "' AND RDB$RELATION_NAME = '" + tableName + "'";

                        try (Statement stmt = conn.createStatement()) {
                            stmt.execute(updateNulls);
                            stmt.execute(alterType);
                            stmt.execute(setNotNull);
                            logger.info("   -> Sucesso! Coluna ajustada para NOT NULL.");
                            tracker.track(setNotNull, fileName, "SMART_FIX_NULL");
                        } catch (SQLException e) {
                            logger.error("   -> Falha no Auto-Correct: {}", e.getMessage());
                        }
                    }
                }
            }
            return true;
        } catch (Exception e) { return false; }
    }

    private boolean checkExists(Connection conn, String tab, String field, String val) {
        String sql = "SELECT 1 FROM " + tab + " WHERE " + field + " = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, val.toUpperCase());
            try (ResultSet rs = stmt.executeQuery()) { return rs.next(); }
        } catch (SQLException e) { return false; }
    }

    private boolean checkExistsDual(Connection conn, String tab, String f1, String v1, String f2, String v2) {
        String sql = "SELECT 1 FROM " + tab + " WHERE " + f1 + " = ? AND " + f2 + " = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, v1.toUpperCase());
            stmt.setString(2, v2.toUpperCase());
            try (ResultSet rs = stmt.executeQuery()) { return rs.next(); }
        } catch (SQLException e) { return false; }
    }

    private boolean viewExiste(Connection conn, String viewName) { return tableExists(conn, viewName); }
    private boolean triggerExiste(Connection conn, String name) { return checkExists(conn, "RDB$TRIGGERS", "RDB$TRIGGER_NAME", name); }
    private boolean colunaExiste(Connection conn, String tab, String col) { return checkExistsDual(conn, "RDB$RELATION_FIELDS", "RDB$RELATION_NAME", tab, "RDB$FIELD_NAME", col); }
    private boolean generatorExiste(Connection conn, String name) { return checkExists(conn, "RDB$GENERATORS", "RDB$GENERATOR_NAME", name); }
    private boolean indexExiste(Connection conn, String name) { return checkExists(conn, "RDB$INDICES", "RDB$INDEX_NAME", name); }
    private boolean tableExists(Connection conn, String tableName) { return checkExists(conn, "RDB$RELATIONS", "RDB$RELATION_NAME", tableName); }

    private boolean isColumnNullableInDb(Connection conn, String tableName, String colName) throws SQLException {
        String sql = "SELECT RF.RDB$NULL_FLAG FROM RDB$RELATION_FIELDS RF WHERE RF.RDB$RELATION_NAME = ? AND RF.RDB$FIELD_NAME = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, tableName.toUpperCase());
            stmt.setString(2, colName.toUpperCase());
            try (ResultSet rs = stmt.executeQuery()) { if (rs.next()) { int nullFlag = rs.getInt(1); if (rs.wasNull()) nullFlag = 0; return nullFlag == 0; } }
        } return false;
    }

    private boolean colunaJaEhDesseTipo(Connection conn, String tabela, String coluna, String tipoAlvo) throws SQLException {
        String sql = "SELECT F.RDB$FIELD_NAME FROM RDB$RELATION_FIELDS RF JOIN RDB$FIELDS F ON RF.RDB$FIELD_SOURCE = F.RDB$FIELD_NAME WHERE RF.RDB$RELATION_NAME = ? AND RF.RDB$FIELD_NAME = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, tabela.toUpperCase());
            stmt.setString(2, coluna.toUpperCase());
            try (ResultSet rs = stmt.executeQuery()) { if (rs.next()) return rs.getString(1).trim().equalsIgnoreCase(tipoAlvo.trim()); }
        } return false;
    }

    private String resumirComando(String cmd) { return cmd.length() > 60 ? cmd.substring(0, 60) + "..." : cmd; }
    private boolean isConfigurationCommand(String sql) { String u = sql.toUpperCase(); return u.startsWith("SET SQL") || u.startsWith("SET NAMES") || u.startsWith("SET CLIENTLIB"); }
    private boolean isTransactionControl(String sql) { String u = sql.toUpperCase(); return u.startsWith("COMMIT") || u.startsWith("ROLLBACK"); }
    private boolean isForbidden(String sql) { String u = sql.toUpperCase(); return u.startsWith("DROP DATABASE") || u.startsWith("CONNECT") || u.startsWith("CREATE DATABASE"); }
    private boolean isCreateTable(String sql) { return sql.toUpperCase().startsWith("CREATE TABLE"); }
    private String extractTableName(String sql) { Matcher m = Pattern.compile("CREATE\\s+TABLE\\s+(\\w+)", Pattern.CASE_INSENSITIVE).matcher(sql); return m.find() ? m.group(1).toUpperCase() : ""; }
    private List<String> splitSqlDefinitions(String body) { List<String> list = new ArrayList<>(); int parenCount = 0; StringBuilder current = new StringBuilder(); for (char c : body.toCharArray()) { if (c == '(') parenCount++; else if (c == ')') parenCount--; if (c == ',' && parenCount == 0) { list.add(current.toString().trim()); current.setLength(0); } else current.append(c); } if (current.length() > 0) list.add(current.toString().trim()); return list; }
}