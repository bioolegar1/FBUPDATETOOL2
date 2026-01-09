package com.fbupdatetool.service;

import com.fbupdatetool.model.ColumnDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScriptParser {

    private static final Logger logger = LoggerFactory.getLogger(ScriptParser.class);

    // Regex para capturar o delimitador (Ex: SET TERM ^ ; ou SET TERM ^)
    private static final Pattern SET_TERM_PATTERN = Pattern.compile("SET\\s+TERM\\s+(\\S+)", Pattern.CASE_INSENSITIVE);

    /**
     * Passo 1: Leitura Física
     * Apenas lê os bytes do disco e transforma em String, lidando com Encoding.
     * NÃO aplica nenhuma lógica de correção aqui.
     */
    public String readContent(Path filePath) throws IOException {
        try {
            // Tenta o padrão moderno (UTF-8)
            return Files.readString(filePath, StandardCharsets.UTF_8);
        } catch (Exception e) {
            // Fallback para padrão legado (Firebird antigo / Windows 1252)
            logger.debug("Leitura UTF-8 falhou para {}. Usando ISO-8859-1.", filePath.getFileName());
            return Files.readString(filePath, Charset.forName("ISO-8859-1"));
        }
    }

    /**
     * Passo 2: Quebra Lógica (Parsing)
     * Recebe o conteúdo (que pode ter vindo direto do arquivo OU ter sido envelopado
     * pela ScriptIdentity com SET TERM virtual) e quebra em comandos executáveis.
     */
    public List<String> parsePreparedContent(String content) {
        // Remove blocos de comentário /* ... */ para evitar falsos positivos
        String cleanContent = content.replaceAll("/\\*[\\s\\S]*?\\*/", " ");

        List<String> commands = new ArrayList<>();
        String delimiter = ";"; // O padrão SQL começa sempre com ponto e vírgula
        StringBuilder currentCommand = new StringBuilder();

        // Quebra linha a linha para processamento
        String[] lines = cleanContent.split("\\r?\\n");

        for (String line : lines) {
            // Cria uma versão da linha sem comentários e espaços para análise lógica
            String lineForLogic = removerComentarioLinha(line).trim();

            if (lineForLogic.isEmpty()) continue;

            // --- DETECÇÃO DE DELIMITADOR ---
            // Verifica se a linha é um comando de troca de delimitador (SET TERM)
            Matcher matcher = SET_TERM_PATTERN.matcher(lineForLogic);
            if (matcher.find()) {
                String rawDelim = matcher.group(1);
                // Se o delimitador vier com ponto e vírgula (Ex: ^;), removemos o ;
                delimiter = rawDelim.endsWith(";") ? rawDelim.substring(0, rawDelim.length() - 1) : rawDelim;

                logger.debug("Delimitador alterado para: {}", delimiter);
                continue; // Não adicionamos a linha SET TERM ao comando SQL final
            }

            // Acumula a linha ORIGINAL (com formatação) no buffer do comando atual
            currentCommand.append(line).append("\n");

            // Verifica se o comando terminou usando o delimitador ATUAL
            if (lineForLogic.endsWith(delimiter)) {
                adicionarComandoNaLista(commands, currentCommand, delimiter);
            }
        }

        // Se sobrou algo no buffer (arquivo sem delimitador no final), adiciona também
        if (currentCommand.length() > 0) {
            adicionarComandoNaLista(commands, currentCommand, delimiter);
        }

        return commands;
    }

    /**
     * Remove comentários de linha (--) para análise lógica,
     * evitando que "-- ;" seja confundido com fim de comando.
     */
    private String removerComentarioLinha(String line) {
        int commentIndex = line.indexOf("--");
        if (commentIndex >= 0) {
            return line.substring(0, commentIndex);
        }
        return line;
    }

    /**
     * Limpa o comando (remove o delimitador do final) e adiciona à lista.
     */
    private void adicionarComandoNaLista(List<String> commands, StringBuilder currentCommand, String delimiter) {
        String cmdSql = currentCommand.toString().trim();

        // Remove o delimitador do final para o JDBC não dar erro (ex: remove o ^ do final do trigger)
        if (cmdSql.endsWith(delimiter)) {
            cmdSql = cmdSql.substring(0, cmdSql.length() - delimiter.length()).trim();
        }

        // Segurança extra: as vezes sobra um ponto e vírgula se a sintaxe estava "SET TERM ^ ;"
        if (cmdSql.endsWith(delimiter)) {
            cmdSql = cmdSql.substring(0, cmdSql.length() - delimiter.length()).trim();
        }

        if (!cmdSql.isEmpty()) {
            commands.add(cmdSql);
        }
        currentCommand.setLength(0); // Limpa o buffer para o próximo comando
    }
    public List<String> extractConstraintsFromCreateTable(String createTableSql) {
        List<String> constraints = new ArrayList<>();
        String cleanCmd = createTableSql.replaceAll("/\\*[\\s\\S]*?\\*/", " ").replaceAll("\\s+", " ");

        int start = cleanCmd.indexOf('(');
        int end = cleanCmd.lastIndexOf(')');
        if (start == -1 || end == -1) return constraints;

        String body = cleanCmd.substring(start + 1, end);
        List<String> rawDefinitions = splitSqlDefinitions(body);  // Usa o helper existente

        for (String def : rawDefinitions) {
            String upper = def.trim().toUpperCase();
            if (upper.startsWith("CONSTRAINT") || upper.startsWith("PRIMARY KEY") ||
                    upper.startsWith("FOREIGN KEY") || upper.startsWith("UNIQUE")) {
                constraints.add(def.trim());
            }
        }
        return constraints;
    }
    /**
     * Extrai a lista de definições de colunas de um comando CREATE TABLE.
     * Isso remove a responsabilidade de parsing do Executor.
     */
    public List<ColumnDefinition> extractColumnsFromCreateTable(String createTableSql) {
        List<ColumnDefinition> columns = new ArrayList<>();

        // Limpeza básica
        String cleanCmd = createTableSql.replaceAll("/\\*[\\s\\S]*?\\*/", " ").replaceAll("\\s+", " ");

        int start = cleanCmd.indexOf('(');
        int end = cleanCmd.lastIndexOf(')');

        if (start == -1 || end == -1) return columns; // Retorna vazio se falhar

        String body = cleanCmd.substring(start + 1, end);
        List<String> rawDefinitions = splitSqlDefinitions(body);

        for (String def : rawDefinitions) {
            String cleanDef = def.trim();

            // Ignora Constraints de tabela, focando apenas em Colunas
            String upper = cleanDef.toUpperCase();
            if (upper.isEmpty() || upper.startsWith("CONSTRAINT") ||
                    upper.startsWith("PRIMARY KEY") || upper.startsWith("FOREIGN KEY")) {
                continue;
            }

            // Separa Nome do Resto (Tipo + Constraints)
            // Ex: "NOME VARCHAR(50)" -> parts[0]="NOME", resto="VARCHAR(50)"
            String[] parts = cleanDef.split("\\s+", 2);
            if (parts.length < 2) continue;

            columns.add(new ColumnDefinition(parts[0], parts[1]));
        }

        return columns;
    }

    /**
     * Helper para dividir as definições respeitando parênteses (ex: DECIMAL(10,2))
     */
    private List<String> splitSqlDefinitions(String body) {
        List<String> list = new ArrayList<>();
        int parenthesisDepth = 0;
        StringBuilder buffer = new StringBuilder();

        for (char c : body.toCharArray()) {
            if (c == '(') parenthesisDepth++;
            else if (c == ')') parenthesisDepth--;

            if (c == ',' && parenthesisDepth == 0) {
                list.add(buffer.toString().trim());
                buffer.setLength(0);
            } else {
                buffer.append(c);
            }
        }
        if (buffer.length() > 0) list.add(buffer.toString().trim());
        return list;
    }


}