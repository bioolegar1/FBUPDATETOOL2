package com.fbupdatetool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        // 1. Cria as pastas necessárias (Backups, Scripts, Logs)
        // Isso é seguro fazer aqui pois não requer interface gráfica
        inicializarEstruturaPastas();

        // 2. Passa o bastão imediatamente para a aplicação JavaFX
        // A validação do Firebird e erros visuais serão tratados lá dentro
        logger.info("Inicialização do sistema de arquivos concluída. Iniciando App JavaFX...");

        // Chama o método main da sua classe de interface
        MainApp.main(args);
    }

    private static void inicializarEstruturaPastas() {
        String[] pastas = {"backups", "scripts", "logs"};
        for (String pasta : pastas) {
            File dir = new File(pasta);
            if (!dir.exists()) {
                if (dir.mkdirs()) {
                    logger.info("Diretório criado: {}", dir.getAbsolutePath());
                }
            }
        }
    }
}