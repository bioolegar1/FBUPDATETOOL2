package com.fbupdatetool.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScriptIdentity {

    private static final Logger logger = LoggerFactory.getLogger(ScriptIdentity.class);

    public enum ScriptType {
        // Tipos de Estrutura e Dados
        TRIGGER, PROCEDURE, EXECUTE_BLOCK,
        VIEW, TABLE, GENERATOR, DOMAIN, EXCEPTION,
        INSERT, UPDATE, DELETE, SELECT,

        // Tipos Especiais (Novos)
        CONFIGURATION,      // SET SQL, SET NAMES...
        TRANSACTION_CONTROL,// COMMIT, ROLLBACK
        FORBIDDEN,          // DROP DATABASE, CONNECT...
        GRANT, REVOKE,

        UNKNOWN
    }

    public static class ScriptAnalysis {
        private final ScriptType type;
        private final String contentSafe;
        private final boolean requiresSpecialHandling;

        public ScriptAnalysis(ScriptType type, String contentSafe, boolean requiresSpecialHandling) {
            this.type = type;
            this.contentSafe = contentSafe;
            this.requiresSpecialHandling = requiresSpecialHandling;
        }
        public ScriptType getType() { return type; }
        public String getContentSafe() { return contentSafe; }
    }

    /**
     * Analisa o arquivo inteiro (para decidir sobre SET TERM)
     */
    public ScriptAnalysis analyze(String rawContent, String fileName) {
        String upper = removeInitialComments(rawContent.trim()).toUpperCase();
        ScriptType type = identifyType(upper); // Usa o método unificado

        boolean needsAutoFix = (type == ScriptType.TRIGGER ||
                type == ScriptType.PROCEDURE ||
                type == ScriptType.EXECUTE_BLOCK)
                && !upper.contains("SET TERM");

        String finalContent = rawContent;
        if (needsAutoFix) {
            logger.info("   ⚡ [IDENTITY] {} identificado como {}. Aplicando 'SET TERM' virtual.", fileName, type);
            finalContent = "SET TERM ^ ;\n" + rawContent + "\n^\nSET TERM ; ^";
        }

        return new ScriptAnalysis(type, finalContent, needsAutoFix);
    }

    /**
     * Analisa um comando individual (para o loop do Executor)
     */
    public ScriptType identifyCommand(String sql) {
        return identifyType(sql.toUpperCase().trim());
    }

    // A Lógica Centralizada de Identificação
    private ScriptType identifyType(String sql) {
        if (sql.isEmpty()) return ScriptType.UNKNOWN;

        // 1. Proibidos / Perigosos
        if (sql.startsWith("DROP DATABASE") || sql.startsWith("CONNECT") || sql.startsWith("CREATE DATABASE")) return ScriptType.FORBIDDEN;

        // 2. Controle de Transação
        if (sql.startsWith("COMMIT") || sql.startsWith("ROLLBACK")) return ScriptType.TRANSACTION_CONTROL;

        // 3. Configuração de Sessão
        if (sql.startsWith("SET SQL") || sql.startsWith("SET NAMES") || sql.startsWith("SET CLIENTLIB")) return ScriptType.CONFIGURATION;

        // 4. Blocos Complexos
        if (sql.startsWith("EXECUTE BLOCK")) return ScriptType.EXECUTE_BLOCK;

        // 5. DDL (Create/Alter)
        if (sql.startsWith("CREATE") || sql.startsWith("ALTER") || sql.startsWith("RECREATE")) {
            if (sql.contains("TRIGGER")) return ScriptType.TRIGGER;
            if (sql.contains("PROCEDURE")) return ScriptType.PROCEDURE;
            if (sql.contains("VIEW")) return ScriptType.VIEW;
            if (sql.contains("TABLE")) return ScriptType.TABLE;
            if (sql.contains("GENERATOR") || sql.contains("SEQUENCE")) return ScriptType.GENERATOR;
            if (sql.contains("DOMAIN")) return ScriptType.DOMAIN;
            if (sql.contains("EXCEPTION")) return ScriptType.EXCEPTION;
        }

        // 6. DML
        if (sql.startsWith("INSERT")) return ScriptType.INSERT;
        if (sql.startsWith("UPDATE")) return ScriptType.UPDATE;
        if (sql.startsWith("DELETE")) return ScriptType.DELETE;
        if (sql.startsWith("GRANT") || sql.startsWith("REVOKE")) return ScriptType.GRANT;

        return ScriptType.UNKNOWN;
    }

    private String removeInitialComments(String sql) {
        while (sql.startsWith("/*")) {
            int end = sql.indexOf("*/");
            if (end == -1) break;
            sql = sql.substring(end + 2).trim();
        }
        return sql;
    }
}