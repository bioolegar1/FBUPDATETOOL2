package com.fbupdatetool.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

public class FirebirdPathUtils {

    /**
     * Tenta localizar o executável gbak.exe automaticamente.
     * Ordem de busca:
     * 1. Registro do Windows (Instalação Padrão)
     * 2. Pastas Comuns (Program Files)
     * @return Caminho completo do gbak.exe ou null se não encontrar.
     */
    public static String descobrirCaminhoGbak() {
        String path = buscarNoRegistro();

        if (path != null) {
            String gbakPath = path + "\\bin\\gbak.exe";
            if (new File(gbakPath).exists()) return gbakPath;
        }

        // Se falhar no registro, tenta "chutar" os caminhos padrões
        String[] caminhosPadrao = {
                "C:\\Program Files\\Firebird\\Firebird_2_5\\bin\\gbak.exe",
                "C:\\Program Files (x86)\\Firebird\\Firebird_2_5\\bin\\gbak.exe",
                "C:\\Program Files\\Firebird\\Firebird_3_0\\bin\\gbak.exe",
                "C:\\Program Files (x86)\\Firebird\\Firebird_3_0\\bin\\gbak.exe"
        };

        for (String p : caminhosPadrao) {
            if (new File(p).exists()) return p;
        }

        return null; // Não encontrou nada
    }

    private static String buscarNoRegistro() {
        try {
            // Comando para ler a chave de instalação do Firebird no Windows
            // Tenta Firebird Project (versões mais novas e algumas antigas)
            String[] comandos = {
                    "reg query \"HKLM\\SOFTWARE\\Firebird Project\\Firebird Server\\Instances\" /v \"DefaultInstance\"",
                    "reg query \"HKLM\\SOFTWARE\\WOW6432Node\\Firebird Project\\Firebird Server\\Instances\" /v \"DefaultInstance\""
            };

            for (String cmd : comandos) {
                Process process = Runtime.getRuntime().exec(cmd);
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("DefaultInstance")) {
                        // A saída é algo como: "    DefaultInstance    REG_SZ    C:\Program Files\Firebird\Firebird_2_5\"
                        // Precisamos pegar apenas o caminho final
                        String path = line.split("REG_SZ")[1].trim();
                        return path;
                    }
                }
            }
        } catch (Exception e) {
            // Ignora erro, apenas segue para o próximo método de busca
        }
        return null;
    }
}