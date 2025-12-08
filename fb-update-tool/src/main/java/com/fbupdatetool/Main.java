package com.fbupdatetool;

import com.fbupdatetool.service.HistoryService;
import com.fbupdatetool.service.ScriptExecutor;
import com.formdev.flatlaf.FlatDarkLaf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        // 1. Configura Tema
        try {
            FlatDarkLaf.setup();
        } catch (Exception e) {
            logger.error("Erro no tema", e);
        }
        logger.info("[INFO] Iniciando FBUpdateTool (Modo Real)...");

        // 2. Chama a execução real
        rodarBackendComScriptsReais();
    }

    private static void rodarBackendComScriptsReais() {
        // --- CONFIGURAÇÃO ---
        // ATENÇÃO: Troque 'SEU_BANCO.GDB' pelo nome exato do arquivo que você colocou na pasta docker-data
        String nomeBanco = "TESTE.GDB";
        String url = "jdbc:firebirdsql://localhost:3050//firebird/data/" + nomeBanco + "?encoding=WIN1252";

        try (Connection conn = DriverManager.getConnection(url, "SYSDBA", "masterkey")) {

            // 1. Inicializa Tabela de Histórico (Obrigatório)
            new HistoryService().initHistoryTable(conn);

            logger.info("--- INICIANDO PROCESSAMENTO DE SCRIPTS REAIS ---");

            // Instancia o Maestro (seu Backend Completo)
            ScriptExecutor executor = new ScriptExecutor();

            // Define a pasta onde estão os arquivos
            Path pastaScripts = Paths.get("scripts");

            // Valida se a pasta existe
            if (!Files.exists(pastaScripts) || !Files.isDirectory(pastaScripts)) {
                logger.error("❌ ERRO: A pasta 'scripts' não foi encontrada na raiz do projeto!");
                logger.info(">> Crie uma pasta chamada 'scripts' junto ao pom.xml e coloque seus arquivos .sql lá.");
                return;
            }

            // 2. Busca e Ordena os arquivos (01, 02, 03...)
            List<Path> listaDeScripts;
            try (Stream<Path> stream = Files.list(pastaScripts)) {
                listaDeScripts = stream
                        .filter(p -> p.toString().toLowerCase().endsWith(".sql")) // Pega só .sql
                        .sorted() // Ordena alfabeticamente/numericamente
                        .collect(Collectors.toList());
            }

            if (listaDeScripts.isEmpty()) {
                logger.warn("⚠️ A pasta 'scripts' está vazia. Nada para fazer.");
                return;
            }

            logger.info("Encontrados {} scripts. Iniciando execução sequencial...", listaDeScripts.size());

            // 3. Loop de Execução (Aqui o Maestro trabalha)
            for (Path script : listaDeScripts) {
                logger.info("------------------------------------------------");
                // Chama o seu ScriptExecutor (que tem Parser, Tradutor e Tracker embutidos)
                boolean sucesso = executor.executeScript(conn, script);

                if (!sucesso) {
                    logger.error("⛔ PARE! Ocorreu um erro fatal no script: {}", script.getFileName());
                    logger.error("Corrija o arquivo antes de continuar.");
                    break; // Para o loop imediatamente para não quebrar o banco
                }
            }

            // 4. Relatório Final (Auditoria)
            logger.info("================================================");
            logger.info("\n--- RELATÓRIO DE MUDANÇAS (AUDITORIA) ---");
            executor.getTracker().getChanges().forEach(change -> {
                logger.info(change.toString());
            });

        } catch (Exception e) {
            logger.error("Erro fatal durante a execução", e);
        }
    }
}