package com.fbupdatetool.service;

import org.junit.Test;
import static org.junit.Assert.*;

public class ScriptSanitizerTest {

    private final ScriptSanitizer sanitizer = new ScriptSanitizer();

    @Test
    public void testNumericCorrection_AplicadoEmTodosOsCasos() {
        String sqlTable = "CREATE TABLE TESTE (PRECO NUMERIC(15,2))";
        String resultado = sanitizer.sanitize(sqlTable, ScriptIdentity.ScriptType.TABLE);

        assertTrue("NUMERIC(15,2) deve virar NUMERIC(18,2) em qualquer contexto",
                resultado.contains("NUMERIC(18,2)"));
    }

    @Test
    public void testNullCast_AplicadoApenasEmView() {
        String sqlView = "CREATE VIEW V_TESTE AS SELECT NULL, NOME FROM CLIENTES";

        // Quando identifica como VIEW, deve adicionar CAST
        String resultadoView = sanitizer.sanitize(sqlView, ScriptIdentity.ScriptType.VIEW);
        assertTrue("NULL em VIEW deve receber CAST",
                resultadoView.contains("CAST(NULL AS VARCHAR(100))"));

        // Quando identifica como INSERT, NÃO deve alterar
        String sqlInsert = "INSERT INTO TESTE VALUES (1, NULL, 'Nome')";
        String resultadoInsert = sanitizer.sanitize(sqlInsert, ScriptIdentity.ScriptType.INSERT);
        assertFalse("NULL em INSERT deve permanecer intacto",
                resultadoInsert.contains("CAST"));

        // Quando identifica como UPDATE, NÃO deve alterar
        String sqlUpdate = "UPDATE CLIENTES SET TELEFONE = NULL WHERE ID = 1";
        String resultadoUpdate = sanitizer.sanitize(sqlUpdate, ScriptIdentity.ScriptType.UPDATE);
        assertFalse("NULL em UPDATE deve permanecer intacto",
                resultadoUpdate.contains("CAST"));
    }

    @Test
    public void testNullCast_VariosContextosEmView() {
        // Testa diferentes posições de NULL dentro de uma VIEW

        // NULL no início
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
    public void testCreateTable_NullNaoDeveSerAlterado() {
        String sql = "CREATE TABLE CLIENTES (ID INT, OBSERVACAO VARCHAR(100) NULL)";
        String resultado = sanitizer.sanitize(sql, ScriptIdentity.ScriptType.TABLE);

        // O NULL de definição de coluna não deve virar CAST
        assertFalse("NULL em definição de coluna não deve ser alterado",
                resultado.contains("CAST"));
    }

    @Test
    public void testAlterTable_NullNaoDeveSerAlterado() {
        String sql = "ALTER TABLE CLIENTES ADD CAMPO_NOVO VARCHAR(50) NULL";
        String resultado = sanitizer.sanitize(sql, ScriptIdentity.ScriptType.TABLE);

        assertFalse("NULL em ALTER TABLE não deve ser alterado",
                resultado.contains("CAST"));
    }

    @Test
    public void testTipoDesconhecido_ApenasCorecoesUniversais() {
        String sql = "SELECT NULL, CAMPO FROM TABELA"; // Sem contexto
        String resultado = sanitizer.sanitize(sql); // Sem passar tipo

        // Sem tipo definido, não deve aplicar correção de NULL
        assertFalse("Sem tipo definido, correção de NULL não deve ser aplicada",
                resultado.contains("CAST"));
    }

    @Test
    public void testMultiplosNulls_EmView() {
        String sql = "CREATE VIEW V_COMPLETA AS " +
                "SELECT NULL, NULL, NOME, NULL FROM CLIENTES";

        String resultado = sanitizer.sanitize(sql, ScriptIdentity.ScriptType.VIEW);

        // Deve ter 3 CASTs (todos os NULLs que não são parte de string)
        int count = resultado.split("CAST\\(NULL AS VARCHAR\\(100\\)\\)", -1).length - 1;
        assertEquals("Deve corrigir todos os 3 NULLs na VIEW", 3, count);
    }
}