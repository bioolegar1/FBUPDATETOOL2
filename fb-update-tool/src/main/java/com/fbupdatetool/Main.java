package com.fbupdatetool;

import com.fbupdatetool.service.ConfigurationService;
import com.fbupdatetool.service.DatabaseService;
import com.formdev.flatlaf.FlatDarkLaf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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