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

        Path path = Paths.get(getClass().getClassLoader().getResource("complex_script.sql").toURI());

        List<String> commands = parser.parse(path);

        Assertions.assertEquals(4, commands.size(), "Deveria ter encontrado 4 comandos SQL.");

        String procedure = commands.get(2);
        System.out.println("Comando Procedure:\n" + procedure);

        Assertions.assertTrue(procedure.contains("CREATE PROCEDURE"));
        Assertions.assertTrue(procedure.contains("END"), "A procedure foi cortada antes do END");
    }

}
