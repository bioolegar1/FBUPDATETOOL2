package com.fbupdatetool.service;

import java.io.*;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ZipService {

    /**
     * Compacta um arquivo (ex: .GBK) para dentro de um .ZIP
     */
    public void compactarParaZip(File arquivoEntrada, File arquivoSaidaZip, Consumer<String> logger) throws Exception {
        logger.accept("Preparando para compactar: " + arquivoEntrada.getName());

        try (FileOutputStream fos = new FileOutputStream(arquivoSaidaZip);
             ZipOutputStream zos = new ZipOutputStream(fos);
             FileInputStream fis = new FileInputStream(arquivoEntrada)) {

            // Cria a entrada dentro do ZIP com o nome original do arquivo
            ZipEntry zipEntry = new ZipEntry(arquivoEntrada.getName());
            zos.putNextEntry(zipEntry);

            byte[] buffer = new byte[4096];
            int length;
            while ((length = fis.read(buffer)) >= 0) {
                zos.write(buffer, 0, length);
            }

            zos.closeEntry();
            logger.accept("Compactação finalizada. Arquivo gerado: " + arquivoSaidaZip.getName());
        } catch (IOException e) {
            throw new Exception("Erro ao compactar arquivo: " + e.getMessage());
        }
    }

    /**
     * Descompacta um arquivo .ZIP e procura por um arquivo de backup (.GBK ou .FBK)
     */
    public File descompactarZip(File arquivoZip, File pastaDestino, Consumer<String> logger) throws Exception {
        logger.accept("Iniciando descompactação de: " + arquivoZip.getName());

        File arquivoExtraido = null;
        byte[] buffer = new byte[4096];

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(arquivoZip))) {
            ZipEntry zipEntry = zis.getNextEntry();

            while (zipEntry != null) {
                String nomeArquivo = zipEntry.getName();

                // Ignora pastas, queremos apenas o arquivo do banco
                if (!zipEntry.isDirectory()) {

                    // Validação opcional: Verifica se parece um backup Firebird
                    // Aceita GBK (prioridade) ou FBK
                    boolean ehBackupValido = nomeArquivo.toUpperCase().endsWith(".GBK")
                            || nomeArquivo.toUpperCase().endsWith(".FBK");

                    if (ehBackupValido) {
                        logger.accept("Encontrado arquivo de backup no ZIP: " + nomeArquivo);

                        File novoArquivo = new File(pastaDestino, nomeArquivo);

                        // Garante que a pasta destino existe
                        if (!novoArquivo.getParentFile().exists()) {
                            novoArquivo.getParentFile().mkdirs();
                        }

                        // Escreve o arquivo no disco
                        try (FileOutputStream fos = new FileOutputStream(novoArquivo)) {
                            int len;
                            while ((len = zis.read(buffer)) > 0) {
                                fos.write(buffer, 0, len);
                            }
                        }

                        arquivoExtraido = novoArquivo;
                        logger.accept("Extração concluída: " + novoArquivo.getAbsolutePath());

                        // Encontramos o backup, podemos parar de procurar outros arquivos no ZIP
                        break;
                    } else {
                        logger.accept("Ignorando arquivo desconhecido no ZIP: " + nomeArquivo);
                    }
                }
                zipEntry = zis.getNextEntry();
            }
            zis.closeEntry();
        } catch (IOException e) {
            throw new Exception("Erro ao ler o arquivo ZIP: " + e.getMessage());
        }

        if (arquivoExtraido == null) {
            throw new Exception("Nenhum arquivo .GBK (ou .FBK) válido foi encontrado dentro do ZIP.");
        }

        return arquivoExtraido;
    }
}