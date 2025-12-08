package com.fbupdatetool.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;

public class HistoryService {

    private static final Logger logger = LoggerFactory.getLogger(HistoryService.class);

    private static final String CREATE_TABLE_SQL =
            "CREATE TABLE FB_UPDATE_HISTORY (" +
                    " ID INTEGER NOT NULL PRIMARY KEY," +
                    " SCRIPT_NAME VARCHAR(255) NOT NULL," +
                    " EXECUTED_AT TIMESTAMP DEFAULT 'NOW'," +
                    " STATUS VARCHAR(20)" +
                    ")";

    private static final String CREATE_GEN_SQL = "CREATE GENERATOR GEN_FB_UPDATE_HISTORY_ID";

    // Trigger para ID automático no Firebird 2.5
    private static final String CREATE_TRIGGER_SQL =
            "CREATE TRIGGER TRG_FB_UPDATE_HISTORY_BI FOR FB_UPDATE_HISTORY " +
                    "ACTIVE BEFORE INSERT POSITION 0 " +
                    "AS BEGIN " +
                    "  IF (NEW.ID IS NULL) THEN " +
                    "    NEW.ID = GEN_ID(GEN_FB_UPDATE_HISTORY_ID, 1); " +
                    "END";

    public void initHistoryTable(Connection conn) throws SQLException {
        if (!tableExists(conn)) {
            // CORREÇÃO: Ajustei o texto para refletir a realidade (não encontrada)
            logger.info("Tabela de histórico não encontrada. Criando...");
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(CREATE_TABLE_SQL);

                // Tenta criar o Generator (pode falhar se já existir, por isso o try isolado)
                try {
                    stmt.execute(CREATE_GEN_SQL);
                } catch (SQLException e) {
                    logger.debug("Generator já existe ou erro ignorável: {}", e.getMessage());
                }

                stmt.execute(CREATE_TRIGGER_SQL);
                logger.info("Tabela de histórico criada com sucesso!");
            }
        }
    }

    public boolean isScriptExecuted(Connection conn, String scriptName) throws SQLException {
        String sql = "SELECT 1 FROM FB_UPDATE_HISTORY WHERE SCRIPT_NAME = ? AND STATUS = 'SUCCESS'";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, scriptName);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    public void markAsExecuted(Connection conn, String scriptName) throws SQLException {
        String sql = "INSERT INTO FB_UPDATE_HISTORY (SCRIPT_NAME, STATUS, EXECUTED_AT) VALUES (?, 'SUCCESS', ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, scriptName);
            // CORREÇÃO: setTimestamp em vez de setString
            stmt.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            stmt.executeUpdate();
            logger.info("Script registrado no histórico: {}", scriptName);
        }
    }

    private boolean tableExists(Connection conn) throws SQLException {
        String checkSql = "SELECT 1 FROM RDB$RELATIONS WHERE RDB$RELATION_NAME = 'FB_UPDATE_HISTORY'";
        // CORREÇÃO: Adicionado o parêntese ')' que faltava antes do '{'
        try (Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(checkSql)) {
                    return rs.next();
        }
    }
}