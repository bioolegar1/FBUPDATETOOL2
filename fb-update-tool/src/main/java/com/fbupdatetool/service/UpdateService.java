package com.fbupdatetool.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.sql.Connection;
import java.util.List;

public class UpdateService {

    private static final Logger logger = LoggerFactory.getLogger(UpdateService.class);

    private final ScriptExecutor scriptExecutor;
    private final DatabaseHealthService healthService;

    public UpdateService() {
        this.scriptExecutor = new ScriptExecutor();
        this.healthService = new DatabaseHealthService();
    }

    /**
     * Executa a atualização completa: simulação ou real.
     * @param conn Conexão com autoCommit = true (gerencia commit por script)
     * @param scripts Lista de scripts ordenados
     * @param isSimulation Se true, não marca histórico (opcional)
     * @return true se tudo OK
     */
    public boolean executarAtualizacao(Connection conn, List<Path> scripts, boolean isSimulation) {
        try {
            conn.setAutoCommit(true); // Commit por script (essencial para simulação)

            logger.info("==================================================");
            logger.info("=== INICIANDO {} ===", isSimulation ? "SIMULAÇÃO (DRY RUN)" : "ATUALIZAÇÃO REAL");
            logger.info("Scripts a processar: {}", scripts.size());
            logger.info("==================================================");

            // 1. Health Check INICIAL
            logger.info("Executando Health Check inicial...");
            if (!healthService.checkDatabaseHealth(conn)) {
                logger.error("❌ Banco com problemas ANTES da atualização. Abortando.");
                return false;
            }
            logger.info("✅ Health Check inicial: Banco saudável.");

            // 2. Executa todos os scripts
            for (Path script : scripts) {
                String name = script.getFileName().toString();
                logger.info(">>> Processando script: {}", name);

                if (!scriptExecutor.executeScript(conn, script)) {
                    logger.error("❌ Falha crítica ao executar script: {}", name);
                    return false;
                }
            }

            // 3. Resolve comandos adiados (dependências cruzadas)
            scriptExecutor.processDeferredQueue(conn);

            // 4. Health Check FINAL
            logger.info("Executando Health Check final...");
            if (healthService.checkDatabaseHealth(conn)) {
                logger.info("✅ Health Check final: Banco saudável após atualização.");
                logger.info("==================================================");
                logger.info("=== {} CONCLUÍDA COM SUCESSO ===", isSimulation ? "SIMULAÇÃO" : "ATUALIZAÇÃO");
                logger.info("==================================================");
                return true;
            } else {
                logger.error("⚠️ Atualização concluída, mas o banco apresenta problemas de integridade.");
                logger.error("Recomenda-se revisar views/procedures inválidas.");
                return false;
            }

        } catch (Exception e) {
            logger.error("Erro inesperado durante a atualização: {}", e.getMessage(), e);
            try { conn.rollback(); } catch (Exception ignored) {}
            return false;
        }
    }
}