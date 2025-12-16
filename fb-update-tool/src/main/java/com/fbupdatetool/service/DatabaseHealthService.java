package com.fbupdatetool.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHealthService {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseHealthService.class);

    /**
     * Executa um check-up completo e ativo no banco de dados.
     * Retorna TRUE se o banco estiver 100% saudavel.
     */
    public boolean checkDatabaseHealth(Connection conn) {
        logger.info("===========================================================");
        logger.info("===   INICIANDO TESTE DE INTEGRIDADE PROFUNDO (DEEP SCAN) ===");
        logger.info("===========================================================");

        List<String> errors = new ArrayList<>();

        // 1. TESTE ATIVO: Tenta compilar todas as Views (Pega erros silenciosos)
        errors.addAll(testAllViews(conn));

        // 2. TESTE ESTRUTURAL: Verifica Procedures marcadas como invalidas
        errors.addAll(checkInvalidProcedures(conn));

        // 3. TESTE ESTRUTURAL: Verifica Triggers invalidas
        errors.addAll(checkInvalidTriggers(conn));

        if (errors.isEmpty()) {
            logger.info("✅ SAUDE DO BANCO: 100% APROVADO. Estrutura consistente.");
            logger.info("===========================================================");
            return true;
        } else {
            logger.error("❌ FALHA NA INTEGRIDADE: Encontrados {} objetos quebrados.", errors.size());
            for (String err : errors) {
                logger.error("   -> {}", err);
            }
            logger.warn("⚠️  ESTES OBJETOS PRECISAM SER RECOMPILADOS OU CORRIGIDOS.");
            logger.info("===========================================================");
            return false;
        }
    }

    /**
     * Tenta fazer um SELECT Dummy em cada View.
     * Se a View tiver dependencias quebradas (ex: coluna que mudou de nome),
     * o Firebird vai estourar o erro aqui, e nos capturamos.
     */
    private List<String> testAllViews(Connection conn) {
        List<String> brokenViews = new ArrayList<>();
        List<String> viewsToTest = new ArrayList<>();
        int checkedCount = 0;

        // PASSO 1: Carrega apenas os NOMES das views para a memória (Fecha o ResultSet rápido)
        String sqlList = "SELECT RDB$RELATION_NAME FROM RDB$RELATIONS " +
                "WHERE RDB$VIEW_BLR IS NOT NULL " +
                "AND (RDB$SYSTEM_FLAG IS NULL OR RDB$SYSTEM_FLAG = 0)";

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sqlList)) {
            while (rs.next()) {
                viewsToTest.add(rs.getString(1).trim());
            }
        } catch (Exception e) {
            logger.error("Erro ao listar views do banco: {}", e.getMessage());
            return brokenViews;
        }

        // PASSO 2: Agora testa uma por uma sem conflito de cursor
        for (String viewName : viewsToTest) {
            checkedCount++;
            try (PreparedStatement testStmt = conn.prepareStatement("SELECT FIRST 0 * FROM " + viewName)) {
                // Apenas prepara e executa. Se falhar, cai no catch.
                testStmt.execute();
            } catch (Exception e) {
                // Limpa a mensagem de erro para ficar legível
                String msgLimpa = e.getMessage().replace("\n", " ").replaceAll("\\s+", " ");
                brokenViews.add("[VIEW QUEBRADA] " + viewName + " | Erro: " + msgLimpa);
            }
        }

        logger.info("Verificadas {} Views.", checkedCount);
        return brokenViews;
    }

    private List<String> checkInvalidProcedures(Connection conn) {
        List<String> invalidProcs = new ArrayList<>();
        // RDB$VALID_BLR = 0 significa que o binario esta desatualizado em relacao as tabelas
        String sql = "SELECT RDB$PROCEDURE_NAME FROM RDB$PROCEDURES " +
                "WHERE RDB$VALID_BLR = 0 " +
                "AND (RDB$SYSTEM_FLAG IS NULL OR RDB$SYSTEM_FLAG = 0)";

        try (Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                invalidProcs.add("[PROCEDURE INVALIDA] " + rs.getString(1).trim() + " (Precisa Recompilar)");
            }
        } catch (Exception e) {
            logger.error("Erro ao verificar procedures: {}", e.getMessage());
        }
        return invalidProcs;
    }

    private List<String> checkInvalidTriggers(Connection conn) {
        List<String> invalidTriggers = new ArrayList<>();
        String sql = "SELECT RDB$TRIGGER_NAME, RDB$RELATION_NAME FROM RDB$TRIGGERS " +
                "WHERE RDB$VALID_BLR = 0 " +
                "AND RDB$TRIGGER_INACTIVE = 0 " +
                "AND (RDB$SYSTEM_FLAG IS NULL OR RDB$SYSTEM_FLAG = 0)";

        try (Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String trig = rs.getString(1).trim();
                String table = rs.getString(2).trim();
                invalidTriggers.add("[TRIGGER INVALIDA] " + trig + " na tabela " + table);
            }
        } catch (Exception e) {
            logger.error("Erro ao verificar triggers: {}", e.getMessage());
        }
        return invalidTriggers;
    }
}