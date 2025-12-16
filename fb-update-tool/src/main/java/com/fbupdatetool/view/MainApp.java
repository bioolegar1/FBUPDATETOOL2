package com.fbupdatetool.view;

import com.fbupdatetool.service.*;
import com.fbupdatetool.util.TextAreaAppender;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MainApp extends Application {

    private static final Logger logger = LoggerFactory.getLogger(MainApp.class);

    private BorderPane root;
    private Node viewAtualizador;
    private Node viewSqlEditor;
    private Stage primaryStage;

    private final ConfigurationService configService = new ConfigurationService();

    private Label lblPathScripts;
    private ListView<CheckBox> scriptList;
    private CheckBox chkSelectAll;

    private TextField txtHost;
    private TextField txtPort;
    private TextField txtUser;
    private PasswordField txtPass;
    private TextField txtDbPath;

    private TextArea txtLog;
    private Button btnStart;
    private Button btnStop;

    // === NOVO: Componentes de Progresso ===
    private ProgressBar progressBar;
    private Label lblStatus;

    private final String ESTILO_MENU_NORMAL = "-fx-background-color: transparent; -fx-text-fill: #E0E0E0; -fx-font-weight: bold; -fx-font-size: 14px; -fx-cursor: hand;";
    private final String ESTILO_MENU_ATIVO  = "-fx-background-color: #1B5E20; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-background-radius: 4;";

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        root = new BorderPane();

        viewAtualizador = criarTelaAtualizador();
        viewSqlEditor = criarPlaceholder("Editor SQL (Em Breve)");

        carregarConfiguracoesIniciais();

        root.setTop(criarCabecalhoComMenu());
        root.setCenter(viewAtualizador);

        Scene scene = new Scene(root, 1100, 700);
        stage.setTitle("Firebird UpdateTool - v2.0");
        stage.setScene(scene);
        stage.show();
    }

    private void carregarConfiguracoesIniciais() {
        String lastFolder = configService.getLastScriptFolder();
        if (lastFolder != null && !lastFolder.isEmpty()) {
            File folder = new File(lastFolder);
            if (folder.exists()) atualizarListaDeScripts(folder);
        }

        txtHost.setText("localhost");
        String portaSalva = configService.getLastDbPort();
        txtPort.setText(portaSalva != null ? portaSalva : "3050");
        txtUser.setText("SYSDBA");
        txtPass.setText("masterkey");
    }

    private void acaoSelecionarBanco() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Selecione o Banco de Dados Firebird");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Banco Firebird", "*.FDB", "*.GDB", "*.IB"));

        File selected = fc.showOpenDialog(primaryStage);
        if (selected != null) {
            txtDbPath.setText(selected.getAbsolutePath());
            logToScreen("Banco selecionado: " + selected.getName());
        }
    }

    private void acaoIniciarAtualizacao() {
        String dbPath = txtDbPath.getText().trim();
        if (dbPath.isEmpty()) {
            alertaErro("Selecione o arquivo do banco de dados (.FDB/.GDB)!");
            return;
        }

        List<Path> scriptsParaRodar = new ArrayList<>();
        for (CheckBox cb : scriptList.getItems()) {
            if (cb.isSelected() && cb.getUserData() instanceof Path) {
                scriptsParaRodar.add((Path) cb.getUserData());
            }
        }

        if (scriptsParaRodar.isEmpty()) {
            alertaErro("Nenhum script selecionado na lista!");
            return;
        }

        // Trava UI
        btnStart.setDisable(true);
        btnStop.setDisable(false);
        progressBar.setProgress(0);
        txtLog.clear();
        logToScreen("=== INICIANDO PROCESSO DE ATUALIZAÇÃO ===");
        logToScreen("Scripts selecionados: " + scriptsParaRodar.size());

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                String jdbcUrl = "jdbc:firebirdsql://" + txtHost.getText() + ":" + txtPort.getText() + "/" + dbPath + "?encoding=WIN1252";
                String user = txtUser.getText();
                String pass = txtPass.getText();

                // Conexão real com try-with-resources (fecha automaticamente)
                try (Connection conn = DriverManager.getConnection(jdbcUrl, user, pass)) {

                    // FASE 1: SIMULAÇÃO
                    updateMessage("Rodando Simulação (Dry Run)...");
                    updateProgress(-1, 1); // Indeterminado
                    UpdateService updateService = new UpdateService();
                    boolean simulacaoOK = updateService.executarAtualizacao(conn, scriptsParaRodar, true);

                    if (!simulacaoOK) {
                        throw new Exception("Simulação falhou. Verifique os logs.");
                    }

                    // FASE 2: ATUALIZAÇÃO REAL (se simulação OK)
                    updateMessage("Rodando Atualização Real...");
                    updateProgress(-1, 1);
                    boolean atualizacaoOK = updateService.executarAtualizacao(conn, scriptsParaRodar, false);

                    if (atualizacaoOK) {
                        updateMessage("Atualização concluída com sucesso!");
                        updateProgress(1, 1);
                    } else {
                        throw new Exception("Atualização real falhou. Verifique os logs.");
                    }
                } catch (SQLException e) {
                    logger.error("Erro de conexão com o banco: {}", e.getMessage());
                    throw new Exception("Falha na conexão com o banco de dados: " + e.getMessage());
                }

                return null;
            }

            @Override
            protected void succeeded() {
                // IMPORTANTE: Remove o binding ANTES de modificar
                lblStatus.textProperty().unbind();
                progressBar.progressProperty().unbind();

                btnStart.setDisable(false);
                btnStop.setDisable(true);
                lblStatus.setText("✅ Concluído com Sucesso");
                lblStatus.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: green;");
                progressBar.setProgress(1.0);
                logToScreen("=== ATUALIZAÇÃO CONCLUÍDA COM SUCESSO ===");
            }

            @Override
            protected void failed() {
                // IMPORTANTE: Remove o binding ANTES de modificar
                lblStatus.textProperty().unbind();
                progressBar.progressProperty().unbind();

                btnStart.setDisable(false);
                btnStop.setDisable(true);
                lblStatus.setText("❌ Falha na Execução");
                lblStatus.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: red;");
                progressBar.setProgress(0);
                logToScreen("=== ERRO NA ATUALIZAÇÃO ===");

                Throwable ex = getException();
                if (ex != null) {
                    logger.error("Erro durante atualização", ex);
                    logToScreen("ERRO: " + ex.getMessage());
                }
            }

            @Override
            protected void cancelled() {
                // IMPORTANTE: Remove o binding ANTES de modificar
                lblStatus.textProperty().unbind();
                progressBar.progressProperty().unbind();

                btnStart.setDisable(false);
                btnStop.setDisable(true);
                lblStatus.setText("⚠️ Cancelado pelo Usuário");
                lblStatus.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: orange;");
                progressBar.setProgress(0);
                logToScreen("=== PROCESSO CANCELADO PELO USUÁRIO ===");
            }
        };

        // Faz o binding APENAS quando a task iniciar
        progressBar.progressProperty().bind(task.progressProperty());
        lblStatus.textProperty().bind(task.messageProperty());

        btnStop.setOnAction(e -> task.cancel(true));

        new Thread(task).start();
    }

    private void logToScreen(String mensagem) {
        Platform.runLater(() -> txtLog.appendText(mensagem + "\n"));
    }

    private void alertaErro(String mensagem) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erro");
            alert.setHeaderText(null);
            alert.setContentText(mensagem);
            alert.showAndWait();
        });
    }

    private void atualizarListaDeScripts(File folder) {
        try {
            scriptList.getItems().clear();

            // Coleta apenas arquivos .sql da pasta raiz (sem subpastas)
            List<Path> sqlFiles = Files.list(folder.toPath())
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().toLowerCase().endsWith(".sql"))
                    .collect(Collectors.toList());

            if (sqlFiles.isEmpty()) {
                alertaErro("Nenhum arquivo .sql encontrado na pasta selecionada!");
                return;
            }

            // Ordena usando ScriptSorter
            ScriptSorter sorter = new ScriptSorter();
            sorter.sortScripts(sqlFiles);

            // Cria CheckBox para cada script
            for (Path path : sqlFiles) {
                CheckBox cb = new CheckBox(path.getFileName().toString());
                cb.setSelected(true);
                cb.setUserData(path);
                scriptList.getItems().add(cb);
            }

            logger.info("Carregados {} scripts da pasta: {}", sqlFiles.size(), folder.getAbsolutePath());
        } catch (IOException e) {
            logger.error("Erro ao ler pasta de scripts: {}", e.getMessage());
            alertaErro("Erro ao carregar scripts: " + e.getMessage());
        }
    }

    private Node criarTelaAtualizador() {
        SplitPane split = new SplitPane();

        // ESQUERDA - Lista de Scripts
        VBox leftPane = new VBox(10);
        leftPane.setPadding(new Insets(10));

        Label headerList = new Label("Scripts Disponíveis");
        headerList.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));

        lblPathScripts = new Label("Pasta: Nenhuma selecionada");
        lblPathScripts.setStyle("-fx-text-fill: gray;");

        Button btnSelecionarPasta = new Button("Selecionar Pasta de Scripts");
        btnSelecionarPasta.setOnAction(e -> {
            DirectoryChooser dc = new DirectoryChooser();
            dc.setTitle("Selecione a pasta com os scripts SQL");
            File selected = dc.showDialog(primaryStage);
            if (selected != null) {
                atualizarListaDeScripts(selected);
                lblPathScripts.setText("Pasta: " + selected.getAbsolutePath());
                configService.saveLastScriptFolder(selected.getAbsolutePath());
            }
        });

        chkSelectAll = new CheckBox("Selecionar Todos");
        chkSelectAll.setSelected(true);

        scriptList = new ListView<>();
        VBox.setVgrow(scriptList, Priority.ALWAYS);

        chkSelectAll.setOnAction(e -> {
            boolean val = chkSelectAll.isSelected();
            scriptList.getItems().forEach(cb -> cb.setSelected(val));
        });

        leftPane.getChildren().addAll(headerList, lblPathScripts, btnSelecionarPasta, chkSelectAll, scriptList);

        // DIREITA
        VBox rightPane = new VBox(15);
        rightPane.setPadding(new Insets(10));

        TitledPane paneConfig = new TitledPane("Configuração de Conexão", criarGridConexao());
        paneConfig.setCollapsible(false);

        TitledPane paneDb = new TitledPane("Banco de Dados", criarSelecaoBanco());
        paneDb.setCollapsible(false);

        HBox bar = new HBox(10);
        bar.setAlignment(Pos.CENTER_RIGHT);
        btnStop = new Button("Parar");
        btnStop.setDisable(true);
        btnStop.setStyle("-fx-background-color: #f44336; -fx-text-fill: white;");
        btnStart = new Button("Iniciar Atualização");
        btnStart.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;");
        btnStart.setOnAction(e -> acaoIniciarAtualizacao());

        bar.getChildren().addAll(new CheckBox("Fazer Backup"), new Region(), new Button("Apenas Backup"), btnStop, btnStart);

        // === BARRA DE PROGRESSO E STATUS ===
        VBox progressBox = new VBox(8);
        progressBox.setPadding(new Insets(10, 0, 0, 0));

        lblStatus = new Label("Pronto");
        lblStatus.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");

        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(Double.MAX_VALUE);
        progressBar.setStyle("-fx-accent: #4CAF50;");

        progressBox.getChildren().addAll(lblStatus, progressBar);

        TitledPane paneLog = new TitledPane("Log de Execução", txtLog = new TextArea());
        paneLog.setCollapsible(false);
        paneLog.setMaxHeight(Double.MAX_VALUE);
        txtLog.setStyle("-fx-control-inner-background: black; -fx-text-fill: lightgreen; -fx-font-family: 'Consolas'; -fx-font-size: 12;");
        txtLog.setEditable(false);
        VBox.setVgrow(paneLog, Priority.ALWAYS);

        // Configura Logback para UI - REMOVE DUPLICAÇÕES
        ch.qos.logback.classic.Logger rootLogger =
                (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);

        // SOLUÇÃO: Remove TODOS os appenders existentes (console, arquivo, etc)
        rootLogger.detachAndStopAllAppenders();

        // Adiciona APENAS o appender da UI
        TextAreaAppender textAreaAppender = new TextAreaAppender();
        textAreaAppender.setContext(rootLogger.getLoggerContext());
        textAreaAppender.setName("GUI_TEXTAREA_APPENDER");
        textAreaAppender.setTextArea(txtLog);
        textAreaAppender.start();

        rootLogger.addAppender(textAreaAppender);
        rootLogger.setLevel(ch.qos.logback.classic.Level.INFO);

        logger.info("==================================================");
        logger.info("   Firebird UpdateTool v2.0 - Interface iniciada");
        logger.info("   Log configurado para exibição na janela");
        logger.info("==================================================");

        rightPane.getChildren().addAll(paneConfig, paneDb, bar, progressBox, paneLog);

        split.setDividerPositions(0.4);
        split.getItems().addAll(leftPane, rightPane);
        return split;
    }

    private GridPane criarGridConexao() {
        GridPane grid = new GridPane();
        grid.setHgap(25); // Espaço horizontal entre as colunas
        grid.setVgap(10);  // Espaço vertical entre o Label e o Campo (reduzi para ficar mais justo)
        grid.setPadding(new Insets(15));

        // --- COLUNA 0: Servidor ---
        grid.add(new Label("Servidor"), 0, 0);        // Coluna 0, Linha 0 (Topo)
        grid.add(txtHost = new TextField(), 0, 1);    // Coluna 0, Linha 1 (Baixo)

        // --- COLUNA 1: Porta ---
        grid.add(new Label("Porta"), 1, 0);           // Coluna 1, Linha 0
        grid.add(txtPort = new TextField(), 1, 1);    // Coluna 1, Linha 1

        // --- COLUNA 2: Usuário ---
        grid.add(new Label("Usuário"), 2, 0);         // Coluna 2, Linha 0
        grid.add(txtUser = new TextField(), 2, 1);    // Coluna 2, Linha 1

        // --- COLUNA 3: Senha ---
        grid.add(new Label("Senha"), 3, 0);           // Coluna 3, Linha 0
        grid.add(txtPass = new PasswordField(), 3, 1);// Coluna 3, Linha 1

        // Ajuste de largura: Como agora são 4 em uma linha, talvez precise reduzir a largura
        // ou usar um valor menor para caber na tela
        txtHost.setPrefWidth(200);
        txtPort.setPrefWidth(80);  // Porta geralmente precisa de menos espaço
        txtUser.setPrefWidth(135);
        txtPass.setPrefWidth(135);

        return grid;
    }

    private HBox criarSelecaoBanco() {
        HBox boxDb = new HBox(10);
        boxDb.setPadding(new Insets(10));
        txtDbPath = new TextField();
        HBox.setHgrow(txtDbPath, Priority.ALWAYS);
        Button btnSelDb = new Button("Selecionar");
        btnSelDb.setOnAction(e -> acaoSelecionarBanco());
        boxDb.getChildren().addAll(txtDbPath, btnSelDb);
        return boxDb;
    }

    private HBox criarCabecalhoComMenu() {
        HBox header = new HBox(20);
        header.setPadding(new Insets(10, 20, 10, 20));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: #2E7D32;");

        Label lblIcon = new Label("🚀");
        lblIcon.setFont(Font.font(24));
        lblIcon.setTextFill(Color.WHITE);

        VBox titleBox = new VBox(-2);
        Label lblTitle = new Label("Firebird UpdateTool");
        lblTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        lblTitle.setTextFill(Color.WHITE);
        Label lblSub = new Label("Soluções Pillar");
        lblSub.setFont(Font.font("Segoe UI", 11));
        lblSub.setTextFill(Color.LIGHTGRAY);
        titleBox.getChildren().addAll(lblTitle, lblSub);

        HBox menuBox = new HBox(5);
        menuBox.setAlignment(Pos.CENTER_RIGHT);

        Button btnAtu = new Button("ATUALIZADOR");
        Button btnSql = new Button("SQL EDITOR");
        Button btnHealth = new Button("HEALTH CHECK");
        Button btnHist = new Button("HISTÓRICO");
        Button btnConfig = new Button("CONFIG");

        btnAtu.setStyle(ESTILO_MENU_ATIVO);
        btnSql.setStyle(ESTILO_MENU_NORMAL);
        btnHealth.setStyle(ESTILO_MENU_NORMAL);
        btnHist.setStyle(ESTILO_MENU_NORMAL);
        btnConfig.setStyle(ESTILO_MENU_NORMAL);

        btnAtu.setOnAction(e -> {
            limparEstilos(menuBox);
            btnAtu.setStyle(ESTILO_MENU_ATIVO);
            root.setCenter(viewAtualizador);
        });

        btnSql.setOnAction(e -> {
            limparEstilos(menuBox);
            btnSql.setStyle(ESTILO_MENU_ATIVO);
            root.setCenter(viewSqlEditor);
        });

        menuBox.getChildren().addAll(btnAtu, btnSql, btnHealth, btnHist, btnConfig);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        header.getChildren().addAll(lblIcon, titleBox, spacer, menuBox);
        return header;
    }

    private void limparEstilos(HBox menuBox) {
        menuBox.getChildren().forEach(node -> node.setStyle(ESTILO_MENU_NORMAL));
    }

    private Node criarPlaceholder(String texto) {
        StackPane p = new StackPane();
        p.getChildren().add(new Label(texto));
        return p;
    }

    public static void main(String[] args) {
        launch(args);
    }
}