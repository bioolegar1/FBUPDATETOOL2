package com.fbupdatetool.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScriptSanitizer {

    private static final Logger logger = LoggerFactory.getLogger(ScriptSanitizer.class);

    /**
     * Aplica correções preventivas no SQL baseado no tipo de comando.
     * Agora é context-aware: só aplica regras específicas onde fazem sentido.
     *
     * @param sql O comando SQL original
     * @param commandType O tipo identificado pelo ScriptIdentity (opcional)
     * @return SQL corrigido
     */
    public String sanitize(String sql, ScriptIdentity.ScriptType commandType) {
        String novoSql = sql;

        // 1. Correção de tipos NUMERIC antigos (APLICA EM TODOS OS CASOS)
        // Justificativa: NUMERIC(15,X) pode causar overflow em versões novas
        if (novoSql.toUpperCase().contains("NUMERIC")) {
            novoSql = novoSql.replaceAll("(?i)NUMERIC\\s*\\(\\s*15\\s*,\\s*2\\s*\\)", "NUMERIC(18,2)");
            novoSql = novoSql.replaceAll("(?i)NUMERIC\\s*\\(\\s*15\\s*,\\s*4\\s*\\)", "NUMERIC(18,4)");
        }

        // 2. Correção de NULLs sem CAST (SOMENTE EM VIEWS)
        // Justificativa: Firebird exige tipo explícito em SELECT de VIEW,
        // mas NÃO em INSERT/UPDATE/CREATE TABLE
        if (commandType == ScriptIdentity.ScriptType.VIEW) {
            novoSql = corrigirNullsEmView(novoSql);
        }

        // Loga apenas se houve alteração
        if (!novoSql.equals(sql)) {
            logger.debug("SQL sanitizado (Tipo: {})", commandType);
        }

        return novoSql;
    }

    /**
     * Sobrecarga para manter compatibilidade com código que não passa o tipo.
     * Aplica apenas correções universais.
     */
    public String sanitize(String sql) {
        return sanitize(sql, ScriptIdentity.ScriptType.UNKNOWN);
    }

    /**
     * Correção específica para VIEWs: Adiciona CAST em NULLs soltos.
     *
     * Exemplos que são corrigidos:
     * - SELECT NULL, CAMPO FROM TABELA  →  SELECT CAST(NULL AS VARCHAR(100)), CAMPO FROM TABELA
     * - SELECT CAMPO, NULL FROM TABELA  →  SELECT CAMPO, CAST(NULL AS VARCHAR(100)) FROM TABELA
     * - SELECT NULL AS COLUNA FROM X    →  SELECT CAST(NULL AS VARCHAR(100)) AS COLUNA FROM X
     */
    private String corrigirNullsEmView(String sql) {
        String resultado = sql;

        // Padrão 1: ", NULL ," (NULL entre vírgulas)
        resultado = resultado.replaceAll("(?i)(,\\s*)NULL(\\s*,)", "$1CAST(NULL AS VARCHAR(100))$2");

        // Padrão 2: "SELECT NULL," (NULL logo após SELECT)
        resultado = resultado.replaceAll("(?i)(SELECT\\s+)NULL(\\s*,)", "$1CAST(NULL AS VARCHAR(100))$2");

        // Padrão 3: "SELECT NULL FROM" (NULL único sem vírgula após)
        resultado = resultado.replaceAll("(?i)(SELECT\\s+)NULL(\\s+FROM)", "$1CAST(NULL AS VARCHAR(100))$2");

        // Padrão 4: ", NULL FROM" (NULL final antes do FROM)
        resultado = resultado.replaceAll("(?i)(,\\s*)NULL(\\s+FROM)", "$1CAST(NULL AS VARCHAR(100))$2");

        // Padrão 5: "SELECT NULL AS" (NULL com alias)
        resultado = resultado.replaceAll("(?i)(SELECT\\s+)NULL(\\s+AS\\s+)", "$1CAST(NULL AS VARCHAR(100))$2");
        resultado = resultado.replaceAll("(?i)(,\\s*)NULL(\\s+AS\\s+)", "$1CAST(NULL AS VARCHAR(100))$2");

        return resultado;
    }
}