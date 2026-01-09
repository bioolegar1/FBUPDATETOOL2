package com.fbupdatetool.service;

import com.fbupdatetool.model.FriendlyError;

import java.sql.SQLException;

public class FirebirdErrorTranslator {

    public FriendlyError translate(SQLException ex){
        int errorCode = ex.getErrorCode();
        String msg = ex.getMessage().toLowerCase();

        switch (errorCode) {
            case 335544580:
            case 335544569:
            case -204:
                return new FriendlyError(
                        "Objeto Não Encontrado",
                        "O script tenta acessar uma Tabela ou Procedure que não existe no banco.",
                        ex.getMessage()
                );

            case 335544665:
            case -803:
                return new FriendlyError(
                        "Dados Duplicados",
                        "Tentativa de inserir um registro que já existe (Violação de Chave primária/Única).",
                        ex.getMessage()
                );

            case 335544634:
            case -104:
                return new FriendlyError(
                        "Erro de Sintaxe SQL",
                        "Há um erro de escrita no comando(Vírgula faltando, nome errado, etc).",
                        ex.getMessage()
                );

            case 335544351:
            case -607:
                // Verifica mensagens específicas dentro deste código
                if (msg.contains("duplicate value") && msg.contains("unique index") &&
                        msg.contains("rdb$index_15")) {
                    // Este é o índice de colunas! Significa coluna duplicada
                    return new FriendlyError(
                            "Coluna Já Existe",  // MUDOU: Era "Coluna Duplicada"
                            "A coluna que está tentando adicionar já existe na tabela. (Ignorável)",
                            ex.getMessage()
                    );
                }

                if (msg.contains("not defined as not null") && msg.contains("primary key")) {
                    return new FriendlyError(
                            "PRIMARY KEY Inválida",
                            "As colunas da chave primária devem ter NOT NULL. Adicione NOT NULL antes de criar a constraint.",
                            ex.getMessage()
                    );
                }

                if (msg.contains("exists") || msg.contains("exist")) {
                    return new FriendlyError(
                            "Objeto Já Existe",
                            "Tentativa de criar uma tabela ou objeto que já está no banco.",
                            ex.getMessage()
                    );
                }

                return new FriendlyError(
                        "Erro de Metadados",
                        "Falha ao atualizar estrutura (objeto em uso ou inválido).",
                        ex.getMessage()
                );

            default:
                return new FriendlyError(
                        "Erro no Banco de Dados",
                        "Ocorreu um erro não classificado. Consulte o log técnico.",
                        ex.getMessage()
                );
        }
    }

    /**
     * Verifica se um erro pode ser ignorado com segurança.
     * @param error O erro traduzido
     * @return true se pode ignorar
     */
    public boolean isIgnorableError(FriendlyError error) {
        String titulo = error.getTitulo().toLowerCase();
        String mensagem = error.getMensagem().toLowerCase();

        // Verifica título
        boolean ignorableTitulo = titulo.contains("dados duplicados") ||
                titulo.contains("objeto já existe") ||
                titulo.contains("coluna já existe");

        // Verifica mensagem (caso o título não bata)
        boolean ignorableMensagem = mensagem.contains("ignorável") ||
                mensagem.contains("já existe");

        return ignorableTitulo || ignorableMensagem;
    }
}