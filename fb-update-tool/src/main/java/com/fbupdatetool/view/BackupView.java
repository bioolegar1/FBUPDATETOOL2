package com.fbupdatetool.view;

import com.fbupdatetool.service.FirebirdService;
import com.fbupdatetool.service.ZipService;
import com.fbupdatetool.util.FirebirdPathUtils;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class BackupView {

    private VBox rootLayout;

    // --- Campos de Configuração Global ---
    private TextField txtHost, txtPort, txtUser;
    private PasswordField txtPass;
    private TextField txtGbakPath;

    // --- Campos da Aba BACKUP ---
    private TextField txtDbPathBackup;
    private CheckBox chkCompactar;
    private Button btnIniciarBackup;

    // --- Campos da Aba RESTAURAR ---
    private TextField txtArquivoOrigemRestauro;
    private TextField txtBancoDestinoRestauro;
    private CheckBox chkSubstituir;
    private Button btnIniciarRestauro;

    // --- Log e Status ---
    private TextArea txtLog;
    private Label lblStatus;
    private ProgressBar progressBar;

    // --- OTIMIZAÇÃO DE LOG (Evita travamento da UI) ---
    private final StringBuilder logBuffer = new StringBuilder();
    private long lastLogUpdate = 0;

    public BackupView() {
        inicializarComponentes();
        detectarGbakAutomaticamente();
    }

    public Node getView() {
        return rootLayout;
    }

    private void inicializarComponentes() {
        rootLayout = new VBox(10);
        rootLayout.setPadding(new Insets(15));
        rootLayout.setStyle("-fx-background-color: #f4f4f4;");

        Label lblTitulo = new Label("Gerenciador de Backup Firebird");
        lblTitulo.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
        lblTitulo.setStyle("-fx-text-fill: #2E7D32;");

        TitledPane paneConfig = new TitledPane("Configurações Globais (Conexão e Ferramentas)", criarPainelConfiguracoes());
        paneConfig.setCollapsible(false);

        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        Tab tabBackup = new Tab("Fazer Backup", criarConteudoAbaBackup());
        Tab tabRestauro = new Tab("Restaurar Backup", criarConteudoAbaRestauro());
        tabPane.getTabs().addAll(tabBackup, tabRestauro);

        txtLog = new TextArea();
        txtLog.setEditable(false);
        txtLog.setPrefHeight(200);
        txtLog.setStyle("-fx-control-inner-background: black; -fx-text-fill: lightgreen; -fx-font-family: 'Consolas';");

        VBox boxStatus = new VBox(5);
        lblStatus = new Label("Pronto.");
        lblStatus.setStyle("-fx-font-weight: bold;");
        progressBar = new ProgressBar(0);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        boxStatus.getChildren().addAll(lblStatus, progressBar);

        rootLayout.getChildren().addAll(lblTitulo, paneConfig, tabPane, new Label("Log de Operações:"), txtLog, boxStatus);
    }

    // ================== CRIAÇÃO DOS PAINÉIS ==================

    private GridPane criarPainelConfiguracoes() {
        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(10));

        txtHost = new TextField("localhost");
        txtPort = new TextField("3050");
        txtUser = new TextField("SYSDBA");
        txtPass = new PasswordField();
        txtPass.setText("masterkey");
        txtGbakPath = new TextField();
        txtGbakPath.setPromptText("Caminho do gbak.exe...");
        Button btnLocGbak = new Button("...");
        btnLocGbak.setOnAction(e -> selecionarArquivo(txtGbakPath, "Executável GBAK", "gbak.exe"));

        grid.add(new Label("Servidor:"), 0, 0); grid.add(txtHost, 1, 0);
        grid.add(new Label("Porta:"), 2, 0);    grid.add(txtPort, 3, 0);
        grid.add(new Label("Usuário:"), 0, 1);  grid.add(txtUser, 1, 1);
        grid.add(new Label("Senha:"), 2, 1);    grid.add(txtPass, 3, 1);
        grid.add(new Label("Caminho GBAK:"), 0, 2);
        grid.add(txtGbakPath, 1, 2, 2, 1);
        grid.add(btnLocGbak, 3, 2);

        txtHost.setPrefWidth(150); txtPort.setPrefWidth(60);
        txtUser.setPrefWidth(150); txtPass.setPrefWidth(100);
        txtGbakPath.setPrefWidth(300);

        return grid;
    }

    private VBox criarConteudoAbaBackup() {
        VBox box = new VBox(15);
        box.setPadding(new Insets(20));

        Label lblInst = new Label("Selecione o banco de dados (.GDB) para gerar um backup.");

        HBox boxFile = new HBox(10);
        txtDbPathBackup = new TextField();
        txtDbPathBackup.setPromptText("Selecione o banco de origem...");
        HBox.setHgrow(txtDbPathBackup, Priority.ALWAYS);
        Button btnSel = new Button("Selecionar Banco");
        btnSel.setOnAction(e -> selecionarArquivo(txtDbPathBackup, "Banco de Dados", "*.GDB", "*.FDB"));
        boxFile.getChildren().addAll(txtDbPathBackup, btnSel);

        chkCompactar = new CheckBox("Compactar arquivo final (.zip)?");
        chkCompactar.setSelected(true);

        btnIniciarBackup = new Button("INICIAR BACKUP");
        btnIniciarBackup.setStyle("-fx-background-color: #2E7D32; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20;");
        btnIniciarBackup.setMaxWidth(Double.MAX_VALUE);
        btnIniciarBackup.setOnAction(e -> acaoFazerBackup());

        box.getChildren().addAll(lblInst, boxFile, chkCompactar, btnIniciarBackup);
        return box;
    }

    private VBox criarConteudoAbaRestauro() {
        VBox box = new VBox(15);
        box.setPadding(new Insets(20));

        Label lblInst = new Label("Selecione um arquivo de backup (.ZIP ou .GBK) e o local de destino.");

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);

        txtArquivoOrigemRestauro = new TextField();
        txtArquivoOrigemRestauro.setPromptText("Arquivo de Backup (.zip ou .gbk)");
        Button btnSelOrigem = new Button("Selecionar Backup");
        btnSelOrigem.setOnAction(e -> selecionarArquivo(txtArquivoOrigemRestauro, "Arquivos de Backup", "*.ZIP", "*.GBK"));

        txtBancoDestinoRestauro = new TextField();
        txtBancoDestinoRestauro.setPromptText("Onde salvar o banco restaurado (.gdb)");
        Button btnSelDestino = new Button("Salvar Como...");
        btnSelDestino.setOnAction(e -> selecionarSalvarArquivo(txtBancoDestinoRestauro, "Salvar Banco Restaurado", "*.GDB"));

        grid.add(new Label("Arquivo Backup:"), 0, 0);
        grid.add(txtArquivoOrigemRestauro, 1, 0);
        grid.add(btnSelOrigem, 2, 0);

        grid.add(new Label("Destino (.GDB):"), 0, 1);
        grid.add(txtBancoDestinoRestauro, 1, 1);
        grid.add(btnSelDestino, 2, 1);

        GridPane.setHgrow(txtArquivoOrigemRestauro, Priority.ALWAYS);
        GridPane.setHgrow(txtBancoDestinoRestauro, Priority.ALWAYS);

        chkSubstituir = new CheckBox("Substituir banco de dados se ele já existir (CUIDADO)");
        chkSubstituir.setStyle("-fx-text-fill: #D32F2F; -fx-font-weight: bold;");

        btnIniciarRestauro = new Button("RESTAURAR BANCO DE DADOS");
        btnIniciarRestauro.setStyle("-fx-background-color: #1976D2; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20;");
        btnIniciarRestauro.setMaxWidth(Double.MAX_VALUE);
        btnIniciarRestauro.setOnAction(e -> acaoRestaurar());

        box.getChildren().addAll(lblInst, grid, chkSubstituir, btnIniciarRestauro);
        return box;
    }

    // ================== AÇÕES (LÓGICA) ==================

    private void acaoFazerBackup() {
        if (!validarCamposGlobais()) return;
        if (txtDbPathBackup.getText().isEmpty()) { alertaErro("Selecione o banco de origem."); return; }

        iniciarTask("Fazendo Backup...", () -> {
            FirebirdService fbService = new FirebirdService();
            ZipService zipService = new ZipService();

            File arquivoBanco = new File(txtDbPathBackup.getText());
            File pastaBackups = new File("backups");
            if (!pastaBackups.exists()) pastaBackups.mkdirs();

            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmm").format(new Date());
            String nomeBase = arquivoBanco.getName().replaceFirst("[.][^.]+$", "");
            File arquivoGbk = new File(pastaBackups, nomeBase + "_" + timeStamp + ".gbk");

            log("=== INICIANDO BACKUP ===");
            fbService.realizarBackup(txtGbakPath.getText(), txtUser.getText(), txtPass.getText(),
                    arquivoBanco, arquivoGbk, this::log);

            log("Backup .GBK gerado com sucesso.");

            if (chkCompactar.isSelected()) {
                log("Compactando para ZIP...");
                File arquivoZip = new File(pastaBackups, nomeBase + "_" + timeStamp + ".zip");
                zipService.compactarParaZip(arquivoGbk, arquivoZip, this::log);
                if (arquivoGbk.delete()) log("Arquivo .GBK temporário removido.");
            }
        });
    }

    private void acaoRestaurar() {
        if (!validarCamposGlobais()) return;
        if (txtArquivoOrigemRestauro.getText().isEmpty()) { alertaErro("Selecione o arquivo de origem (.zip ou .gbk)."); return; }
        if (txtBancoDestinoRestauro.getText().isEmpty()) { alertaErro("Selecione onde salvar o banco (.gdb)."); return; }

        boolean substituir = chkSubstituir.isSelected();
        File arquivoOrigem = new File(txtArquivoOrigemRestauro.getText());
        File bancoDestino = new File(txtBancoDestinoRestauro.getText());

        iniciarTask("Restaurando...", () -> {
            FirebirdService fbService = new FirebirdService();
            ZipService zipService = new ZipService();
            File arquivoGbkParaRestauro = arquivoOrigem;

            log("=== INICIANDO RESTAURAÇÃO ===");

            if (arquivoOrigem.getName().toLowerCase().endsWith(".zip")) {
                log("Detectado arquivo ZIP. Buscando .GBK interno...");
                File pastaTemp = bancoDestino.getParentFile();
                arquivoGbkParaRestauro = zipService.descompactarZip(arquivoOrigem, pastaTemp, this::log);
                log("Arquivo extraído: " + arquivoGbkParaRestauro.getName());
            }

            log("Restaurando banco de dados a partir do .GBK...");
            fbService.restaurarBackup(txtGbakPath.getText(), txtUser.getText(), txtPass.getText(),
                    arquivoGbkParaRestauro, bancoDestino, substituir, this::log);

            log("Restauração concluída com sucesso!");
        });
    }

    // ================== UTILITÁRIOS E BUFFERING DE LOG ==================

    private void iniciarTask(String statusInicial, RunnableWithException acao) {
        btnIniciarBackup.setDisable(true);
        btnIniciarRestauro.setDisable(true);
        txtLog.clear();
        progressBar.setProgress(-1);
        lblStatus.setText(statusInicial);

        // Limpa buffer antes de começar
        synchronized (logBuffer) { logBuffer.setLength(0); }

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                acao.run();
                return null;
            }
            @Override
            protected void succeeded() {
                flushLog(); // Garante que o restante do log apareça
                lblStatus.setText("Concluído!");
                lblStatus.setStyle("-fx-text-fill: green;");
                desbloquearBotoes();
                progressBar.setProgress(1);
                alertaSucesso("Operação finalizada com sucesso.");
            }
            @Override
            protected void failed() {
                flushLog(); // Garante que logs de erro apareçam
                lblStatus.setText("Erro.");
                lblStatus.setStyle("-fx-text-fill: red;");
                desbloquearBotoes();
                progressBar.setProgress(0);
                Throwable e = getException();
                log("ERRO FATAL: " + e.getMessage());
                flushLog(); // Força log imediato do erro
                alertaErro(e.getMessage());
            }
        };
        new Thread(task).start();
    }

    /**
     * Adiciona mensagem ao buffer de memória.
     * Só atualiza a tela se passar 300ms, evitando travar a UI Thread.
     */
    private void log(String msg) {
        synchronized (logBuffer) {
            logBuffer.append(msg).append("\n");
        }

        long now = System.currentTimeMillis();
        // Atualiza a cada 300ms
        if (now - lastLogUpdate > 300) {
            flushLog();
            lastLogUpdate = now;
        }
    }

    /**
     * Despeja o conteúdo do buffer na TextArea na Thread da UI.
     */
    private void flushLog() {
        Platform.runLater(() -> {
            synchronized (logBuffer) {
                if (logBuffer.length() > 0) {
                    txtLog.appendText(logBuffer.toString());
                    logBuffer.setLength(0); // Limpa o buffer
                    txtLog.selectPositionCaret(txtLog.getLength()); // Rola para o fim
                    txtLog.deselect();
                }
            }
        });
    }

    private void desbloquearBotoes() {
        btnIniciarBackup.setDisable(false);
        btnIniciarRestauro.setDisable(false);
    }

    private boolean validarCamposGlobais() {
        if (txtGbakPath.getText().isEmpty()) { alertaErro("Configure o caminho do GBAK nas configurações globais."); return false; }
        return true;
    }

    private void selecionarArquivo(TextField campo, String titulo, String... extensies) {
        FileChooser fc = new FileChooser();
        fc.setTitle(titulo);
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Arquivos", extensies));
        File f = fc.showOpenDialog(rootLayout.getScene().getWindow());
        if (f != null) campo.setText(f.getAbsolutePath());
    }

    private void selecionarSalvarArquivo(TextField campo, String titulo, String extensao) {
        FileChooser fc = new FileChooser();
        fc.setTitle(titulo);
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Arquivo Firebird", extensao));
        File f = fc.showSaveDialog(rootLayout.getScene().getWindow());
        if (f != null) campo.setText(f.getAbsolutePath());
    }

    private void detectarGbakAutomaticamente() {
        new Thread(() -> {
            String path = FirebirdPathUtils.descobrirCaminhoGbak();
            if (path != null) Platform.runLater(() -> txtGbakPath.setText(path));
        }).start();
    }

    private void alertaErro(String msg) { Platform.runLater(() -> { Alert a = new Alert(Alert.AlertType.ERROR); a.setContentText(msg); a.show(); }); }
    private void alertaSucesso(String msg) { Platform.runLater(() -> { Alert a = new Alert(Alert.AlertType.INFORMATION); a.setContentText(msg); a.show(); }); }

    @FunctionalInterface
    interface RunnableWithException {
        void run() throws Exception;
    }
}