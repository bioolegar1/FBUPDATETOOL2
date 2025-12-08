package com.fbupdatetool.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
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
     * Lê o arquivo, aplica correção automática de SET TERM se necessário e retorna os comandos.
     */
    public List<String> parse(Path filePath) throws IOException {
        logger.info("Lendo script: {}", filePath.getFileName());

        // Lê o conteúdo bruto do arquivo
        String rawContent = Files.readString(filePath, StandardCharsets.UTF_8);

        // --- AUTO-FIX: INJEÇÃO AUTOMÁTICA DE SET TERM ---
        String contentToProcess = applyAutoFix(rawContent, filePath.getFileName().toString());

        return parseContent(contentToProcess);
    }

    /**
     * Verifica se o script precisa de SET TERM e não tem. Se precisar, adiciona.
     */
    private String applyAutoFix(String content, String fileName) {
        String upper = content.toUpperCase();

        // 1. Se já tem SET TERM, respeitamos o arquivo original
        if (upper.contains("SET TERM")) {
            return content;
        }

        // 2. Verifica se é um objeto complexo que EXIGE Set Term (Trigger, Procedure, Execute Block)
        // Regex simplificado: Procura por CREATE/RECREATE/ALTER seguido de TRIGGER ou PROCEDURE
        boolean needsFix = (upper.contains("TRIGGER") || upper.contains("PROCEDURE") || upper.contains("EXECUTE BLOCK")) &&
                (upper.contains("CREATE") || upper.contains("ALTER") || upper.contains("RECREATE") || upper.contains("AS"));

        if (needsFix) {
            logger.info("⚡ Auto-Fix: Adicionando 'SET TERM ^' virtualmente em {}", fileName);

            // Envelopa o conteúdo original.
            // Adiciona o cabeçalho e o rodapé necessários para o Firebird entender o bloco.
            return "SET TERM ^ ;\n" + content + "\n^\nSET TERM ; ^";
        }

        // Se for script simples (CREATE TABLE, INSERT), retorna como está
        return content;
    }

    /**
     * Lógica "State Machine" para quebrar os comandos
     */
    public List<String> parseContent(String content) {
        String cleanContent = removeComments(content);

        List<String> commands = new ArrayList<>();
        String delimiter = ";"; // Delimitador padrão
        StringBuilder currentCommand = new StringBuilder();

        String[] lines = cleanContent.split("\\r?\\n");

        for (String line : lines) {
            String trimmedLine = line.trim();

            if (trimmedLine.isEmpty()) continue;

            // Detecta SET TERM
            Matcher matcher = SET_TERM_PATTERN.matcher(trimmedLine);
            if (matcher.find()) {
                delimiter = matcher.group(1);
                if (delimiter.endsWith(";")) delimiter = delimiter.substring(0, delimiter.length() - 1);
                logger.debug("Delimitador alterado para: {}", delimiter);
                continue;
            }

            // Acumula a linha
            currentCommand.append(line).append("\n");

            // Verifica se o comando terminou
            if (trimmedLine.endsWith(delimiter)) {
                String cmdSql = currentCommand.toString().trim();

                // Remove o delimitador do final para o JDBC aceitar
                if (cmdSql.endsWith(delimiter)) {
                    cmdSql = cmdSql.substring(0, cmdSql.length() - delimiter.length()).trim();
                }

                if (!cmdSql.isEmpty()) {
                    commands.add(cmdSql);
                }

                currentCommand.setLength(0);
            }
        }
        return commands;
    }

    private String removeComments(String text) {
        String noBlock = text.replaceAll("/\\*[\\s\\S]*?\\*/", " ");
        return noBlock.replaceAll("(?m)^\\s*--.*$", "");
    }
}