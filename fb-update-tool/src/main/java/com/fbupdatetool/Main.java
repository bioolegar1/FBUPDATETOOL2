package com.fbupdatetool;

import com.fbupdatetool.service.ConfigurationService;
import com.fbupdatetool.service.DatabaseService;
import com.fbupdatetool.service.HistoryService;
import com.formdev.flatlaf.FlatDarkLaf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.Connection;
import java.sql.DriverManager;


public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {

        try {
            FlatDarkLaf.setup();
            logger.info("Tema FlatLaf configurado com sucesso.");
        }catch (Exception e){
            logger.error("Falha ao configurar tema", e);

        }
        logger.info("[INFO] Iniciando FBUpdateTool...");

        validarConexao();

        testarHistorico();

    }


    private static void testarHistorico(){
        String url = "jdbc:firebirdsql://localhost:3050//firebird/data/TESTE.GDB?encoding=WIN1252";
        try (Connection conn = DriverManager.getConnection(url, "SYSDBA", "masterkey")){

            HistoryService history = new HistoryService();

            history.initHistoryTable(conn);

            String script = "teste_issue_05.sql";
            if (!history.isScriptExecuted(conn, script)) {
                logger.info("Executando SQL...", script);
                history.markAsExecuted(conn, script);
            }else {
                logger.info("Script {} já foi executado!", script);
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void validarConexao() {
        DatabaseService dbService = new DatabaseService();
        ConfigurationService configService = new ConfigurationService();

        String caminhoBanco = "/firebird/data/TESTE.GDB";

        try {
            boolean sucesso = dbService.testConnection(caminhoBanco);
            if (sucesso) {
                logger.info("SUCESSO! Conexão JDBC funcionando.");
                configService.saveLastDbPath(caminhoBanco);
                logger.info("Caminho salvo no app.properties.");
            }
        } catch (Exception e) {
            logger.error("Falha na conexão", e);
        }
    }
}