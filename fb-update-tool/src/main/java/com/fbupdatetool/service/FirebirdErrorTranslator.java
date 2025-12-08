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
                        "Objeto Não  Encontrado",
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
                // Verifica se a mensagem diz que já existe
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
}
