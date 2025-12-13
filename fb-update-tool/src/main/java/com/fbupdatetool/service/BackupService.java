package com.fbupdatetool.service;

import org.firebirdsql.management.FBBackupManager;
import org.firebirdsql.gds.impl.GDSType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class BackupService {

    private static final Logger logger = LoggerFactory.getLogger(BackupService.class);

    // Método corrigido para aceitar OutputStream e evitar erro de formatação
    public void performBackup(String dbPath, String host, int port, String user, String password, OutputStream outputStream) throws Exception {
        logger.info("=== INICIANDO ROTINA DE BACKUP ===");

        // 1. Validação de Pasta
        File dbFile = new File(dbPath);
        File pastaDoBanco = dbFile.getAbsoluteFile().getParentFile();

        // Fallback: Se não achar a pasta ou não puder escrever, tenta C:\Temp
        if (pastaDoBanco == null || !pastaDoBanco.exists()) {
            pastaDoBanco = new File("C:\\Temp");
            if(!pastaDoBanco.exists()) pastaDoBanco.mkdirs();
        }

        // 2. Geração do Nome (Sem String.format para evitar erro "Conversion = .")
        String fileName = dbFile.getName();
        int lastDotIndex = fileName.lastIndexOf('.');
        String nomeSemExt = (lastDotIndex == -1) ? fileName : fileName.substring(0, lastDotIndex);

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm"));

        // Concatenação simples e segura
        String backupFileName = nomeSemExt + "_backup_" + timestamp + ".gbk";

        File backupFile = new File(pastaDoBanco, backupFileName);
        String backupPath = backupFile.getAbsolutePath();

        logger.info("Destino: " + backupPath);

        // 3. Execução via Jaybird
        FBBackupManager backupManager = new FBBackupManager(GDSType.getType("PURE_JAVA"));

        try {
            backupManager.setHost(host);
            backupManager.setPort(port);
            backupManager.setUser(user);
            backupManager.setPassword(password);
            backupManager.setDatabase(dbPath);
            backupManager.setBackupPath(backupPath);

            // Liga o modo verboso e redireciona para a janela (se houver)
            backupManager.setVerbose(true);
            if (outputStream != null) {
                backupManager.setLogger(outputStream);
            } else {
                backupManager.setLogger(System.out);
            }

            backupManager.backupDatabase();

            logger.info("Backup concluído!");

        } catch (Exception e) {
            logger.error("ERRO BACKUP: " + e.getMessage());
            // Apaga arquivo corrompido (0 bytes)
            if (backupFile.exists() && backupFile.length() == 0) {
                try { backupFile.delete(); } catch (Exception ignored) {}
            }
            throw e;
        }
    }
}