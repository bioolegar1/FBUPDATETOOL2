package com.fbupdatetool;

import com.fbupdatetool.service.*;
import com.fbupdatetool.util.TextAreaAppender;
import com.fbupdatetool.view.BackupView;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MainApp extends Application {

    private static final Logger logger = LoggerFactory.getLogger(MainApp.class);

    private BorderPane root;
    private Node viewAtualizador;
    private Node viewSqlEditor;
    private Node viewBackup;
    private Stage primaryStage;

    private final ConfigurationService configService = new ConfigurationService();
    private final HistoryService historyService = new HistoryService();

    private Label lblPathScripts;
    private ListView<ScriptItem> scriptList;
    private CheckBox chkSelectAll;
    private TextField txtHost, txtPort, txtUser, txtDbPath;
    private PasswordField txtPass;
    private TextArea txtLog;
    private Button btnStart, btnStop;
    private ProgressBar progressBar;
    private Label lblStatus;

    private HBox menuBox;
    private Button btnMenuAtu, btnMenuSql, btnMenuBackup, btnMenuHealth, btnMenuHist, btnMenuConfig;

    private final String ESTILO_MENU_NORMAL = "-fx-background-color: transparent; -fx-text-fill: #E0E0E0; -fx-font-weight: bold; -fx-font-size: 14px; -fx-cursor: hand;";
    private final String ESTILO_MENU_ATIVO  = "-fx-background-color: #1B5E20; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-background-radius: 4;";

    private static class ScriptItem {
        Path path;
        boolean executed;
        boolean selected;
        String displayText;

        public ScriptItem(Path path) {
            this.path = path;
            this.displayText = path.getFileName().toString();
            this.selected = true;
            this.executed = false;
        }
        @Override public String toString() { return displayText; }
    }

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;

        if (!validarAmbienteFirebird()) {
            Platform.exit(); System.exit(0); return;
        }

        root = new BorderPane();
        viewAtualizador = criarTelaAtualizador();
        viewSqlEditor = criarPlaceholder("Editor SQL (Em Breve)");
        BackupView backupViewObj = new BackupView();
        viewBackup = backupViewObj.getView();

        carregarConfiguracoesIniciais();

        root.setTop(criarCabecalhoComMenu());
        root.setCenter(viewAtualizador);

        PrintStream guiPrintStream = new PrintStream(new ConsoleRecorder(txtLog), true);
        System.setOut(guiPrintStream);
        System.setErr(guiPrintStream);

        Scene scene = new Scene(root, 1100, 750);
        stage.setTitle("Firebird UpdateTool - v3.3 (Liberado)");
        stage.setScene(scene);
        stage.show();
    }

    private void acaoIniciarAtualizacao() {
        String dbPath = txtDbPath.getText().trim();
        if (dbPath.isEmpty()) { alertaErro("Selecione o arquivo do banco de dados!"); return; }

        List<Path> scriptsParaRodar = new ArrayList<>();
        for (ScriptItem item : scriptList.getItems()) {
            if (item.selected) scriptsParaRodar.add(item.path);
        }

        if (scriptsParaRodar.isEmpty()) { alertaErro("Nenhum script selecionado!"); return; }

        btnStart.setDisable(true);
        btnStop.setDisable(false);
        progressBar.setProgress(0);
        txtLog.clear();
        logToScreen("=== INICIANDO PROCESSO ===");

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                String url = "jdbc:firebirdsql://" + txtHost.getText() + ":" + txtPort.getText() + "/" + dbPath + "?encoding=WIN1252";
                String u = txtUser.getText();
                String p = txtPass.getText();

                // 1. SIMULAÇÃO
                logToScreen(">> Rodando Simulação...");
                SimulationService simulator = new SimulationService();
                boolean simOk = simulator.runSimulation(url, u, p, scriptsParaRodar);

                if (!simOk) {
                    throw new Exception("Simulação falhou. O processo foi abortado.");
                }
                logToScreen("✅ Simulação OK. Iniciando execução real...");
                Thread.sleep(1000);

                // 2. EXECUÇÃO REAL
                ScriptExecutor executor = new ScriptExecutor();
                ScriptSorter sorter = new ScriptSorter();
                sorter.sortScripts(scriptsParaRodar);

                int total = scriptsParaRodar.size();
                int current = 0;

                for (Path script : scriptsParaRodar) {
                    current++;
                    updateProgress(current, total);
                    updateMessage("Executando " + script.getFileName() + "...");
                    logToScreen(">>> Processando script: " + script.getFileName());

                    Connection conn = null;
                    try {
                        conn = DriverManager.getConnection(url, u, p);
                        conn.setAutoCommit(false);

                        boolean success = executor.executeScript(conn, script);

                        if (!success) {
                            try { conn.rollback(); } catch(SQLException exRb) {}
                            throw new Exception("Falha crítica no script: " + script.getFileName());
                        }
                        conn.commit();

                    } catch (SQLException e) {
                        throw new Exception("Erro no script " + script.getFileName() + ": " + e.getMessage());
                    } finally {
                        if (conn != null) try { conn.close(); } catch (SQLException exClose) {}
                    }

                    String fName = script.getFileName().toString();
                    Platform.runLater(() -> marcarItemComoExecutado(fName));
                }

                // 3. PROCESSAR FILA
                logToScreen(">> Processando Fila de Dependências...");
                Connection connFila = null;
                try {
                    connFila = DriverManager.getConnection(url, u, p);
                    connFila.setAutoCommit(false);
                    executor.processDeferredQueue(connFila);
                    connFila.commit();
                } catch (Exception e) {
                    logToScreen("Erro na fila: " + e.getMessage());
                } finally {
                    if (connFila != null) try { connFila.close(); } catch (SQLException e) {}
                }

                // 4. HEALTH CHECK
                logToScreen(">> Executando Health Check Final...");
                Connection connHealth = null;
                try {
                    connHealth = DriverManager.getConnection(url, u, p);
                    DatabaseHealthService healthService = new DatabaseHealthService();
                    if (healthService.checkDatabaseHealth(connHealth)) {
                        logToScreen("🚀 SUCESSO TOTAL! Banco Atualizado.");
                    } else {
                        throw new Exception("Erro no Health Check final.");
                    }
                } finally {
                    if (connHealth != null) try { connHealth.close(); } catch (SQLException e) {}
                }

                return null;
            }

            @Override
            protected void succeeded() {
                lblStatus.setText("✅ Concluído");
                lblStatus.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                progressBar.progressProperty().unbind();
                progressBar.setProgress(1);
                btnStart.setDisable(false);
                btnStop.setDisable(true);
                alertaSucesso("Atualização Concluída!");
            }

            @Override
            protected void failed() {
                lblStatus.setText("❌ Falha");
                lblStatus.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                progressBar.progressProperty().unbind();
                progressBar.setProgress(0);
                btnStart.setDisable(false);
                btnStop.setDisable(true);
                Throwable ex = getException();
                alertaErro(ex.getMessage());
                logToScreen("=== ERRO: " + ex.getMessage() + " ===");
            }

            @Override
            protected void cancelled() {
                lblStatus.setText("⚠️ Cancelado");
                btnStart.setDisable(false);
                logToScreen("=== PROCESSO CANCELADO ===");
            }
        };

        progressBar.progressProperty().bind(task.progressProperty());
        btnStop.setOnAction(e -> task.cancel(true));
        new Thread(task).start();
    }

    // =================================================================
    // CORREÇÃO 1: Checkbox "Marcar Todos" agora afeta TUDO
    // CORREÇÃO 2: Itens executados NÃO ficam desabilitados
    // =================================================================

    private void configurarRenderizacaoLista() {
        scriptList.setCellFactory(lv -> new ListCell<>() {
            final CheckBox cb = new CheckBox();
            @Override protected void updateItem(ScriptItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); } else {
                    cb.setText(item.displayText);
                    cb.setSelected(item.selected);

                    // Se já rodou, fica verde, mas PERMANECE HABILITADO
                    if (item.executed) {
                        cb.setStyle("-fx-text-fill: #2E7D32; -fx-font-weight: bold;");
                    } else {
                        cb.setStyle("-fx-text-fill: black;");
                    }
                    // cb.setDisable(item.executed);  <-- REMOVIDO!

                    cb.setOnAction(e -> item.selected = cb.isSelected());
                    setGraphic(cb);
                }
            }
        });
    }

    // Método criado para configurar o evento do chkSelectAll corretamente
    private void configurarAcaoMarcarTodos() {
        chkSelectAll.setOnAction(e -> {
            boolean marcar = chkSelectAll.isSelected();
            for(ScriptItem item : scriptList.getItems()) {
                // Marca TUDO, sem preconceito com quem já rodou
                item.selected = marcar;
            }
            scriptList.refresh();
        });
    }

    private void verificarHistoricoNoBanco() {
        String db = txtDbPath.getText().trim();
        if (db.isEmpty() || !new File(db).exists()) return;

        new Thread(() -> {
            String url = "jdbc:firebirdsql://" + txtHost.getText() + ":" + txtPort.getText() + "/" + db + "?encoding=WIN1252";
            try (Connection conn = DriverManager.getConnection(url, txtUser.getText(), txtPass.getText())) {
                Platform.runLater(() -> logToScreen(">> Verificando histórico..."));

                for (ScriptItem item : scriptList.getItems()) {
                    // --- CORREÇÃO: Calcula o Hash real do arquivo para comparação ---
                    String currentHash = null;
                    try {
                        currentHash = com.fbupdatetool.util.ChecksumUtil.calculateHash(item.path);
                    } catch (Exception e) {
                        // Se der erro ao ler (ex: arquivo aberto), segue como null
                    }

                    // Passa o hash calculado em vez de 'null'
                    boolean jaRodou = historyService.isScriptExecuted(conn, item.path.getFileName().toString(), currentHash);

                    if (jaRodou) {
                        item.executed = true;
                        item.selected = false;
                        item.displayText = item.path.getFileName().toString() + " (Já Executado)";
                    } else {
                        item.executed = false;
                        item.selected = true;
                        item.displayText = item.path.getFileName().toString();
                    }
                }
                Platform.runLater(() -> { scriptList.refresh(); logToScreen(">> Histórico atualizado."); });
            } catch (Exception e) { Platform.runLater(() -> logToScreen("Aviso: Histórico indisponível (" + e.getMessage() + ")")); }
        }).start();
    }

    // --- Helpers UI e Boilerplate ---
    private static class ConsoleRecorder extends OutputStream {
        private final TextArea ta;
        public ConsoleRecorder(TextArea ta) { this.ta = ta; }
        @Override public void write(int b) { Platform.runLater(() -> ta.appendText(String.valueOf((char) b))); }
        @Override public void write(byte[] b, int off, int len) { String s = new String(b, off, len); Platform.runLater(() -> ta.appendText(s)); }
    }
    private void logToScreen(String msg) { Platform.runLater(() -> txtLog.appendText(msg + "\n")); }

    private void marcarItemComoExecutado(String filename) {
        for (ScriptItem item : scriptList.getItems()) {
            if (item.path.getFileName().toString().equals(filename)) {
                item.executed = true;
                item.selected = false;
                item.displayText = filename + " (Concluído)";
            }
        }
        scriptList.refresh();
    }

    private boolean validarAmbienteFirebird() {
        FirebirdProcessDetector detector = new FirebirdProcessDetector();
        String p = configService.getLastDbPort(); String porta = (p != null && !p.isEmpty()) ? p : "3050";
        int procs = detector.countFirebirdProcesses();
        if (procs == 0) return mostrarErroFirebirdAusente();
        else if (procs > 1) { String s = mostrarDialogoMultiplasInstancias(porta); if (s == null) return false; porta = s; }
        return validarPortaFirebird(porta);
    }
    private boolean mostrarErroFirebirdAusente() {
        Alert a = new Alert(Alert.AlertType.ERROR); a.setTitle("Erro"); a.setContentText("Firebird não detectado.");
        a.getButtonTypes().setAll(new ButtonType("Tentar Novamente"), new ButtonType("Sair", ButtonBar.ButtonData.CANCEL_CLOSE));
        return a.showAndWait().orElse(null).getText().equals("Tentar Novamente") && validarAmbienteFirebird();
    }
    private String mostrarDialogoMultiplasInstancias(String porta) {
        TextInputDialog d = new TextInputDialog(porta); d.setTitle("Config"); d.setHeaderText("Múltiplas instâncias"); d.setContentText("Porta:");
        return d.showAndWait().orElse(null);
    }
    private boolean validarPortaFirebird(String porta) {
        try { if (DatabaseService.checkFirebirdService(Integer.parseInt(porta))) { configService.saveLastDbPort(porta); return true; } return mostrarErroPortaInacessivel(porta); } catch (Exception e) { return validarAmbienteFirebird(); }
    }
    private boolean mostrarErroPortaInacessivel(String porta) {
        Alert a = new Alert(Alert.AlertType.WARNING); a.setContentText("Porta " + porta + " inacessível.");
        ButtonType b3050 = new ButtonType("Tentar 3050");
        a.getButtonTypes().setAll(b3050, new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE));
        return a.showAndWait().orElse(null) == b3050 && validarPortaFirebird("3050");
    }
    private Node criarTelaAtualizador() {
        VBox left = new VBox(10); left.setPadding(new Insets(10));
        HBox head = new HBox(10); head.setAlignment(Pos.CENTER_LEFT);
        Label lbl = new Label("Scripts"); lbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        Button btnFolder = new Button("Alterar Pasta");
        btnFolder.setOnAction(e -> {
            DirectoryChooser dc = new DirectoryChooser(); String last = configService.getLastScriptFolder();
            if (last != null) dc.setInitialDirectory(new File(last));
            File f = dc.showDialog(primaryStage);
            if (f != null) { atualizarListaDeScripts(f); verificarHistoricoNoBanco(); }
        });
        Region r = new Region(); HBox.setHgrow(r, Priority.ALWAYS); head.getChildren().addAll(lbl, r, btnFolder);
        lblPathScripts = new Label("..."); lblPathScripts.setTextFill(Color.GRAY);
        chkSelectAll = new CheckBox("Marcar Todos"); chkSelectAll.setSelected(true);
        scriptList = new ListView<>();
        configurarRenderizacaoLista();

        // Ativa a lógica nova do Marcar Todos
        configurarAcaoMarcarTodos();

        VBox.setVgrow(scriptList, Priority.ALWAYS);
        left.getChildren().addAll(head, lblPathScripts, chkSelectAll, scriptList);

        VBox right = new VBox(15); right.setPadding(new Insets(10));
        TitledPane tpConf = new TitledPane("Configuração", criarGridConexao()); tpConf.setCollapsible(false);
        TitledPane tpDb = new TitledPane("Banco de Dados", criarSelecaoBanco()); tpDb.setCollapsible(false);
        VBox pBox = new VBox(5);
        HBox bar = new HBox(10); bar.setAlignment(Pos.CENTER_RIGHT);
        btnStop = new Button("Parar"); btnStop.setDisable(true); btnStop.setStyle("-fx-background-color: #f44336; -fx-text-fill: white;");
        btnStart = new Button("Iniciar"); btnStart.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;");
        btnStart.setOnAction(e -> acaoIniciarAtualizacao());
        bar.getChildren().addAll(new CheckBox("Backup"), new Region(), btnStop, btnStart);
        lblStatus = new Label("Aguardando...");
        progressBar = new ProgressBar(0); progressBar.setMaxWidth(Double.MAX_VALUE); progressBar.setStyle("-fx-accent: #4CAF50;");
        pBox.getChildren().addAll(bar, lblStatus, progressBar);
        TitledPane tpLog = new TitledPane("Log", txtLog = new TextArea()); tpLog.setCollapsible(false); tpLog.setMaxHeight(Double.MAX_VALUE);
        txtLog.setStyle("-fx-control-inner-background: black; -fx-text-fill: lightgreen; -fx-font-family: 'Consolas';"); txtLog.setEditable(false);
        VBox.setVgrow(tpLog, Priority.ALWAYS);
        configurarLogbackParaGUI();
        right.getChildren().addAll(tpConf, tpDb, pBox, tpLog);
        SplitPane split = new SplitPane(left, right); split.setDividerPositions(0.4);
        return split;
    }
    private void configurarLogbackParaGUI() {
        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.detachAndStopAllAppenders();
        TextAreaAppender appender = new TextAreaAppender();
        appender.setContext(root.getLoggerContext()); appender.setName("GUI"); appender.setTextArea(txtLog); appender.start();
        root.addAppender(appender);
    }
    private void carregarConfiguracoesIniciais() {
        String last = configService.getLastScriptFolder();
        if (last != null && new File(last).exists()) atualizarListaDeScripts(new File(last));
        txtHost.setText("localhost");
        String p = configService.getLastDbPort(); txtPort.setText(p != null ? p : "3050");
        txtUser.setText("SYSDBA"); txtPass.setText("masterkey");
    }
    private void atualizarListaDeScripts(File folder) {
        lblPathScripts.setText(folder.getAbsolutePath()); scriptList.getItems().clear();
        try (Stream<Path> paths = Files.list(folder.toPath())) {
            List<Path> sorted = paths.filter(p -> p.toString().toLowerCase().endsWith(".sql")).sorted().collect(Collectors.toList());
            if(sorted.isEmpty()) alertaErro("Sem arquivos .sql");
            ScriptSorter sorter = new ScriptSorter(); sorter.sortScripts(sorted);
            for (Path p : sorted) scriptList.getItems().add(new ScriptItem(p));
            configService.saveLastScriptFolder(folder.getAbsolutePath());
        } catch (IOException e) { alertaErro("Erro pasta: " + e.getMessage()); }
    }
    private GridPane criarGridConexao() {
        GridPane g = new GridPane(); g.setHgap(10); g.setVgap(10); g.setPadding(new Insets(10));
        txtHost = new TextField(); txtPort = new TextField(); txtUser = new TextField(); txtPass = new PasswordField();
        g.add(new Label("Host"),0,0); g.add(txtHost,0,1); g.add(new Label("Port"),1,0); g.add(txtPort,1,1);
        g.add(new Label("User"),2,0); g.add(txtUser,2,1); g.add(new Label("Pass"),3,0); g.add(txtPass,3,1);
        return g;
    }
    private HBox criarSelecaoBanco() {
        HBox b = new HBox(10); b.setPadding(new Insets(10));
        txtDbPath = new TextField(); HBox.setHgrow(txtDbPath, Priority.ALWAYS);
        Button s = new Button("Selecionar"); s.setOnAction(e -> acaoSelecionarBanco());
        b.getChildren().addAll(txtDbPath, s); return b;
    }
    private void acaoSelecionarBanco() {
        FileChooser fc = new FileChooser(); fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("FB DB", "*.FDB", "*.GDB"));
        File f = fc.showOpenDialog(primaryStage);
        if (f != null) { txtDbPath.setText(f.getAbsolutePath()); verificarHistoricoNoBanco(); }
    }
    private HBox criarCabecalhoComMenu() {
        HBox h = new HBox(20); h.setPadding(new Insets(10,20,10,20)); h.setAlignment(Pos.CENTER_LEFT); h.setStyle("-fx-background-color: #2E7D32;");
        Label l1 = new Label("🚀"); l1.setFont(Font.font(24)); l1.setTextFill(Color.WHITE);
        VBox v = new VBox(-2); Label l2 = new Label("FB UpdateTool"); l2.setFont(Font.font(18)); l2.setTextFill(Color.WHITE); v.getChildren().addAll(l2, new Label("Pillar"));
        menuBox = new HBox(5); menuBox.setAlignment(Pos.CENTER_RIGHT);
        btnMenuAtu = criarBotaoMenu("ATUALIZADOR"); btnMenuAtu.setStyle(ESTILO_MENU_ATIVO);
        btnMenuBackup = criarBotaoMenu("BACKUP"); btnMenuSql = criarBotaoMenu("SQL");
        btnMenuHealth = criarBotaoMenu("HEALTH"); btnMenuHist = criarBotaoMenu("HIST"); btnMenuConfig = criarBotaoMenu("CONFIG");
        btnMenuAtu.setOnAction(e -> trocarTela(viewAtualizador, btnMenuAtu));
        btnMenuBackup.setOnAction(e -> trocarTela(viewBackup, btnMenuBackup));
        btnMenuSql.setOnAction(e -> trocarTela(viewSqlEditor, btnMenuSql));
        menuBox.getChildren().addAll(btnMenuAtu, btnMenuBackup, btnMenuSql, btnMenuHealth, btnMenuHist, btnMenuConfig);
        Region r = new Region(); HBox.setHgrow(r, Priority.ALWAYS); h.getChildren().addAll(l1, v, r, menuBox);
        return h;
    }
    private Button criarBotaoMenu(String t) { Button b = new Button(t); b.setStyle(ESTILO_MENU_NORMAL); return b; }
    private void trocarTela(Node v, Button b) { menuBox.getChildren().forEach(n -> n.setStyle(ESTILO_MENU_NORMAL)); b.setStyle(ESTILO_MENU_ATIVO); root.setCenter(v); }
    private Node criarPlaceholder(String t) { return new StackPane(new Label(t)); }
    private void alertaErro(String msg) { Platform.runLater(() -> { Alert a = new Alert(Alert.AlertType.ERROR); a.setContentText(msg); a.showAndWait(); }); }
    private void alertaSucesso(String msg) { Platform.runLater(() -> { Alert a = new Alert(Alert.AlertType.INFORMATION); a.setContentText(msg); a.showAndWait(); }); }

    public static void main(String[] args) { launch(args); }
}