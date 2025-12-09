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
    private static final Logger logger = LoggerFactory.getLogger(ConfigurationService.class);
    private static final String CONFIG_FILE = "app.properties";

    // Nomes das chaves no arquivo de texto
    private static final String KEY_LAST_DB_PATH = "last_db_path";
    private static final String KEY_LAST_SCRIPT_FOLDER = "last_script_folder";
    private static final String KEY_LAST_DB_PORT = "last_db_port";

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
                logger.error("Erro ao carregar configurações", e);
            }
        }
    }

    private void save() {
        try (FileOutputStream out = new FileOutputStream(CONFIG_FILE)) {
            properties.store(out, "FBUpdateTool Configuration");
        } catch (IOException e) {
            logger.error("Erro ao salvar configurações", e);
        }
    }

    // =================================================================================
    // MÉTODOS DE ACESSO (GETTERS E SETTERS)
    // =================================================================================

    // --- 1. CAMINHO DO BANCO DE DADOS ---

    public String getLastDbPath() {
        return properties.getProperty(KEY_LAST_DB_PATH, "");
    }

    // ESTE ERA O MÉTODO QUE FALTAVA E GEROU O ERRO:
    public void saveLastDbPath(String dbPath) {
        properties.setProperty(KEY_LAST_DB_PATH, dbPath);
        save();
    }

    // --- 2. PASTA DE SCRIPTS ---

    public String getLastScriptFolder() {
        return properties.getProperty(KEY_LAST_SCRIPT_FOLDER, "");
    }

    public void saveLastScriptFolder(String folderPath) {
        properties.setProperty(KEY_LAST_SCRIPT_FOLDER, folderPath);
        save();
    }

    // --- 3. PORTA DO FIREBIRD ---

    public String getLastDbPort() {
        return properties.getProperty(KEY_LAST_DB_PORT, "3050"); // Padrão 3050
    }

    public void saveLastDbPort(String port) {
        properties.setProperty(KEY_LAST_DB_PORT, port);
        save();
    }
}