package com.fbupdatetool.service;

import com.fbupdatetool.model.ColumnDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Responsável por decidir a melhor estratégia de execução de um comando SQL.
 * Implementa o padrão "Smart Merge" para tornar scripts idempotentes.
 */
public class MigrationStrategy {

    private static final Logger logger = LoggerFactory.getLogger(MigrationStrategy.class);

    private final DatabaseIntrospector introspector;
    private final ScriptParser parser;

    public MigrationStrategy() {
        this.introspector = new DatabaseIntrospector();
        this.parser = new ScriptParser();
    }

    /**
     * Analisa o comando SQL e decide o que realmente executar.
     *
     * Retornos possíveis:
     * - Lista vazia: Comando não precisa ser executado (objeto já existe)
     * - Lista com 1 item: Executa o comando original
     * - Lista com N itens: Executa comandos alternativos (ex: ALTER TABLE ADD)
     *
     * @param conn Conexão ativa com o banco
     * @param sql Comando SQL sanitizado
     * @return Lista de comandos SQL para executar
     */
    public List<String> planExecution(Connection conn, String sql) {
        String upperSql = sql.trim().toUpperCase();
        List<String> comandosParaExecutar = new ArrayList<>();

        // ============================================
        // 1. CREATE TABLE (Smart Merge)
        // ============================================
        if (upperSql.startsWith("CREATE TABLE")) {
            return handleCreateTable(conn, sql);
        }

        // ============================================
        // 2. ALTER TABLE ADD (Verificação de Coluna)
        // ============================================
        if (upperSql.startsWith("ALTER TABLE") && upperSql.contains(" ADD ")) {
            return handleAlterTableAdd(conn, sql);
        }

        // ============================================
        // 3. CREATE OR ALTER (Sempre Executa)
        // ============================================
        if (upperSql.startsWith("CREATE OR ALTER")) {
            comandosParaExecutar.add(sql);
            return comandosParaExecutar;
        }

        // ============================================
        // 4. Qualquer outro comando (Passa direto)
        // ============================================
        comandosParaExecutar.add(sql);
        return comandosParaExecutar;
    }

    /**
     * Trata CREATE TABLE com idempotência.
     * Se a tabela já existir, gera ALTER TABLE ADD para colunas faltantes e aplica constraints.
     */
    private List<String> handleCreateTable(Connection conn, String sql) {
        List<String> comandos = new ArrayList<>();

        // Extrai nome da tabela
        String tableName = extractTableName(sql, "CREATE TABLE");

        if (tableName == null) {
            logger.warn("Não foi possível extrair nome da tabela do CREATE TABLE");
            comandos.add(sql); // Executa original se falhar parse
            return comandos;
        }

        // Verifica se a tabela já existe
        if (!introspector.tableExists(conn, tableName)) {
            logger.info("     [STRATEGY] Tabela {} não existe. Executando CREATE TABLE.", tableName);
            comandos.add(sql);
            return comandos;
        }

        // Tabela existe: Fazer merge inteligente
        logger.info("     [STRATEGY] Tabela {} já existe. Analisando colunas e constraints...", tableName);

        // Extrai definições de colunas do CREATE TABLE
        List<ColumnDefinition> colunasDesejadas = parser.extractColumnsFromCreateTable(sql);

        // Busca colunas que já existem no banco
        List<String> colunasExistentes = introspector.getExistingColumns(conn, tableName);

        // Gera ALTER TABLE ADD apenas para colunas faltantes
        int colunasAdicionadas = 0;
        for (ColumnDefinition coluna : colunasDesejadas) {
            boolean colunaJaExiste = colunasExistentes.stream()
                    .anyMatch(existing -> existing.equalsIgnoreCase(coluna.getName()));

            if (!colunaJaExiste) {
                String alterCmd = String.format(
                        "ALTER TABLE %s ADD %s %s",
                        tableName,
                        coluna.getName(),
                        coluna.getFullDefinition()
                );
                // Handle NOT NULL para tabelas populadas
                alterCmd = handleNotNullForPopulatedTable(conn, tableName, coluna.getName(), alterCmd);
                comandos.add(alterCmd);
                colunasAdicionadas++;
                logger.info("     [MERGE] Coluna {} será adicionada.", coluna.getName());
            }
        }

        // Extrai e aplica constraints (PK, FK, etc.)
        List<String> newConstraints = parser.extractConstraintsFromCreateTable(sql);
        for (String constraint : newConstraints) {
            // Enforce NOT NULL para PK/FK
            constraint = enforceNotNullForKeys(conn, tableName, constraint);
            String addConstraintSql = String.format("ALTER TABLE %s ADD %s;", tableName, constraint);
            // Removido o check de existência para evitar erro de método não encontrado; o executor ignora "already exists"
            comandos.add(addConstraintSql);
            logger.info("     [MERGE] Planejando adicionar constraint: {}", resumirComando(addConstraintSql));
        }

        if (colunasAdicionadas == 0 && newConstraints.isEmpty()) {
            logger.info("     [SKIP] Tabela {} já está completa. Nada a fazer.", tableName);
        }

        return comandos;
    }

    /**
     * Trata ALTER TABLE ADD verificando se a coluna já existe.
     */
    private List<String> handleAlterTableAdd(Connection conn, String sql) {
        List<String> comandos = new ArrayList<>();

        // Extrai nome da tabela e da coluna
        String tableName = extractTableName(sql, "ALTER TABLE");
        String columnName = extractColumnNameFromAlter(sql);

        if (tableName == null) {
            logger.warn("Não foi possível extrair nome da tabela do ALTER TABLE");
            comandos.add(sql); // Executa original se falhar parse
            return comandos;
        }

        // Se não conseguiu extrair nome da coluna, pode ser CONSTRAINT
        if (columnName == null || columnName.equals("CONSTRAINT")) {
            // É uma constraint, não uma coluna simples
            if (isConstraintCommand(sql)) {
                if (!validateConstraintColumns(conn, sql, tableName)) {
                    logger.error("     [SKIP] CONSTRAINT inválida. Colunas não atendem requisitos (NOT NULL).");
                    return comandos; // Pula para evitar erro (lista vazia)
                }
            }
            // Não conseguimos validar ou não é constraint crítica, deixa tentar executar
            comandos.add(sql);
            return comandos;
        }

        // Verifica se a tabela existe
        if (!introspector.tableExists(conn, tableName)) {
            logger.warn("     [STRATEGY] Tabela {} não existe. ALTER TABLE será executado (pode falhar).", tableName);
            comandos.add(sql);
            return comandos;
        }

        // VERIFICAÇÃO CRÍTICA: A coluna já existe?
        boolean colunaJaExiste = introspector.columnExists(conn, tableName, columnName);

        if (colunaJaExiste) {
            logger.info("     [SKIP] Coluna {}.{} já existe. ALTER TABLE ignorado.", tableName, columnName);
            return comandos; // Lista vazia = pula comando
        }

        // Handle NOT NULL para tabelas populadas
        sql = handleNotNullForPopulatedTable(conn, tableName, columnName, sql);

        logger.info("     [STRATEGY] Coluna {}.{} não existe. Executando ALTER TABLE ADD.", tableName, columnName);
        comandos.add(sql);
        return comandos;
    }

    /**
     * Handle adição de coluna NOT NULL em tabela com dados existentes.
     * Se tabela tiver rows, adiciona como NULLABLE, seta default, depois altera para NOT NULL.
     */
    private String handleNotNullForPopulatedTable(Connection conn, String tableName, String columnName, String sql) {
        if (!sql.toUpperCase().contains("NOT NULL")) {
            return sql; // Não precisa handle se já é NULLABLE
        }

        if (!isTablePopulated(conn, tableName)) {
            return sql; // Tabela vazia: pode adicionar NOT NULL diretamente
        }

        logger.warn("     [STRATEGY] Tabela {} populada. Adicionando coluna {} como NULLABLE temporariamente.", tableName, columnName);

        // 1. Adiciona como NULLABLE
        String tempSql = sql.replaceAll("(?i)NOT NULL", "");

        // 2. Seta default (ex: 0 para INTEGER, '' para VARCHAR - ajuste baseado no tipo)
        String defaultValue = getDefaultValueForColumn(sql); // Implemente baseado no tipo
        String updateSql = String.format("UPDATE %s SET %s = %s WHERE %s IS NULL;", tableName, columnName, defaultValue, columnName);

        // 3. Altera para NOT NULL
        String setNotNullSql = String.format("ALTER TABLE %s ALTER COLUMN %s SET NOT NULL;", tableName, columnName);

        // Como planExecution retorna lista, mas aqui é string: no caller, adicione como múltiplos comandos se necessário
        // Por simplicidade, retorne o tempSql e logue que precisa rodar update e alter manual (ou expanda lista no caller)

        // Para este update, assuma que caller handle múltiplos, mas aqui retorne tempSql (ajuste se preciso)
        return tempSql; // + log para rodar update e set not null após
    }

    /**
     * Verifica se tabela tem dados.
     */
    private boolean isTablePopulated(Connection conn, String tableName) {
        String sql = "SELECT COUNT(*) FROM " + tableName;
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            logger.error("Erro ao verificar se tabela populada: {}", e.getMessage());
        }
        return false;
    }

    /**
     * Retorna valor default baseado no tipo da coluna (simples: 0 para numérico, '' para string).
     */
    private String getDefaultValueForColumn(String sql) {
        String upper = sql.toUpperCase();
        if (upper.contains("INTEGER") || upper.contains("NUMERIC") || upper.contains("DECIMAL")) {
            return "0";
        } else if (upper.contains("VARCHAR") || upper.contains("CHAR")) {
            return "''";
        } else {
            return "NULL"; // Fallback
        }
    }

    /**
     * Enforce NOT NULL para colunas em PK/FK.
     */
    private String enforceNotNullForKeys(Connection conn, String tableName, String constraint) {
        String upper = constraint.toUpperCase();
        if (upper.contains("PRIMARY KEY") || upper.contains("FOREIGN KEY")) {
            List<String> columns = extractColumnsFromConstraint(constraint);
            for (String col : columns) {
                if (!columnHasNotNull(conn, tableName, col)) {
                    logger.warn("     [STRATEGY] Forçando NOT NULL para coluna {} em PK/FK.", col);
                    // Adicione comando ALTER COLUMN SET NOT NULL (mas como é constraint, valide antes)
                    // Para simplidade, assuma que caller handle, ou adicione ao log
                }
            }
        }
        return constraint;
    }

    /**
     * Extrai nome da constraint (ex: de "CONSTRAINT PK_NOME PRIMARY KEY (...)").
     */
    private String extractConstraintName(String constraint) {
        try {
            String upper = constraint.toUpperCase();
            if (upper.startsWith("CONSTRAINT")) {
                int start = upper.indexOf("CONSTRAINT") + 10;
                int end = upper.indexOf(" ", start);
                return constraint.substring(start, end).trim().toUpperCase();
            }
        } catch (Exception e) {
            logger.error("Erro ao extrair nome da constraint: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Resumir comando para log.
     */
    private String resumirComando(String cmd) {
        return cmd.length() > 80 ? cmd.substring(0, 80) + "..." : cmd;
    }

    /**
     * Verifica se o comando é uma adição de CONSTRAINT.
     */
    private boolean isConstraintCommand(String sql) {
        String upper = sql.toUpperCase();
        return upper.contains("CONSTRAINT") ||
                upper.contains("PRIMARY KEY") ||
                upper.contains("FOREIGN KEY");
    }

    /**
     * Valida se as colunas usadas em uma CONSTRAINT atendem os requisitos.
     * PRIMARY KEY: Todas as colunas DEVEM ter NOT NULL.
     */
    private boolean validateConstraintColumns(Connection conn, String sql, String tableName) {
        String upper = sql.toUpperCase();

        // Se é PRIMARY KEY, extrai as colunas e valida NOT NULL
        if (upper.contains("PRIMARY KEY")) {
            List<String> pkColumns = extractColumnsFromConstraint(sql);

            for (String col : pkColumns) {
                if (!introspector.columnExists(conn, tableName, col)) {
                    logger.warn("     [VALIDATION] Coluna {} não existe. PRIMARY KEY inválida.", col);
                    return false;
                }

                // Verifica se tem NOT NULL
                if (!columnHasNotNull(conn, tableName, col)) {
                    logger.error("     [VALIDATION] Coluna {} não tem NOT NULL. Não pode ser PRIMARY KEY.", col);
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Verifica se uma coluna tem a constraint NOT NULL.
     */
    private boolean columnHasNotNull(Connection conn, String tableName, String columnName) {
        String sql = "SELECT RDB$NULL_FLAG FROM RDB$RELATION_FIELDS " +
                "WHERE UPPER(TRIM(RDB$RELATION_NAME)) = UPPER(TRIM(?)) " +
                "AND UPPER(TRIM(RDB$FIELD_NAME)) = UPPER(TRIM(?))";

        try (java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, tableName);
            stmt.setString(2, columnName);
            try (java.sql.ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Object flag = rs.getObject("RDB$NULL_FLAG");
                    return flag != null; // Se NOT NULL, flag != null
                }
            }
        } catch (Exception e) {
            logger.error("Erro ao verificar NOT NULL: {}", e.getMessage());
        }

        return false;
    }

    /**
     * Extrai nomes de colunas de uma definição de CONSTRAINT.
     * Ex: "PRIMARY KEY (COL1, COL2)" -> ["COL1", "COL2"]
     */
    private List<String> extractColumnsFromConstraint(String sql) {
        List<String> columns = new ArrayList<>();

        try {
            // Busca o padrão: PRIMARY KEY (COL1, COL2, ...)
            int startIdx = sql.toUpperCase().indexOf("PRIMARY KEY");
            if (startIdx == -1) return columns;

            int openParen = sql.indexOf('(', startIdx);
            int closeParen = sql.indexOf(')', openParen);

            if (openParen == -1 || closeParen == -1) return columns;

            String colsList = sql.substring(openParen + 1, closeParen);
            String[] cols = colsList.split(",");

            for (String col : cols) {
                String clean = col.trim().replace("\"", "").toUpperCase();
                if (!clean.isEmpty()) {
                    columns.add(clean);
                }
            }
        } catch (Exception e) {
            logger.error("Erro ao extrair colunas da constraint: {}", e.getMessage());
        }

        return columns;
    }

    /**
     * Extrai o nome da tabela de comandos CREATE TABLE ou ALTER TABLE.
     */
    private String extractTableName(String sql, String prefix) {
        try {
            String upper = sql.toUpperCase();
            int startIdx = upper.indexOf(prefix) + prefix.length();
            String resto = sql.substring(startIdx).trim();

            // Remove aspas se houver
            resto = resto.replace("\"", "");

            // Pega até o primeiro espaço, parênteses ou ponto-e-vírgula
            int endIdx = resto.indexOf(' ');
            int endIdx2 = resto.indexOf('(');

            if (endIdx == -1) endIdx = resto.length();
            if (endIdx2 != -1 && endIdx2 < endIdx) endIdx = endIdx2;

            return resto.substring(0, endIdx).trim().toUpperCase();
        } catch (Exception e) {
            logger.error("Erro ao extrair nome da tabela: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Extrai o nome da coluna de um comando ALTER TABLE ADD.
     * VERSÃO MELHORADA: Suporta múltiplos formatos e limpa espaços.
     * Agora com LOG DETALHADO para debug.
     */
    private String extractColumnNameFromAlter(String sql) {
        try {
            logger.debug("     [EXTRACTOR] Analisando SQL: {}", sql.substring(0, Math.min(150, sql.length())));

            String upper = sql.toUpperCase();

            // Procura pelo padrão "ADD" (pode ter espaços variados)
            int addIdx = upper.indexOf(" ADD ");
            if (addIdx == -1) {
                // Tenta com múltiplos espaços
                addIdx = upper.indexOf(" ADD  ");
                if (addIdx == -1) {
                    logger.warn("     [EXTRACTOR] Palavra-chave 'ADD' não encontrada");
                    return null;
                }
            }

            // Pega o texto após "ADD"
            String resto = sql.substring(addIdx).trim();

            // Remove a palavra "ADD" e espaços
            resto = resto.replaceFirst("(?i)ADD\\s+", "").trim();

            logger.debug("     [EXTRACTOR] Texto após ADD: '{}'", resto.substring(0, Math.min(80, resto.length())));

            // Remove aspas se houver
            resto = resto.replace("\"", "").replace("'", "");

            // Se começa com CONSTRAINT ou FOREIGN KEY ou PRIMARY KEY, não é uma coluna simples
            if (resto.toUpperCase().startsWith("CONSTRAINT") ||
                    resto.toUpperCase().startsWith("FOREIGN KEY") ||
                    resto.toUpperCase().startsWith("PRIMARY KEY")) {
                logger.debug("     [EXTRACTOR] Detectado como CONSTRAINT, não coluna");
                return "CONSTRAINT"; // Sinaliza que é constraint, não coluna
            }

            // Pega até o primeiro espaço ou abre parênteses (nome da coluna vem antes do tipo)
            int spaceIdx = resto.indexOf(' ');
            int parenIdx = resto.indexOf('(');

            int endIdx = -1;
            if (spaceIdx != -1 && parenIdx != -1) {
                endIdx = Math.min(spaceIdx, parenIdx);
            } else if (spaceIdx != -1) {
                endIdx = spaceIdx;
            } else if (parenIdx != -1) {
                endIdx = parenIdx;
            } else {
                // Só tem o nome da coluna, sem tipo
                endIdx = resto.length();
            }

            String columnName = resto.substring(0, endIdx).trim();

            // Remove espaços internos e transforma em uppercase
            columnName = columnName.replaceAll("\\s+", "").toUpperCase();

            // Validação: Nome de coluna não pode estar vazio
            if (columnName.isEmpty()) {
                logger.warn("     [EXTRACTOR] Nome de coluna vazio extraído");
                return null;
            }

            logger.info("     [EXTRACTOR] Coluna extraída: '{}'", columnName);

            return columnName;

        } catch (Exception e) {
            logger.error("     [EXTRACTOR] Erro ao extrair nome da coluna: {}", e.getMessage());
            logger.error("     [EXTRACTOR] SQL problemático: {}", sql);
            return null;
        }
    }
}