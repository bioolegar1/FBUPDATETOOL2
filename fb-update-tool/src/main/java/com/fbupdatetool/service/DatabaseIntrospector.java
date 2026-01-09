package com.fbupdatetool.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Responsável por inspecionar a estrutura do banco de dados Firebird.
 * Usa as tabelas de sistema (RDB$) para consultar metadados.
 */
public class DatabaseIntrospector {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseIntrospector.class);

    /**
     * Verifica se uma tabela existe no banco de dados.
     *
     * @param conn Conexão ativa
     * @param tableName Nome da tabela (case-insensitive)
     * @return true se a tabela existir
     */
    public boolean tableExists(Connection conn, String tableName) {
        String sql = "SELECT 1 FROM RDB$RELATIONS " +
                "WHERE UPPER(RDB$RELATION_NAME) = UPPER(?) " +
                "AND RDB$VIEW_BLR IS NULL"; // Filtra apenas tabelas (não views)

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, tableName.trim());
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            logger.error("Erro ao verificar existência da tabela {}: {}", tableName, e.getMessage());
            return false;
        }
    }

    /**
     * Verifica se uma view existe no banco de dados.
     *
     * @param conn Conexão ativa
     * @param viewName Nome da view
     * @return true se a view existir
     */
    public boolean viewExists(Connection conn, String viewName) {
        String sql = "SELECT 1 FROM RDB$RELATIONS " +
                "WHERE UPPER(RDB$RELATION_NAME) = UPPER(?) " +
                "AND RDB$VIEW_BLR IS NOT NULL"; // Filtra apenas views

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, viewName.trim());
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            logger.error("Erro ao verificar existência da view {}: {}", viewName, e.getMessage());
            return false;
        }
    }

    /**
     * Retorna a lista de nomes de colunas existentes em uma tabela.
     *
     * @param conn Conexão ativa
     * @param tableName Nome da tabela
     * @return Lista de nomes de colunas (em uppercase, sem espaços)
     */
    public List<String> getExistingColumns(Connection conn, String tableName) {
        List<String> columns = new ArrayList<>();

        String sql = "SELECT TRIM(RDB$FIELD_NAME) AS FIELD_NAME FROM RDB$RELATION_FIELDS " +
                "WHERE UPPER(TRIM(RDB$RELATION_NAME)) = UPPER(TRIM(?)) " +
                "ORDER BY RDB$FIELD_POSITION";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, tableName);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String columnName = rs.getString("FIELD_NAME");
                    if (columnName != null) {
                        // Remove todos os espaços e converte para uppercase
                        String cleanName = columnName.replaceAll("\\s+", "").toUpperCase();
                        columns.add(cleanName);
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Erro ao buscar colunas da tabela {}: {}", tableName, e.getMessage());
        }

        logger.debug("Tabela {} possui {} colunas: {}", tableName, columns.size(), columns);
        return columns;
    }

    /**
     * Verifica se uma coluna específica existe em uma tabela.
     * VERSÃO MELHORADA com log detalhado.
     *
     * @param conn Conexão ativa
     * @param tableName Nome da tabela
     * @param columnName Nome da coluna
     * @return true se a coluna existir
     */
    public boolean columnExists(Connection conn, String tableName, String columnName) {
        // Normaliza os nomes (remove espaços, uppercase)
        String cleanTable = tableName.trim().replaceAll("\\s+", "").toUpperCase();
        String cleanColumn = columnName.trim().replaceAll("\\s+", "").toUpperCase();

        String sql = "SELECT TRIM(RDB$FIELD_NAME) AS FIELD_NAME " +
                "FROM RDB$RELATION_FIELDS " +
                "WHERE UPPER(TRIM(RDB$RELATION_NAME)) = ? " +
                "AND UPPER(TRIM(RDB$FIELD_NAME)) = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, cleanTable);
            stmt.setString(2, cleanColumn);

            try (ResultSet rs = stmt.executeQuery()) {
                boolean exists = rs.next();

                if (exists) {
                    String actualName = rs.getString("FIELD_NAME");
                    logger.info("     [INTROSPECTOR] Coluna {}.{} ENCONTRADA no banco (nome real: '{}')",
                            cleanTable, cleanColumn, actualName);
                } else {
                    logger.debug("     [INTROSPECTOR] Coluna {}.{} NÃO existe no banco.",
                            cleanTable, cleanColumn);
                }

                return exists;
            }
        } catch (SQLException e) {
            logger.error("Erro ao verificar coluna {}.{}: {}", cleanTable, cleanColumn, e.getMessage());
            return false;
        }
    }

    /**
     * Verifica se uma procedure existe no banco.
     *
     * @param conn Conexão ativa
     * @param procedureName Nome da procedure
     * @return true se a procedure existir
     */
    public boolean procedureExists(Connection conn, String procedureName) {
        String sql = "SELECT 1 FROM RDB$PROCEDURES " +
                "WHERE UPPER(RDB$PROCEDURE_NAME) = UPPER(?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, procedureName.trim());
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            logger.error("Erro ao verificar procedure {}: {}", procedureName, e.getMessage());
            return false;
        }
    }

    /**
     * Verifica se um generator/sequence existe no banco.
     *
     * @param conn Conexão ativa
     * @param generatorName Nome do generator
     * @return true se o generator existir
     */
    public boolean generatorExists(Connection conn, String generatorName) {
        String sql = "SELECT 1 FROM RDB$GENERATORS " +
                "WHERE UPPER(RDB$GENERATOR_NAME) = UPPER(?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, generatorName.trim());
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            logger.error("Erro ao verificar generator {}: {}", generatorName, e.getMessage());
            return false;
        }
    }

    /**
     * Retorna informações detalhadas sobre uma coluna (tipo, tamanho, constraints).
     * Útil para comparações mais complexas.
     *
     * @param conn Conexão ativa
     * @param tableName Nome da tabela
     * @param columnName Nome da coluna
     * @return String com informações da coluna ou null se não existir
     */
    public String getColumnDetails(Connection conn, String tableName, String columnName) {
        String sql = "SELECT " +
                "  rf.RDB$FIELD_NAME, " +
                "  f.RDB$FIELD_TYPE, " +
                "  f.RDB$FIELD_LENGTH, " +
                "  rf.RDB$NULL_FLAG " +
                "FROM RDB$RELATION_FIELDS rf " +
                "JOIN RDB$FIELDS f ON rf.RDB$FIELD_SOURCE = f.RDB$FIELD_NAME " +
                "WHERE UPPER(rf.RDB$RELATION_NAME) = UPPER(?) " +
                "AND UPPER(rf.RDB$FIELD_NAME) = UPPER(?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, tableName.trim());
            stmt.setString(2, columnName.trim());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String name = rs.getString("RDB$FIELD_NAME").trim();
                    int type = rs.getInt("RDB$FIELD_TYPE");
                    int length = rs.getInt("RDB$FIELD_LENGTH");
                    boolean notNull = rs.getObject("RDB$NULL_FLAG") != null;

                    return String.format("%s (Type:%d, Length:%d, NotNull:%b)",
                            name, type, length, notNull);
                }
            }
        } catch (SQLException e) {
            logger.error("Erro ao buscar detalhes da coluna {}.{}: {}",
                    tableName, columnName, e.getMessage());
        }

        return null;
    }
}