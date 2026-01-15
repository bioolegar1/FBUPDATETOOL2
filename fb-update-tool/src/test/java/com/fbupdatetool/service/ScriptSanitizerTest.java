package com.fbupdatetool.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ScriptSanitizerTest {

    private final ScriptSanitizer sanitizer = new ScriptSanitizer();

    @Test
    void testNumericCorrection_AplicadoEmTodosOsCasos() {
        String sqlTable = "CREATE TABLE TESTE (PRECO NUMERIC(15,2))";
        String resultado = sanitizer.sanitize(sqlTable, ScriptIdentity.ScriptType.TABLE);

        assertTrue(resultado.contains("NUMERIC(18,2)"),
                "NUMERIC(15,2) deve virar NUMERIC(18,2) em qualquer contexto");
    }

    @Test
    void testNullCast_AplicadoApenasEmView() {
        String sqlView = "CREATE VIEW V_TESTE AS SELECT NULL, NOME FROM CLIENTES";

        // Quando identifica como VIEW, deve adicionar CAST
        String resultadoView = sanitizer.sanitize(sqlView, ScriptIdentity.ScriptType.VIEW);
        assertTrue(resultadoView.contains("CAST(NULL AS VARCHAR(100))"),
                "NULL em VIEW deve receber CAST");

        // Quando identifica como INSERT, NAO deve alterar
        String sqlInsert = "INSERT INTO TESTE VALUES (1, NULL, 'Nome')";
        String resultadoInsert = sanitizer.sanitize(sqlInsert, ScriptIdentity.ScriptType.INSERT);
        assertFalse(resultadoInsert.contains("CAST"),
                "NULL em INSERT deve permanecer intacto");

        // Quando identifica como UPDATE, NAO deve alterar
        String sqlUpdate = "UPDATE CLIENTES SET TELEFONE = NULL WHERE ID = 1";
        String resultadoUpdate = sanitizer.sanitize(sqlUpdate, ScriptIdentity.ScriptType.UPDATE);
        assertFalse(resultadoUpdate.contains("CAST"),
                "NULL em UPDATE deve permanecer intacto");
    }

    @Test
    void testNullCast_VariosContextosEmView() {
        // Testa diferentes posicoes de NULL dentro de uma VIEW

        // NULL no inicio
        String sql1 = "CREATE VIEW V1 AS SELECT NULL, COL1 FROM TAB1";
        String r1 = sanitizer.sanitize(sql1, ScriptIdentity.ScriptType.VIEW);
        assertTrue(r1.contains("SELECT CAST(NULL AS VARCHAR(100)), COL1"));

        // NULL no meio
        String sql2 = "CREATE VIEW V2 AS SELECT COL1, NULL, COL2 FROM TAB1";
        String r2 = sanitizer.sanitize(sql2, ScriptIdentity.ScriptType.VIEW);
        assertTrue(r2.contains("COL1, CAST(NULL AS VARCHAR(100)), COL2"));

        // NULL no final antes do FROM
        String sql3 = "CREATE VIEW V3 AS SELECT COL1, NULL FROM TAB1";
        String r3 = sanitizer.sanitize(sql3, ScriptIdentity.ScriptType.VIEW);
        assertTrue(r3.contains("COL1, CAST(NULL AS VARCHAR(100)) FROM"));

        // NULL com alias
        String sql4 = "CREATE VIEW V4 AS SELECT NULL AS CAMPO_VAZIO FROM TAB1";
        String r4 = sanitizer.sanitize(sql4, ScriptIdentity.ScriptType.VIEW);
        assertTrue(r4.contains("CAST(NULL AS VARCHAR(100)) AS CAMPO_VAZIO"));
    }

    @Test
    void testCreateTable_NullNaoDeveSerAlterado() {
        String sql = "CREATE TABLE CLIENTES (ID INT, OBSERVACAO VARCHAR(100) NULL)";
        String resultado = sanitizer.sanitize(sql, ScriptIdentity.ScriptType.TABLE);

        // O NULL de definicao de coluna nao deve virar CAST
        assertFalse(resultado.contains("CAST"),
                "NULL em definicao de coluna nao deve ser alterado");
    }

    @Test
    void testAlterTable_NullNaoDeveSerAlterado() {
        String sql = "ALTER TABLE CLIENTES ADD CAMPO_NOVO VARCHAR(50) NULL";
        String resultado = sanitizer.sanitize(sql, ScriptIdentity.ScriptType.TABLE);

        assertFalse(resultado.contains("CAST"),
                "NULL em ALTER TABLE nao deve ser alterado");
    }

    @Test
    void testTipoDesconhecido_ApenasCorecoesUniversais() {
        String sql = "SELECT NULL, CAMPO FROM TABELA"; // Sem contexto
        String resultado = sanitizer.sanitize(sql); // Sem passar tipo

        // Sem tipo definido, nao deve aplicar correcao de NULL
        assertFalse(resultado.contains("CAST"),
                "Sem tipo definido, correcao de NULL nao deve ser aplicada");
    }

    @Test
    void testMultiplosNulls_EmView() {
        String sql = "CREATE VIEW V_COMPLETA AS " +
                "SELECT NULL, NULL, NOME, NULL FROM CLIENTES";

        String resultado = sanitizer.sanitize(sql, ScriptIdentity.ScriptType.VIEW);

        // Deve ter 3 CASTs (todos os NULLs que nao sao parte de string)
        int count = resultado.split("CAST\\(NULL AS VARCHAR\\(100\\)\\)", -1).length - 1;
        assertEquals(3, count,
                "Deve corrigir todos os 3 NULLs na VIEW");
    }
}