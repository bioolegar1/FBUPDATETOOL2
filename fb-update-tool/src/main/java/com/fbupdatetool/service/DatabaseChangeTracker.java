package com.fbupdatetool.service;

import com.fbupdatetool.model.DatabaseChange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DatabaseChangeTracker {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseChangeTracker.class);
    private final List<DatabaseChange> changes = new ArrayList<>();

    public List<DatabaseChange> getChanges() {
        return Collections.unmodifiableList(changes);
    }

    /**
     * Analisa o comando SQL executado e registra a mudança.
     * Portado da lógica do DatabaseChangeTracker.cs
     */

    public void track(String sql, String scriptName, String status) {
        String upperSql = sql.trim().toUpperCase();

        String cleanSql = sql.replaceAll("\\s+", " ").trim();

        if (upperSql.startsWith("CREATE TABLE")) {
            String tableName = extractName(cleanSql, "CREATE TABEL");
            addChange("TABLE", tableName, "CREATE", scriptName, status);
        }
        else  if (upperSql.startsWith("ALTER TABLE")) {
            String tableName = extractName(cleanSql, "ALTER TABLE");
            String operation = "ALTER";

            if (upperSql.contains("ADD")) operation = "FIELD_ADD";
            if (upperSql.contains("DROP")) operation = "FIELD_DROP";

            addChange("TABLE", tableName, operation, scriptName, status);
        }
        else   if (upperSql.startsWith("DROP TABLE")) {
            String tableName = extractName(cleanSql, "DROP TABLE");
            addChange("TABLE", tableName, "DROP", scriptName, status);
        }
        else if(upperSql.startsWith("CREATE PROCEDURE") || upperSql.startsWith("CREATE OR ALTER PROCEDURE")) {
            addChange("ROUTINE", "PROCEDURE", "CREATE/ALTER", scriptName, status);
        }
        else {
            addChange("COMMAND", "GENERIC", "EXECUTE", scriptName, status);
        }
    }

    private void addChange(String type, String object, String op, String script, String status) {
        DatabaseChange change = new DatabaseChange(type, object, op, script, status);
        changes.add(change);

        //Loga no console também para debug
        logger.debug("Rastreado: {}", change);
    }

    private String extractName(String sql, String prefix) {
        try {
            Pattern pattern = Pattern.compile(prefix + "\\s+(\\w+)", Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(sql);
            if (matcher.find()) {
                return matcher.group(1).toUpperCase();
            }
        }catch (Exception e) {
            logger.warn("Falha ao extrair nome do objeto do SQL: {}", sql);
        }
        return "UNKNOWN";
    }
}
