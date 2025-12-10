package com.fbupdatetool.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScriptParser {

    private static final Logger logger = LoggerFactory.getLogger(ScriptParser.class);

    // Regex para identificar troca de delimitador (Ex: SET TERM ^ ;)
    private static final Pattern SET_TERM_PATTERN = Pattern.compile("SET\\s+TERM\\s+(\\S+)", Pattern.CASE_INSENSITIVE);

    /**
     * Lê o arquivo com estratégia de Fallback de Encoding e aplica correções.
     */
    public List<String> parse(Path filePath) throws IOException {
        logger.info("Lendo script: {}", filePath.getFileName());

        // 1. Tenta ler o conteúdo lidando com Encoding (UTF-8 vs ANSI)
        String rawContent = readFileWithFallback(filePath);

        // Remove espaços em branco do início/fim do arquivo inteiro para evitar falsos vazios
        rawContent = rawContent.trim();

        // 2. Aplica Auto-Fix de SET TERM se necessário
        String contentToProcess = applyAutoFix(rawContent, filePath.getFileName().toString());

        // 3. Quebra em comandos
        return parseContent(contentToProcess);
    }

    /**
     * Tenta ler UTF-8. Se falhar (arquivo legado), tenta ISO-8859-1 (compatível com WIN1252).
     */
    private String readFileWithFallback(Path path) throws IOException {
        try {
            // Tentativa 1: Padrão Moderno (UTF-8)
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (MalformedInputException | UncheckedIOException e) {
            // Tentativa 2: Padrão Legado (Windows/Ansi)
            logger.warn("Encoding UTF-8 falhou. Tentando ISO-8859-1 (Legado) para o arquivo: {}", path.getFileName());
            return Files.readString(path, Charset.forName("ISO-8859-1"));
        }
    }

    private String applyAutoFix(String content, String fileName) {
        String upper = content.toUpperCase();

        if (upper.contains("SET TERM")) {
            return content;
        }

        boolean needsFix = (upper.contains("TRIGGER") || upper.contains("PROCEDURE") || upper.contains("EXECUTE BLOCK")) &&
                (upper.contains("CREATE") || upper.contains("ALTER") || upper.contains("RECREATE") || upper.contains("AS"));

        if (needsFix) {
            logger.info("Auto-Fix: Adicionando 'SET TERM ^' virtualmente em {}", fileName);
            return "SET TERM ^ ;\n" + content + "\n^\nSET TERM ; ^";
        }

        return content;
    }

    public List<String> parseContent(String content) {
        String cleanContent = removeComments(content);

        List<String> commands = new ArrayList<>();
        String delimiter = ";";
        StringBuilder currentCommand = new StringBuilder();

        String[] lines = cleanContent.split("\\r?\\n");

        for (String line : lines) {
            String trimmedLine = line.trim();

            if (trimmedLine.isEmpty()) continue;

            Matcher matcher = SET_TERM_PATTERN.matcher(trimmedLine);
            if (matcher.find()) {
                delimiter = matcher.group(1);
                if (delimiter.endsWith(";")) delimiter = delimiter.substring(0, delimiter.length() - 1);
                logger.debug("Delimitador alterado para: {}", delimiter);
                continue;
            }

            currentCommand.append(line).append("\n");

            // Verifica se o comando terminou (tem o delimitador no final da linha)
            if (trimmedLine.endsWith(delimiter)) {
                adicionarComandoNaLista(commands, currentCommand, delimiter);
            }
        }

        // --- CORREÇÃO PRINCIPAL AQUI ---
        // Se o loop terminou e ainda tem coisa no buffer (ex: comando sem ; no final)
        // Adicionamos ele como um comando válido.
        if (currentCommand.length() > 0) {
            String resto = currentCommand.toString().trim();
            if (!resto.isEmpty()) {
                // Se por acaso o delimitador estiver lá mas o 'endsWith' falhou por espaço, removemos agora
                if (resto.endsWith(delimiter)) {
                    resto = resto.substring(0, resto.length() - delimiter.length()).trim();
                }
                commands.add(resto);
            }
        }

        return commands;
    }

    // Método auxiliar para evitar duplicação de código
    private void adicionarComandoNaLista(List<String> commands, StringBuilder currentCommand, String delimiter) {
        String cmdSql = currentCommand.toString().trim();

        if (cmdSql.endsWith(delimiter)) {
            cmdSql = cmdSql.substring(0, cmdSql.length() - delimiter.length()).trim();
        }

        if (!cmdSql.isEmpty()) {
            commands.add(cmdSql);
        }

        currentCommand.setLength(0); // Limpa o buffer
    }

    private String removeComments(String text) {
        String noBlock = text.replaceAll("/\\*[\\s\\S]*?\\*/", " ");
        return noBlock.replaceAll("(?m)^\\s*--.*$", "");
    }
}