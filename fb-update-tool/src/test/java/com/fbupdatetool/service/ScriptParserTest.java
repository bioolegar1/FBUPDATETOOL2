package com.fbupdatetool.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class ScriptParserTest {
    @Test
    void testParseComplexScript() throws Exception {
        ScriptParser parser = new ScriptParser();

        // Passo 1: Obtém o Path do recurso
        Path path = Paths.get(getClass().getClassLoader().getResource("complex_script.sql").toURI());

        // Passo 2: Lê o conteúdo bruto
        String rawContent = parser.readContent(path);

        // Opcional: Analisa com ScriptIdentity para aplicar SET TERM se necessário
        // (Isso é feito em ScriptExecutor, mas para o teste, pode ser necessário se o script for complexo)
        ScriptIdentity identity = new ScriptIdentity();
        ScriptIdentity.ScriptAnalysis analysis = identity.analyze(rawContent, path.getFileName().toString());
        String preparedContent = analysis.getContentSafe();

        // Passo 3: Parseia o conteúdo preparado
        List<String> commands = parser.parsePreparedContent(preparedContent);

        // Asserções originais
        Assertions.assertEquals(4, commands.size(), "Deveria ter encontrado 4 comandos SQL.");

        String procedure = commands.get(2);
        System.out.println("Comando Procedure:\n" + procedure);

        Assertions.assertTrue(procedure.contains("CREATE PROCEDURE"));
        Assertions.assertTrue(procedure.contains("END"), "A procedure foi cortada antes do END");
    }
}
