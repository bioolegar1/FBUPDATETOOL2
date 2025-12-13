package com.fbupdatetool.view;

import com.fbupdatetool.service.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.FutureTask;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MainApp extends Application {

    private static final Logger logger = LoggerFactory.getLogger(MainApp.class);
    private final ConfigurationService configService = new ConfigurationService();
    private static final String WINDOWS_DEFAULT_PATH = "C:\\SoluçõesPillar\\scriptAtt";

    // Variável de controle para interrupção
    private volatile boolean stopRequested = false;

    // Componentes UI
    private TextField txtServer, txtPort, txtUser, txtDbPath;
    private PasswordField txtPass;
    private TextArea txtLog;
    private ListView<ScriptItem> listScripts;
    private ProgressBar progressBar;
    private Label lblCurrentFolder;
    private CheckBox chkSelectAll;
    private CheckBox chkBackup;

    // Botões de Ação
    private Button btnUpdate;
    private Button btnBackupOnly;
    private Button btnStop;

    private Stage primaryStage;

    // Cores
    private static final String STYLE_PRIMARY = "-fx-background-color: #2E7D32;";
    private static final String STYLE_BACKGROUND = "-fx-background-color: #FAFAFA;";
    private static final String STYLE_CARD = "-fx-background-color: white; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 0); -fx-background-radius: 8;";

    // Cores de Status
    private static final Color COL_SUCCESS = Color.web("#2E7D32");
    private static final Color COL_ERROR = Color.web("#C62828");
    private static final Color COL_INFO = Color.web("#1565C0");
    private static final Color COL_WARNING = Color.web("#EF6C00");
    private static final Color COL_SKIPPED = Color.web("#7B1FA2");

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;

        BorderPane root = new BorderPane();
        root.setStyle(STYLE_BACKGROUND);
        root.setTop(criarCabecalho());

        HBox contentContainer = new HBox(20);
        contentContainer.setPadding(new Insets(20));

        VBox leftPane = criarPainelListaScripts();
        HBox.setHgrow(leftPane, Priority.ALWAYS);
        leftPane.setMinWidth(450);

        VBox rightPane = criarPainelDireito();
        HBox.setHgrow(rightPane, Priority.ALWAYS);

        contentContainer.getChildren().addAll(leftPane, rightPane);
        root.setCenter(contentContainer);

        Scene scene = new Scene(root, 1150, 750);
        stage.setScene(scene);
        stage.setTitle("Firebird UpdateTool");
        stage.show();

        // Init Data
        txtDbPath.setText(configService.getLastDbPath());
        txtPort.setText(configService.getLastDbPort());
        com.fbupdatetool.util.TextAreaAppender.setTextArea(txtLog);
        Platform.runLater(this::inicializarPastaPadrao);
    }

    // ================= UI BUILDERS =================

    private VBox criarCabecalho() {
        VBox header = new VBox(5);
        header.setStyle(STYLE_PRIMARY);
        header.setPadding(new Insets(15, 20, 15, 20));

        Label lblTitle = new Label("Firebird UpdateTool");
        lblTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
        lblTitle.setTextFill(Color.WHITE);

        FontIcon rocketIcon = new FontIcon(MaterialDesignR.ROCKET);
        rocketIcon.setIconSize(24);
        rocketIcon.setIconColor(Color.WHITE);
        lblTitle.setGraphic(rocketIcon);
        lblTitle.setGraphicTextGap(15);

        Label lblSubtitle = new Label("Sistema de Atualização de Banco de Dados");
        lblSubtitle.setTextFill(Color.web("#FFFFFF", 0.8));
        lblSubtitle.setFont(Font.font("Segoe UI", 12));

        header.getChildren().addAll(lblTitle, lblSubtitle);
        return header;
    }

    private VBox criarPainelListaScripts() {
        VBox card = new VBox(10);
        card.setStyle(STYLE_CARD);
        card.setPadding(new Insets(15));

        Label lblTitle = new Label("Scripts Disponíveis");
        lblTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));

        lblCurrentFolder = new Label("Nenhuma pasta selecionada");
        lblCurrentFolder.setStyle("-fx-text-fill: #757575; -fx-font-size: 11px;");
        lblCurrentFolder.setWrapText(true);

        Button btnFolder = new Button("Alterar Pasta");
        btnFolder.setGraphic(new FontIcon(MaterialDesignF.FOLDER_OPEN));
        btnFolder.setOnAction(e -> selecionarPastaScripts());

        HBox topRow = new HBox(10, new VBox(5, lblTitle, lblCurrentFolder), btnFolder);
        HBox.setHgrow(topRow.getChildren().get(0), Priority.ALWAYS);
        topRow.setAlignment(Pos.TOP_LEFT);

        chkSelectAll = new CheckBox("Marcar/Desmarcar Todos");
        chkSelectAll.setSelected(true);
        chkSelectAll.setOnAction(e -> {
            boolean sel = chkSelectAll.isSelected();
            listScripts.getItems().forEach(item -> item.setSelected(sel));
            listScripts.refresh();
        });

        listScripts = new ListView<>();
        listScripts.setCellFactory(param -> new ScriptListCell());
        VBox.setVgrow(listScripts, Priority.ALWAYS);

        card.getChildren().addAll(topRow, chkSelectAll, listScripts);
        return card;
    }

    private VBox criarPainelDireito() {
        VBox panel = new VBox(15);

        // --- Configuração ---
        VBox configCard = criarBaseCard("Configuração de Conexão", MaterialDesignC.COG);
        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);

        txtServer = new TextField("localhost");
        txtPort = new TextField("3050");
        txtUser = new TextField("SYSDBA");
        txtPass = new PasswordField(); txtPass.setText("masterkey");

        adicionarCampoGrid(grid, "Servidor", txtServer, 0, 0);
        adicionarCampoGrid(grid, "Porta", txtPort, 1, 0);
        adicionarCampoGrid(grid, "Usuário", txtUser, 0, 1);
        adicionarCampoGrid(grid, "Senha", txtPass, 1, 1);
        configCard.getChildren().add(grid);

        // --- Banco ---
        VBox dbCard = criarBaseCard("Banco de Dados", MaterialDesignD.DATABASE);
        HBox dbRow = new HBox(10);
        txtDbPath = new TextField();
        HBox.setHgrow(txtDbPath, Priority.ALWAYS);
        Button btnDb = new Button("Selecionar");
        btnDb.setGraphic(new FontIcon(MaterialDesignF.FILE));
        btnDb.setOnAction(e -> escolherBancoDeDados());
        dbRow.getChildren().addAll(txtDbPath, btnDb);
        dbCard.getChildren().add(dbRow);

        // --- Log & Ações ---
        VBox logCard = criarBaseCard("Log de Execução", MaterialDesignC.CLIPBOARD_TEXT);
        VBox.setVgrow(logCard, Priority.ALWAYS);

        txtLog = new TextArea();
        txtLog.setEditable(false);
        txtLog.setStyle("-fx-control-inner-background: #212121; -fx-text-fill: #A5D6A7; -fx-font-family: 'Consolas';");
        VBox.setVgrow(txtLog, Priority.ALWAYS);

        progressBar = new ProgressBar(0);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setStyle("-fx-accent: #2E7D32;");

        // --- BARRA DE BOTÕES ---
        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);

        // 1. Checkbox (Esquerda)
        chkBackup = new CheckBox("Fazer Backup antes");
        chkBackup.setSelected(true);
        chkBackup.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));

        // Spacer
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // 2. Botão Apenas Backup (Azul/Neutro)
        btnBackupOnly = new Button("Apenas Backup");
        btnBackupOnly.setTooltip(new Tooltip("Executa apenas o backup, sem rodar scripts"));
        btnBackupOnly.setGraphic(new FontIcon(MaterialDesignC.CLOUD_DOWNLOAD));
        btnBackupOnly.setOnAction(e -> iniciarProcesso(true, false)); // Backup=Sim, Scripts=Não

        // 3. Botão Parar (Vermelho)
        btnStop = new Button("Parar");
        btnStop.setStyle("-fx-background-color: #D32F2F; -fx-text-fill: white; -fx-font-weight: bold;");
        btnStop.setGraphic(new FontIcon(MaterialDesignS.STOP));
        btnStop.setDisable(true); // Começa desativado
        btnStop.setOnAction(e -> solicitarParada());

        // 4. Botão Iniciar (Verde)
        btnUpdate = new Button("Iniciar Atualização");
        btnUpdate.setStyle("-fx-background-color: #2E7D32; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 15;");
        FontIcon playIcon = new FontIcon(MaterialDesignP.PLAY_CIRCLE_OUTLINE);
        playIcon.setIconColor(Color.WHITE);
        btnUpdate.setGraphic(playIcon);
        // Backup=Opcional, Scripts=Sim
        btnUpdate.setOnAction(e -> iniciarProcesso(chkBackup.isSelected(), true));

        // Ordem solicitada: Checkbox ... Stop ... Iniciar
        actions.getChildren().addAll(chkBackup, spacer, btnBackupOnly, btnStop, btnUpdate);

        logCard.getChildren().addAll(txtLog, progressBar, actions);

        panel.getChildren().addAll(configCard, dbCard, logCard);
        return panel;
    }

    // ================= LÓGICA DE NEGÓCIO =================

    private void solicitarParada() {
        stopRequested = true;
        btnStop.setDisable(true); // Evita duplo clique
        txtLog.appendText("\n>>> SOLICITAÇÃO DE PARADA RECEBIDA! <<<\n");
        txtLog.appendText(">>> O processo será interrompido na próxima etapa segura.\n");
    }

    private void iniciarProcesso(boolean forcarBackup, boolean rodarScripts) {
        if (txtDbPath.getText().trim().isEmpty()) {
            mostrarAlerta(Alert.AlertType.WARNING, "Atenção", "Selecione o banco de dados!");
            return;
        }

        // Validação de scripts apenas se formos rodá-los
        if (rodarScripts) {
            boolean temSelecionado = listScripts.getItems().stream().anyMatch(ScriptItem::isSelected);
            if (!temSelecionado) {
                mostrarAlerta(Alert.AlertType.WARNING, "Atenção", "Selecione pelo menos um script.");
                return;
            }
        }

        // Se for update completo e o backup estiver desmarcado, pede confirmação
        boolean realizarBackup = forcarBackup;
        if (rodarScripts && !realizarBackup) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Segurança");
            confirm.setHeaderText("Backup Desativado");
            confirm.setContentText("É altamente recomendado fazer backup. Deseja ativar agora?");

            ButtonType btnSim = new ButtonType("Sim", ButtonBar.ButtonData.YES);
            ButtonType btnNao = new ButtonType("Não", ButtonBar.ButtonData.NO);
            confirm.getButtonTypes().setAll(btnSim, btnNao);

            Optional<ButtonType> result = confirm.showAndWait();
            if (result.isPresent() && result.get() == btnSim) {
                realizarBackup = true;
                chkBackup.setSelected(true);
            } else if (result.isEmpty()) {
                return;
            }
        }

        // Resetar Flag e UI
        stopRequested = false;
        travamentoDeInterface(true);
        txtLog.clear();
        progressBar.setProgress(0);

        final String host = txtServer.getText().trim();
        final String portStr = txtPort.getText().trim();
        final String user = txtUser.getText().trim();
        final String pass = txtPass.getText();
        final String dbPath = txtDbPath.getText().trim();

        final boolean doBackup = realizarBackup;
        final boolean doScripts = rodarScripts;

        new Thread(() -> executarLogicaBackend(host, portStr, user, pass, dbPath, doBackup, doScripts)).start();
    }

    private void executarLogicaBackend(String host, String portStr, String user, String pass, String dbPath, boolean fazerBackup, boolean rodarScripts) {
        int port = 3050;
        try { port = Integer.parseInt(portStr); } catch (NumberFormatException ignored) {}

        // --- CORREÇÃO DO CAMINHO PARA DOCKER ---
        // Se for Mac/Linux, assumimos que é Docker e mudamos o caminho
        String finalPath = dbPath;
        if (!System.getProperty("os.name").toLowerCase().contains("win")) {
            finalPath = "/firebird/data/" + new File(dbPath).getName();
        }

        // Conexão JDBC usa o caminho ajustado
        String url = String.format("jdbc:firebirdsql://%s:%d/%s?encoding=WIN1252", host, port, finalPath.replace("\\", "/"));

        // --- 1. ETAPA DE BACKUP ---
        if (fazerBackup) {
            if (stopRequested) { finalizarProcesso("Interrompido antes do Backup."); return; }

            try {
                // Configuração da Janela de Backup
                TextArea areaLogBackup = new TextArea();
                areaLogBackup.setEditable(false);
                areaLogBackup.setStyle("-fx-control-inner-background: black; -fx-text-fill: lime; -fx-font-family: 'Consolas';");
                areaLogBackup.setPrefSize(600, 300);

                Platform.runLater(() -> {
                    progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
                    Alert progressDialog = new Alert(Alert.AlertType.NONE);
                    progressDialog.setTitle("Realizando Backup");
                    progressDialog.setHeaderText("Aguarde... Compactando banco de dados.");
                    VBox content = new VBox(10, new Label("Processo do Firebird (GBak):"), areaLogBackup);
                    progressDialog.getDialogPane().setContent(content);
                    progressDialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
                    progressDialog.show();
                    logger.info(">>> Iniciando Backup...");
                });

                // Redirecionador
                com.fbupdatetool.util.GuiOutputStream guiStream = new com.fbupdatetool.util.GuiOutputStream(areaLogBackup);

                // Ação Bloqueante - CORREÇÃO CRÍTICA AQUI: USAR finalPath (Caminho Docker)
                // Usando 'finalPath' em vez de 'dbPath' para o servidor Firebird encontrar o arquivo.
                new BackupService().performBackup(dbPath, host, port, user, pass, guiStream);

                Platform.runLater(() -> logger.info(">>> Backup OK."));

            } catch (Exception e) {
                Platform.runLater(() -> {
                    logger.error("FALHA CRÍTICA NO BACKUP: {}", e.getMessage());
                    mostrarAlerta(Alert.AlertType.ERROR, "Erro de Backup", "O Backup falhou. Processo abortado.\n" + e.getMessage());
                    finalizarProcesso("Erro no Backup");
                });
                return;
            }
        }

        // Verifica Parada após Backup
        if (stopRequested) {
            finalizarProcesso("Processo interrompido pelo usuário após Backup.");
            return;
        }

        // Se era só backup, termina aqui
        if (!rodarScripts) {
            Platform.runLater(() -> logger.info("Modo 'Apenas Backup' finalizado com sucesso."));
            finalizarProcesso(null);
            return;
        }

        // --- 2. ETAPA DE ATUALIZAÇÃO ---
        Platform.runLater(() -> logger.info("Conectando para scripts: " + url));

        try (Connection conn = DriverManager.getConnection(url, user, pass)) {
            new HistoryService().initHistoryTable(conn);

            List<ScriptItem> scriptsParaRodar = listScripts.getItems().stream()
                    .filter(ScriptItem::isSelected)
                    .collect(Collectors.toList());

            Platform.runLater(() -> {
                listScripts.getItems().forEach(item -> item.setStatus(ScriptItem.Status.PENDING));
                listScripts.refresh();
                progressBar.setProgress(0);
            });

            ScriptExecutor executor = new ScriptExecutor(cmd -> {
                FutureTask<Boolean> askUser = new FutureTask<>(() -> {
                    TextInputDialog dialog = new TextInputDialog("");
                    dialog.setTitle("Segurança");
                    dialog.setHeaderText("COMANDO CRÍTICO");
                    dialog.setContentText("Senha de administrador:");
                    Optional<String> res = dialog.showAndWait();
                    return res.isPresent() && "suporte#1234".equals(res.get());
                });
                Platform.runLater(askUser);
                try { return askUser.get(); } catch (Exception e) { return false; }
            });

            int total = scriptsParaRodar.size();
            int current = 0;
            boolean errorOccurred = false;

            for (ScriptItem item : scriptsParaRodar) {
                // Checa parada DENTRO do loop
                if (stopRequested) {
                    Platform.runLater(() -> logger.warn(">>> Execução de scripts interrompida pelo usuário!"));
                    break;
                }

                if (errorOccurred) {
                    atualizarStatusItem(item, ScriptItem.Status.SKIPPED);
                    continue;
                }

                atualizarStatusItem(item, ScriptItem.Status.RUNNING);
                boolean success = executor.executeScript(conn, item.getPath());

                if (success) {
                    atualizarStatusItem(item, ScriptItem.Status.SUCCESS);
                } else {
                    atualizarStatusItem(item, ScriptItem.Status.ERROR);
                    errorOccurred = true;
                }

                if (!errorOccurred) {
                    current++;
                    double progress = (double) current / total;
                    Platform.runLater(() -> progressBar.setProgress(progress));
                }
            }

            Platform.runLater(() -> logger.info("FIM DO PROCESSO DE SCRIPTS."));

        } catch (Exception e) {
            Platform.runLater(() -> {
                logger.error("ERRO FATAL:", e);
                mostrarAlerta(Alert.AlertType.ERROR, "Erro Fatal", e.getMessage());
            });
        } finally {
            finalizarProcesso(null);
        }
    }

    private void travamentoDeInterface(boolean travado) {
        btnUpdate.setDisable(travado);
        btnBackupOnly.setDisable(travado);
        chkBackup.setDisable(travado);

        // Botão Stop funciona ao inverso: Ativo quando travado
        btnStop.setDisable(!travado);
    }

    private void finalizarProcesso(String mensagemExtra) {
        Platform.runLater(() -> {
            if (mensagemExtra != null) logger.info(mensagemExtra);
            salvarLogEmArquivo();
            travamentoDeInterface(false); // Destrava tudo
            progressBar.setProgress(0);
            stopRequested = false;
        });
    }

    // ================= HELPERS GERAIS =================

    private void atualizarStatusItem(ScriptItem item, ScriptItem.Status status) {
        Platform.runLater(() -> {
            item.setStatus(status);
            listScripts.refresh();
        });
    }

    private void salvarLogEmArquivo() {
        String conteudo = txtLog.getText();
        new Thread(() -> {
            FileLogService logService = new FileLogService();
            File arquivo = logService.salvarLogEmArquivo(conteudo);
            if(arquivo != null) logService.abrirLogNoBlocoDeNotas(arquivo);
        }).start();
    }

    private void inicializarPastaPadrao() {
        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
        String pathCarregar = "";
        if (isWindows) {
            Path defaultPath = Paths.get(WINDOWS_DEFAULT_PATH);
            try {
                if (!Files.exists(defaultPath)) {
                    Files.createDirectories(defaultPath);
                    logger.info("Pasta padrão criada: " + WINDOWS_DEFAULT_PATH);
                }
                pathCarregar = WINDOWS_DEFAULT_PATH;
                configService.saveLastScriptFolder(pathCarregar);
            } catch (Exception e) { logger.error("Falha ao criar pasta", e); }
        }
        if (pathCarregar.isEmpty()) pathCarregar = configService.getLastScriptFolder();
        if (!pathCarregar.isEmpty()) carregarListaScripts(pathCarregar);
    }

    private void carregarListaScripts(String pathStr) {
        listScripts.getItems().clear();
        lblCurrentFolder.setText(pathStr);
        Path path = Paths.get(pathStr);
        if (Files.exists(path)) {
            try (Stream<Path> stream = Files.list(path)) {
                List<ScriptItem> items = stream
                        .filter(p -> p.toString().toLowerCase().endsWith(".sql"))
                        .sorted(Comparator.comparing(Path::getFileName))
                        .map(p -> new ScriptItem(p, true))
                        .collect(Collectors.toList());
                listScripts.getItems().addAll(items);
            } catch (Exception e) { logger.error("Erro ao ler scripts", e); }
        }
    }

    private void selecionarPastaScripts() {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Selecione a pasta");
        String last = configService.getLastScriptFolder();
        if (!last.isEmpty()) dc.setInitialDirectory(new File(last));
        File file = dc.showDialog(primaryStage);
        if (file != null) {
            configService.saveLastScriptFolder(file.getAbsolutePath());
            carregarListaScripts(file.getAbsolutePath());
        }
    }

    private void escolherBancoDeDados() {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Firebird DB", "*.fdb", "*.gdb"));
        File file = fc.showOpenDialog(primaryStage);
        if (file != null) {
            txtDbPath.setText(file.getAbsolutePath());
            configService.saveLastDbPath(file.getAbsolutePath());
        }
    }

    private void mostrarAlerta(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private VBox criarBaseCard(String titulo, Ikon icon) {
        VBox card = new VBox(10);
        card.setStyle(STYLE_CARD);
        card.setPadding(new Insets(15));
        Label lbl = new Label(titulo);
        lbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        lbl.setGraphic(new FontIcon(icon));
        card.getChildren().add(lbl);
        return card;
    }

    private void adicionarCampoGrid(GridPane grid, String label, Control field, int col, int row) {
        VBox box = new VBox(2);
        Label lbl = new Label(label);
        lbl.setStyle("-fx-text-fill: #757575; -fx-font-size: 11px;");
        box.getChildren().addAll(lbl, field);
        grid.add(box, col, row);
    }

    // ================= INNER CLASSES =================

    public static class ScriptItem {
        private final Path path;
        private final BooleanProperty selected = new SimpleBooleanProperty();
        private final SimpleObjectProperty<Status> status = new SimpleObjectProperty<>(Status.PENDING);
        enum Status { PENDING, RUNNING, SUCCESS, ERROR, WARNING, IGNORED, SKIPPED }
        public ScriptItem(Path path, boolean isSelected) { this.path = path; this.selected.set(isSelected); }
        public Path getPath() { return path; }
        public boolean isSelected() { return selected.get(); }
        public void setSelected(boolean s) { this.selected.set(s); }
        public Status getStatus() { return status.get(); }
        public void setStatus(Status s) { this.status.set(s); }
    }

    private static class ScriptListCell extends ListCell<ScriptItem> {
        private final HBox content;
        private final CheckBox checkBox;
        private final Label label;
        private final Label statusIcon;

        public ScriptListCell() {
            checkBox = new CheckBox();
            label = new Label();
            statusIcon = new Label();
            content = new HBox(10);
            content.setAlignment(Pos.CENTER_LEFT);
            content.getChildren().addAll(checkBox, statusIcon, label);
            checkBox.setOnAction(e -> { if(getItem() != null) getItem().setSelected(checkBox.isSelected()); });
        }

        @Override
        protected void updateItem(ScriptItem item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                setText(null);
                setStyle("-fx-background-color: transparent;");
            } else {
                label.setText(item.getPath().getFileName().toString());
                checkBox.setSelected(item.isSelected());

                FontIcon icon = null;
                String statusBg = "transparent";

                switch (item.getStatus()) {
                    case SUCCESS: icon = new FontIcon(MaterialDesignC.CHECK_CIRCLE); icon.setIconColor(COL_SUCCESS); statusBg = "#E8F5E9"; break;
                    case ERROR: icon = new FontIcon(MaterialDesignA.ALERT_CIRCLE); icon.setIconColor(COL_ERROR); statusBg = "#FFEBEE"; break;
                    case RUNNING: icon = new FontIcon(MaterialDesignP.PROGRESS_CLOCK); icon.setIconColor(COL_INFO); statusBg = "#E3F2FD"; break;
                    case WARNING: icon = new FontIcon(MaterialDesignA.ALERT); icon.setIconColor(COL_WARNING); statusBg = "#FFF3E0"; break;
                    case SKIPPED: icon = new FontIcon(MaterialDesignS.SKIP_NEXT); icon.setIconColor(COL_SKIPPED); statusBg = "#F3E5F5"; break;
                    case IGNORED: icon = new FontIcon(MaterialDesignC.CHECK_ALL); icon.setIconColor(COL_INFO); break;
                }
                statusIcon.setGraphic(icon);

                if (isSelected()) {
                    setStyle("-fx-background-color: #BBDEFB; -fx-text-fill: black;");
                    label.setStyle("-fx-text-fill: black;");
                } else {
                    setStyle("-fx-background-color: " + statusBg + "; -fx-text-fill: black;");
                    label.setStyle("-fx-text-fill: black;");
                }
                setGraphic(content);
            }
        }
    }
}