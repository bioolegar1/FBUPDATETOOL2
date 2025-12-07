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

import static java.awt.SystemColor.text;

public class ScriptParser {

    private static final Logger logger = LoggerFactory.getLogger(ScriptParser.class);

    private static final Pattern SET_TERM_PATTERN = Pattern.compile(
            ("SET\\s+TERM\\s+(\\S+)"), Pattern.CASE_INSENSITIVE);

    public List<String> parse(Path filePath) throws IOException {
        logger.info("Lendo script: {}", filePath.getFileName());

        String rawContent = Files.readString(filePath, StandardCharsets.UTF_8);
        return parseContent(rawContent);
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
            if(matcher.find()) {
                delimiter = matcher.group(1);
                if (delimiter.endsWith(";")) delimiter = delimiter.substring(0, delimiter.length() -1);
                logger.debug("Delimitador Alterado para: {}", delimiter);
                continue;
            }
            currentCommand.append(line).append("\n");

            if (trimmedLine.endsWith(delimiter)){
                String cmdSql = currentCommand.toString().trim();

                if (cmdSql.endsWith(delimiter)) {
                    cmdSql = cmdSql.substring(0, cmdSql.length() - delimiter.length()).trim();
                }
                if (!cmdSql.isEmpty()) commands.add(cmdSql);
                currentCommand.setLength(0);
            }


        }
        return commands;
    }

    private  String removeComments(String content) {
        String noBlock = content.replaceAll("/\\*[\\s\\S]*?\\*/", " ");
        return noBlock.replaceAll("(?m)ˆ\\s*--.*$", "" );
    }

}
