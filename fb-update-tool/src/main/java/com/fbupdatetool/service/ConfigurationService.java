package com.fbupdatetool.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;


public class ConfigurationService {

    private static final Logger logger = LoggerFactory
            .getLogger(ConfigurationService.class);
    private static final String CONFIG_FILE = "app.properties";
    private static final String KEY_LAST_DB_PATH = "last_db_path";

    private final Properties properties = new Properties();

    public ConfigurationService() {
        load();
    }

    private void load() {
        Path path = Paths.get(CONFIG_FILE);
        if (Files.exists(path)) {
            try (FileInputStream in = new FileInputStream(path.toFile())) {
                properties.load(in);
            } catch (IOException e) {
                logger.error("Erro ao carregar configuraçao", e);
            }
        }
    }
    public  void saveLastDbPath(String dbPath) {
        properties.setProperty(KEY_LAST_DB_PATH, dbPath);
        try (FileOutputStream out = new FileOutputStream(CONFIG_FILE)){
            properties.store(out, "FBUpdateTool Configuration");
        }catch (IOException e) {
            logger.error("Erro ao salvar configuraçoes", e);
        }
    }

    public String getLastDbPath() {
        return properties.getProperty(KEY_LAST_DB_PATH, "");
    }
}
