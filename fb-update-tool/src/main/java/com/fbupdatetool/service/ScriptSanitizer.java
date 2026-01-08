package com.fbupdatetool.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScriptSanitizer {

    private static final Logger logger = LoggerFactory.getLogger(ScriptSanitizer.class);

    /**
     * Aplica correções preventivas no SQL para evitar erros comuns do Firebird.
     */
    public String sanitize(String sql) {
        String novoSql = sql;

        // 1. Corrige tipos NUMERIC antigos/inválidos para o padrão atual
        if (novoSql.toUpperCase().contains("NUMERIC")) {
            novoSql = novoSql.replaceAll("(?i)NUMERIC\\s*\\(\\s*15\\s*,\\s*2\\s*\\)", "NUMERIC(18,2)");
            novoSql = novoSql.replaceAll("(?i)NUMERIC\\s*\\(\\s*15\\s*,\\s*4\\s*\\)", "NUMERIC(18,4)");
        }

        // 2. Corrige NULLs soltos em Selects/Updates que precisam de CAST no Firebird
        // Ex: ", NULL ," vira ", CAST(NULL AS VARCHAR(100)) ,"
        // (Regex simplificada para evitar falsos positivos extremos, mas cobrindo o básico)
        novoSql = novoSql.replaceAll("(?i)(,\\s*)NULL(\\s*,)", "$1CAST(NULL AS VARCHAR(100))$2");
        novoSql = novoSql.replaceAll("(?i)(SELECT\\s+)NULL(\\s*,)", "$1CAST(NULL AS VARCHAR(100))$2");

        // Loga apenas se houve alteração
        if (!novoSql.equals(sql)) {
            // logger.debug("SQL sanitizado/corrigido preventivamente.");
        }

        return novoSql;
    }
}