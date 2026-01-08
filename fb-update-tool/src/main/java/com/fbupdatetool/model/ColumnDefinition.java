package com.fbupdatetool.model;

public class ColumnDefinition {
    private final String name;
    private final String fullDefinition; // Ex: VARCHAR(100) NOT NULL
    private final boolean isNotNull;

    public ColumnDefinition(String name, String fullDefinition) {
        this.name = name.toUpperCase().replace("\"", "").trim();
        this.fullDefinition = fullDefinition.trim();
        this.isNotNull = fullDefinition.toUpperCase().contains("NOT NULL");
    }

    public String getName() { return name; }
    public String getFullDefinition() { return fullDefinition; }
    public boolean isNotNull() { return isNotNull; }
}