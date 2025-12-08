package com.fbupdatetool.model;

import java.time.LocalDateTime;

public class DatabaseChange {

    private String type;
    private String objectName;
    private String operation;
    private String scriptFile;
    private String status;
    private LocalDateTime timestamp;

    // CONSTRUTOR CORRIGIDO: Aceita exatamente os 5 dados que o Tracker envia
    public DatabaseChange(String type, String objectName, String operation, String scriptFile, String status) {
        this.type = type;
        this.objectName = objectName;
        this.operation = operation;
        this.scriptFile = scriptFile;
        this.status = status;

        // A data é gerada automaticamente aqui, não precisa vir de fora
        this.timestamp = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return String.format("[%s] %s %s em %s (%s)", status, operation, objectName, scriptFile, type);
    }

    // Getters
    public String getType() { return type; }
    public String getObjectName() { return objectName; }
    public String getOperation() { return operation; }
    public String getScriptFile() { return scriptFile; }
    public String getStatus() { return status; }
    public LocalDateTime getTimestamp() { return timestamp; }

    // Setter
    public void setStatus(String status) {
        this.status = status;
    }
}