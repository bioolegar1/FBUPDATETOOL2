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
                    " SCRIPT_HASH VARCHAR(100)," +  // Nova coluna
                    " EXECUTED_AT TIMESTAMP DEFAULT 'NOW'," +
                    " STATUS VARCHAR(20)" +
                    ")";

    private static final String CREATE_GEN_SQL = "CREATE GENERATOR GEN_FB_UPDATE_HISTORY_ID";

    private static final String CREATE_TRIGGER_SQL =
            "CREATE TRIGGER TRG_FB_UPDATE_HISTORY_BI FOR FB_UPDATE_HISTORY " +
                    "ACTIVE BEFORE INSERT POSITION 0 " +
                    "AS BEGIN " +
                    "  IF (NEW.ID IS NULL) THEN " +
                    "    NEW.ID = GEN_ID(GEN_FB_UPDATE_HISTORY_ID, 1); " +
                    "END";

    public void initHistoryTable(Connection conn) throws SQLException {
        if (!tableExists(conn)) {
            logger.info("Infraestrutura de histórico não encontrada. Inicializando...");
            try (Statement stmt = conn.createStatement()) {
                try { stmt.execute(CREATE_TABLE_SQL); } catch (SQLException e) { }
                try { stmt.execute(CREATE_GEN_SQL); } catch (SQLException e) { }
                try { stmt.execute(CREATE_TRIGGER_SQL); } catch (SQLException e) { }
            }
        } else {
            // Migração inteligente: Verifica se a coluna HASH existe, se não, cria
            ensureHashColumnExists(conn);
        }
    }

    private void ensureHashColumnExists(Connection conn) {
        try {
            // Tenta selecionar a coluna. Se der erro, ela não existe.
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("SELECT SCRIPT_HASH FROM FB_UPDATE_HISTORY WHERE 1=0");
            }
        } catch (SQLException e) {
            logger.info("Atualizando tabela de histórico para suportar Checksum...");
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("ALTER TABLE FB_UPDATE_HISTORY ADD SCRIPT_HASH VARCHAR(100)");
            } catch (SQLException ex) {
                logger.error("Erro ao adicionar coluna de hash: " + ex.getMessage());
            }
        }
    }

    /**
     * Retorna TRUE se o script já foi executado.
     * Agora verifica também se o conteúdo mudou (apenas para log de aviso).
     */
    public boolean isScriptExecuted(Connection conn, String scriptName, String currentHash) throws SQLException {
        if (!tableExists(conn)) initHistoryTable(conn);

        String sql = "SELECT SCRIPT_HASH FROM FB_UPDATE_HISTORY WHERE SCRIPT_NAME = ? AND STATUS = 'SUCCESS'";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, scriptName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String dbHash = rs.getString("SCRIPT_HASH");
                    // Se houver hash no banco e for diferente do atual
                    if (dbHash != null && !dbHash.equals(currentHash)) {
                        logger.warn("ALERTA: O script '{}' foi alterado desde a ultima execucao!", scriptName);
                        logger.warn("Hash Antigo: {}", dbHash);
                        logger.warn("Hash Novo:   {}", currentHash);
                        // AQUI DECIDIMOS: Por segurança, retornamos TRUE (já executado) para não rodar de novo.
                        // Se quiser que rode de novo quando muda, mude para FALSE aqui.
                    }
                    return true;
                }
                return false;
            }
        }
    }

    public void markAsExecuted(Connection conn, String scriptName, String hash) throws SQLException {
        String sql = "INSERT INTO FB_UPDATE_HISTORY (SCRIPT_NAME, SCRIPT_HASH, STATUS, EXECUTED_AT) VALUES (?, ?, 'SUCCESS', ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, scriptName);
            stmt.setString(2, hash);
            stmt.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            stmt.executeUpdate();
            logger.info("Script registrado: {} (Hash: {})", scriptName, hash != null ? hash.substring(0, 8) + "..." : "N/A");
        }
    }

    private boolean tableExists(Connection conn) throws SQLException {
        String checkSql = "SELECT 1 FROM RDB$RELATIONS WHERE RDB$RELATION_NAME = 'FB_UPDATE_HISTORY'";
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(checkSql)) {
            return rs.next();
        }
    }
}