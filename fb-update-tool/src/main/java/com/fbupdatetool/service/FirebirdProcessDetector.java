package com.fbupdatetool.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class FirebirdProcessDetector {

    private static final Logger logger = LoggerFactory.getLogger(FirebirdProcessDetector.class);

    /**
     * Conta quantas instancias de processos Firebird estão rodando.
     * @return Número de processos encontrados (0, 1 ou mais).
     */
    public int countFirebirdProcesses() {
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("win")) {
            return countWindowsProcess();
        } else {
            logger.info("Ambiente não-Windows detectado. Assumindo 1 instância padrão.");
            return 1;
        }
    }

    private int countWindowsProcess() {
        int count = 0;
        // CORREÇÃO: Um único bloco TRY englobando tudo
        try {
            ProcessBuilder builder = new ProcessBuilder("tasklist", "/FO", "CSV", "/NH");
            Process process = builder.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String lowerLine = line.toLowerCase();
                    // Verifica se é um dos executáveis do Firebird
                    if (lowerLine.contains("fbserver.exe") ||
                            lowerLine.contains("fb_inet_server.exe") ||
                            lowerLine.contains("firebird.exe")) {
                        count++;
                    }
                }
            }

            logger.info("Scanner detectou {} processos de Firebird rodando.", count);
            return count;

        } catch (Exception e) {
            // CORREÇÃO: O catch agora pega erros tanto do ProcessBuilder quanto do Reader
            logger.error("Falha ao escanear processo do Windows", e);
            return 1; // Retorna 1 para não travar o app em caso de erro (Fail Safe)
        }
    }
}