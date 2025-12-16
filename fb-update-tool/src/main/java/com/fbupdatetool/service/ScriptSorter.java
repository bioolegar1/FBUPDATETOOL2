package com.fbupdatetool.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ScriptSorter {

    private static final Logger logger = LoggerFactory.getLogger(ScriptSorter.class);

    public void sortScripts(List<Path> scripts) {
        logger.info("Organizando sequencia de execucao baseada no conteudo...");

        Collections.sort(scripts, new Comparator<Path>() {
            @Override
            public int compare(Path p1, Path p2) {
                // 1. Mantem a ordem numerica do arquivo (ex: 01 antes de 02)
                // Isso e prioridade maxima se o usuario numerou explicitamente.
                String n1 = extractNumber(p1.getFileName().toString());
                String n2 = extractNumber(p2.getFileName().toString());
                int numCompare = n1.compareTo(n2);

                if (numCompare != 0) return numCompare;

                // 2. Se a numeracao for igual (ou sem numero), usa a inteligencia de conteudo
                int score1 = getContentScore(p1);
                int score2 = getContentScore(p2);

                return Integer.compare(score1, score2);
            }
        });
    }

    private String extractNumber(String name) {
        // Pega os primeiros digitos do arquivo (ex: "01 - Script" -> "01")
        return name.split("\\D+")[0];
    }

    private int getContentScore(Path path) {
        try {
            String content = Files.readString(path).toUpperCase();

            // PESOS DE PRIORIDADE (Menor roda primeiro)
            if (content.contains("CREATE TABLE") || content.contains("CREATE DOMAIN")) return 10;
            if (content.contains("ALTER TABLE") && content.contains("ADD")) return 20; // Criar campo
            if (content.contains("CREATE OR ALTER VIEW") || content.contains("CREATE VIEW")) return 30;
            if (content.contains("CREATE OR ALTER PROCEDURE")) return 40;
            if (content.contains("CREATE OR ALTER TRIGGER")) return 50;
            if (content.contains("INSERT INTO") || content.contains("UPDATE ")) return 60; // Dados

            return 99; // Desconhecido
        } catch (IOException e) {
            return 99;
        }
    }
}