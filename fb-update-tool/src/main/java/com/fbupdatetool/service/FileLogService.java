package com.fbupdatetool.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class FileLogService {

    private static final Logger logger = LoggerFactory.getLogger(FileLogService.class);

    // Pasta padrão solicitada
    private static final String LOG_FOLDER = "C:\\SoluçõesPillar\\log_script_att";

    /**
     * Salva o conteúdo do log em um arquivo TXT com data/hora no nome.
     * @param conteudoLog O texto completo que está na janela de log.
     * @return O arquivo criado (para poder abrir depois).
     */
    public File salvarLogEmArquivo(String conteudoLog) {
        try {
            // 1. Cria a pasta se não existir
            Path folderPath = Paths.get(LOG_FOLDER);
            if (!Files.exists(folderPath)) {
                Files.createDirectories(folderPath);
            }

            // 2. Define o nome do arquivo: log_2023-12-10_14-30-00.txt
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
            String fileName = "log_execucao_" + timestamp + ".txt";
            File logFile = new File(folderPath.toFile(), fileName);

            // 3. Escreve o conteúdo
            try (FileWriter writer = new FileWriter(logFile)) {
                writer.write("=== LOG DE EXECUÇÃO - FB UPDATE TOOL ===\n");
                writer.write("Data: " + timestamp + "\n");
                writer.write("========================================\n\n");
                writer.write(conteudoLog);
            }

            logger.info("Log salvo com sucesso em: " + logFile.getAbsolutePath());
            return logFile;

        } catch (IOException e) {
            logger.error("Erro ao salvar log em arquivo", e);
            return null;
        }
    }

    /**
     * Abre o arquivo no editor padrão do sistema (Bloco de Notas).
     */
    public void abrirLogNoBlocoDeNotas(File arquivo) {
        if (arquivo == null || !arquivo.exists()) return;

        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(arquivo);
            } else {
                logger.warn("Abertura automática não suportada neste sistema.");
            }
        } catch (IOException e) {
            logger.error("Erro ao abrir log no bloco de notas", e);
        }
    }
}