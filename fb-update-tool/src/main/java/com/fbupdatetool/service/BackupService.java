package com.fbupdatetool.service;

import org.firebirdsql.management.FBBackupManager;
import org.firebirdsql.gds.impl.GDSType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class BackupService {

    private static final Logger logger = LoggerFactory.getLogger(BackupService.class);

    public void performBackup(String dbPath, String host, int port, String user, String password) throws Exception {
        logger.info("=== INICIANDO ROTINA DE BACKUP ===");

        // 1. Validate File and Folder
        File dbFile = new File(dbPath);
        File pastaDoBanco = dbFile.getAbsoluteFile().getParentFile();

        if (pastaDoBanco == null || !pastaDoBanco.exists()) {
            throw new Exception("Não foi possível identificar a pasta do banco de dados: " + dbPath);
        }

        // 2. Generate Filename (Using concatenation to avoid String.format errors)
        String fileName = dbFile.getName();
        // Remove extension (everything after the last dot)
        int lastDotIndex = fileName.lastIndexOf('.');
        String nomeBancoSemExtensao = (lastDotIndex == -1) ? fileName : fileName.substring(0, lastDotIndex);

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm"));

        // CORRECTION: Replaced String.format with concatenation to prevent "Conversion = '.'" error
        String backupFileName = nomeBancoSemExtensao + "_backup_" + timestamp + ".gbk";

        File backupFile = new File(pastaDoBanco, backupFileName);
        String backupPath = backupFile.getAbsolutePath();

        logger.info("Origem: " + dbPath);
        logger.info("Destino do Backup: " + backupPath);

        // 3. Execute Backup via Jaybird
        FBBackupManager backupManager = new FBBackupManager(GDSType.getType("PURE_JAVA"));

        try {
            backupManager.setHost(host);
            backupManager.setPort(port);
            backupManager.setUser(user);
            backupManager.setPassword(password);
            backupManager.setDatabase(dbPath);
            backupManager.setBackupPath(backupPath);
            backupManager.setVerbose(true);
            backupManager.setLogger(System.out);

            logger.info("Enviando comando de backup ao servidor Firebird...");

            backupManager.backupDatabase();

            logger.info("Backup concluído com SUCESSO!");
            logger.info("Arquivo gerado: " + backupFile.getName());

        } catch (Exception e) {
            logger.error("ERRO AO REALIZAR BACKUP: " + e.getMessage());

            // Cleanup: delete 0-byte file if failed
            if (backupFile.exists() && backupFile.length() == 0) {
                try {
                    backupFile.delete();
                } catch (Exception ignored) {}
            }
            throw e;
        }
    }
}