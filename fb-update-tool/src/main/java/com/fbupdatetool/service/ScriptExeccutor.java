package com.fbupdatetool.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScriptExeccutor {

    private static final Logger logger = LoggerFactory.getLogger(ScriptExeccutor.class);

    private final ScriptParser parser;
    private final HistoryService historyService;
    private final DatabaseChangeTracker tracker;
    private final FirebirdErrorTranslator errorTranslator;



    public ScriptExeccutor() {
        this.parser = new ScriptParser();
        this.historyService = new HistoryService();
        this.tracker = new DatabaseChangeTracker();
        this.errorTranslator = new FirebirdErrorTranslator();
    }

    public DatabaseChangeTracker getTracker() { return tracker; }


}
