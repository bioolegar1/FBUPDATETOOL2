package com.fbupdatetool.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseService {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseService.class);

    private static final String FB_URL_TEMPLATE = "jdbc:firebirdsql://localhost:%s/%s?encoding=WIN1252";
    private static final String USER = "SYSDBA";
    private static final String PASS = "masterkey";

    /**
     * Verifica se o serviço do Firebird esa rodando na porta 3050.
     */
    public static boolean checkFirebirdService(int port) {
        String host = "localhost";
        int timeoutMs = 2000;

        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeoutMs);
            logger.info("Serviço Firebird detectado na porta  {}.", port);
            return true;
        } catch (Exception e) {
            logger.warn("Porta {} fechada ou inacessível: {}", port, e.getMessage());
            return false;
        }

    }

    /**
     * Testa a conexão (Agora exige a porta).
     */
    public boolean testConnection(String dbPath, String port) throws SQLException {
        if (dbPath == null || dbPath.trim().isEmpty()) {
            throw new SQLException("O caminho do banco não pode ser vazio!");
        }

        String cleanPath = dbPath.replace("\\", "/");
        String jdbcUrl = String.format(FB_URL_TEMPLATE, port, cleanPath);
        logger.info("Testando conexão em: {", jdbcUrl);

        try (Connection conn = DriverManager.getConnection(jdbcUrl, USER, PASS);
                Statement stmt = conn.createStatement()) {
            stmt.executeQuery("SELECT 1 FROM RDB$DATABASE");
            logger.info("Conexao estabelecida com sucesso!");
            return true;
        }
    }
}
