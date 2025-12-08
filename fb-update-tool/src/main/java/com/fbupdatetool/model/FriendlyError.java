package com.fbupdatetool.model;

public class FriendlyError {
    private final String titulo;
    private final String mensagem;
    private final String errrTecnico;


    public FriendlyError(String titulo, String mensagem, String errrTecnico) {
        this.titulo = titulo;
        this.mensagem = mensagem;
        this.errrTecnico = errrTecnico;
    }

    @Override
    public String toString() {
        return String.format("=== %s ===\n>> Dica: %s\n>> Técnico: %s", titulo, mensagem, errrTecnico);
    }

    public String getTitulo() {
        return titulo;
    }
    public String getMensagem() {
        return mensagem;
    }

}
