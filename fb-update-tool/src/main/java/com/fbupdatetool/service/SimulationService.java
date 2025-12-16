package com.fbupdatetool.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;

public class SimulationService {

    private static final Logger logger = LoggerFactory.getLogger(SimulationService.class);
    private final ScriptExecutor executor;
    private final DatabaseHealthService healthService;
    private final ScriptSorter sorter;

    public SimulationService() {
        // Callback muda para permitir tudo na simulacao (bypass de senha)
        this.executor = new ScriptExecutor(cmd -> true);
        this.healthService = new DatabaseHealthService();
        this.sorter = new ScriptSorter();
    }

    public boolean runSimulation(String url, String user, String password, List<Path> scripts) {
        logger.info("******************************************************");
        logger.info("*** INICIANDO SIMULACAO (DRY RUN) - NADA SERA SALVO ***");
        logger.info("******************************************************");

        Connection conn = null;
        try {
            conn = DriverManager.getConnection(url, user, password);
            conn.setAutoCommit(false); // IMPORTANTE: Desliga commit automatico

            // 1. Organiza os scripts (Inteligencia de Ordem)
            sorter.sortScripts(scripts);

            // 2. Executa todos os scripts (dentro da transacao)
            boolean scriptErrors = false;
            for (Path script : scripts) {
                boolean success = executor.executeScript(conn, script);
                if (!success) scriptErrors = true;
            }

            // 3. Resolve fila de espera (Dependencias)
            executor.processDeferredQueue(conn);

            // 4. Executa Health Check no "Futuro" (Como o banco ficaria)
            boolean isHealthy = healthService.checkDatabaseHealth(conn);

            logger.info("------------------------------------------------------");
            if (!scriptErrors && isHealthy) {
                logger.info("✅ RESULTADO DA SIMULACAO: SUCESSO! O banco ficara integro.");
                return true;
            } else {
                logger.error("❌ RESULTADO DA SIMULACAO: FALHA! Erros detectados.");
                if (scriptErrors) logger.error("   -> Houve erros na execucao dos scripts.");
                if (!isHealthy)   logger.error("   -> O Health Check detectou objetos invalidos.");
                return false;
            }

        } catch (Exception e) {
            logger.error("Erro critico durante a simulacao: {}", e.getMessage());
            return false;
        } finally {
            // O GRANDE TRUQUE: ROLLBACK SEMPRE
            if (conn != null) {
                try {
                    conn.rollback(); // Desfaz tudo que foi testado
                    conn.close();
                    logger.info("--- Simulacao finalizada. Banco restaurado ao estado original. ---");
                } catch (Exception e) { /* ignorar */ }
            }
        }
    }
}