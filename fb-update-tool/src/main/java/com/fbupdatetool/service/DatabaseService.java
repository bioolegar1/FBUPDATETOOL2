package com.fbupdatetool.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseService {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseService.class);

    private static final String FB_URL_TEMPLATE = "jdbc:firebirdsql://localhost:3050/%s?encoding=WIN1252";
    private static final String USER = "SYSDBA";
    private static final String PASS = "masterkey";

    /**
     * Testa a conexao com baco de dados.
     * @param dbPath Caminho completo do arquivo .GDB
     * @return true se conectar com sucesso
     * @throws SQLException se houver erro (para ser tratado na UI)
     */
    public  boolean testConnection(String dbPath) throws SQLException {
        if (dbPath == null || dbPath.trim().isEmpty()) {
            throw new SQLException("O caminho do banco não pode ser vazio");
        }

        String cleanPath = dbPath.replace("\\", "/");
        String jdbcUrl = String.format(FB_URL_TEMPLATE, cleanPath);

        logger.info("Testando conexão em: {}", jdbcUrl);

        try (Connection conn = DriverManager.getConnection(jdbcUrl, USER, PASS);
             Statement stmt = conn.createStatement()) {

            stmt.executeQuery("SELECT 1 FROM RDB$DATABASE");
            logger.info("Conexão estabelecida com sucesso!");
            return true;
        }
    }
}
