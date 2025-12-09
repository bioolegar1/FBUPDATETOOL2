package com.fbupdatetool.view;

import com.fbupdatetool.service.ConfigurationService;
import com.fbupdatetool.service.HistoryService;
import com.fbupdatetool.service.ScriptExecutor;
import com.fbupdatetool.service.ScriptFolderManager;
import com.fbupdatetool.util.TextAreaAppender;
import com.formdev.flatlaf.FlatClientProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MainFrame extends JFrame {

    private static final Logger logger = LoggerFactory.getLogger(MainFrame.class);
    private final ConfigurationService configService;

    // Componentes Visuais
    private JTextField txtDbPath;
    private JButton btnUpdate;
    private JButton btnSelectDb; // Botão novo (Pasta)
    private JTextArea txtLog;
    private JProgressBar progressBar;
    private JLabel lblStatus;

    // Cores e Fontes
    private final Color COLOR_BG_PANEL = new Color(60, 63, 65);
    private final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 22);
    private final Font FONT_NORMAL = new Font("Segoe UI", Font.PLAIN, 14);
    private final Font FONT_MONO = new Font("JetBrains Mono", Font.PLAIN, 12);

    public MainFrame() {
        this.configService = new ConfigurationService(); // Carrega configurações
        initWindow();
        initComponents();
        TextAreaAppender.setTextArea(txtLog);

        // Ações dos Botões
        btnUpdate.addActionListener(e -> iniciarProcessoDeAtualizacao());
        btnSelectDb.addActionListener(e -> escolherBancoDeDados());

        // Verifica se já tem banco configurado ao abrir
        verificarConfiguracaoInicial();
    }

    private void initWindow() {
        setTitle("FBUpdateTool 2.0 Pro");
        setSize(1000, 750);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(new Color(45, 45, 45));
    }

    private void initComponents() {
        JPanel mainPanel = new JPanel(new BorderLayout(15, 15));
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        mainPanel.setBackground(new Color(45, 45, 45));

        // --- 1. CABEÇALHO ---
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        JLabel lblTitle = new JLabel("🚀 FBUpdateTool Enterprise");
        lblTitle.setFont(FONT_TITLE);
        lblTitle.setForeground(Color.WHITE);
        JLabel lblSubtitle = new JLabel("Atualizador Automático de Banco de Dados Firebird 2.5");
        lblSubtitle.setFont(new Font("Segoe UI", Font.ITALIC, 14));
        lblSubtitle.setForeground(new Color(170, 170, 170));
        headerPanel.add(lblTitle, BorderLayout.NORTH);
        headerPanel.add(lblSubtitle, BorderLayout.SOUTH);
        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // --- 2. ÁREA CENTRAL ---
        JPanel centerContainer = new JPanel(new BorderLayout(0, 15));
        centerContainer.setOpaque(false);

        // 2.1 Painel de Configuração
        JPanel configPanel = createRoundedPanel(" Configuração do Banco de Dados ");
        configPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        JLabel lblPath = new JLabel("📂 Arquivo do Banco (.GDB / .FDB):");
        lblPath.setFont(FONT_NORMAL);

        // Recupera o último caminho salvo
        txtDbPath = new JTextField(configService.getLastDbPath());
        txtDbPath.setFont(FONT_NORMAL);
        txtDbPath.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Clique na pasta para selecionar...");

        // Botão de Selecionar Arquivo
        btnSelectDb = new JButton("...");
        btnSelectDb.setToolTipText("Selecionar arquivo do banco");
        btnSelectDb.setPreferredSize(new Dimension(40, 30));

        // Layout
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2; gbc.anchor = GridBagConstraints.WEST; gbc.insets = new Insets(0, 0, 5, 0);
        configPanel.add(lblPath, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1; gbc.weightx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.insets = new Insets(0, 0, 0, 5);
        configPanel.add(txtDbPath, gbc);

        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 0; gbc.fill = GridBagConstraints.NONE; gbc.insets = new Insets(0, 0, 0, 0);
        configPanel.add(btnSelectDb, gbc);

        centerContainer.add(configPanel, BorderLayout.NORTH);

        // 2.2 Painel de Logs
        JPanel logPanel = new JPanel(new BorderLayout(0, 5));
        logPanel.setOpaque(false);
        JLabel lblLogTitle = new JLabel("📜 Auditoria de Execução:");
        lblLogTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblLogTitle.setForeground(Color.WHITE);

        txtLog = new JTextArea();
        txtLog.setEditable(false);
        txtLog.setFont(FONT_MONO);
        txtLog.setBackground(new Color(30, 30, 30));
        txtLog.setForeground(new Color(200, 200, 200));
        txtLog.setMargin(new Insets(10, 10, 10, 10));

        JScrollPane scrollPane = new JScrollPane(txtLog);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(60, 60, 60)));
        logPanel.add(lblLogTitle, BorderLayout.NORTH);
        logPanel.add(scrollPane, BorderLayout.CENTER);

        centerContainer.add(logPanel, BorderLayout.CENTER);
        mainPanel.add(centerContainer, BorderLayout.CENTER);

        // --- 3. RODAPÉ ---
        JPanel footerPanel = createRoundedPanel("");
        footerPanel.setLayout(new BorderLayout(10, 0));

        JPanel statusPanel = new JPanel(new BorderLayout(0, 5));
        statusPanel.setOpaque(false);
        lblStatus = new JLabel("Aguardando início...");
        lblStatus.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblStatus.setForeground(Color.GRAY);
        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        progressBar.setPreferredSize(new Dimension(100, 25));

        statusPanel.add(lblStatus, BorderLayout.NORTH);
        statusPanel.add(progressBar, BorderLayout.CENTER);

        btnUpdate = new JButton("▶ Iniciar Atualização");
        btnUpdate.setFont(new Font("Segoe UI", Font.BOLD, 16));
        btnUpdate.setBackground(new Color(40, 167, 69));
        btnUpdate.setForeground(Color.WHITE);
        btnUpdate.setFocusPainted(false);
        btnUpdate.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnUpdate.setPreferredSize(new Dimension(220, 50));

        footerPanel.add(statusPanel, BorderLayout.CENTER);
        footerPanel.add(btnUpdate, BorderLayout.EAST);
        mainPanel.add(footerPanel, BorderLayout.SOUTH);

        add(mainPanel);
    }

    private JPanel createRoundedPanel(String title) {
        JPanel p = new JPanel();
        p.setBackground(COLOR_BG_PANEL);
        p.setBorder(new CompoundBorder(
                new LineBorder(new Color(80, 80, 80), 1, true),
                new EmptyBorder(15, 15, 15, 15)
        ));
        return p;
    }

    // =================================================================================
    // LÓGICA DE SELEÇÃO DE ARQUIVO (ISSUE UI-03)
    // =================================================================================

    private void verificarConfiguracaoInicial() {
        // Se o campo estiver vazio (nenhum banco salvo), força a escolha
        if (txtDbPath.getText().trim().isEmpty()) {
            SwingUtilities.invokeLater(this::escolherBancoDeDados);
        }
    }

    private void escolherBancoDeDados() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Selecione o arquivo do Banco de Dados");

        // Filtro para mostrar apenas arquivos de banco
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Banco Firebird (*.gdb, *.fdb)", "gdb", "fdb");
        chooser.setFileFilter(filter);

        // Tenta abrir na pasta atual ou última usada
        chooser.setCurrentDirectory(new File("."));

        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = chooser.getSelectedFile();
            String path = selectedFile.getAbsolutePath();

            // Atualiza a tela e salva na memória
            txtDbPath.setText(path);
            configService.saveLastDbPath(path);

            logger.info("Banco de dados selecionado: {}", path);
        }
    }

    // =================================================================================
    // LÓGICA DE EXECUÇÃO
    // =================================================================================

    private void iniciarProcessoDeAtualizacao() {
        String dbPath = txtDbPath.getText().trim();
        if (dbPath.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Por favor, selecione um arquivo de banco de dados primeiro!", "Atenção", JOptionPane.WARNING_MESSAGE);
            escolherBancoDeDados(); // Abre o seletor se esqueceu
            return;
        }

        btnUpdate.setEnabled(false);
        btnSelectDb.setEnabled(false); // Bloqueia troca durante execução
        txtLog.setText("");
        progressBar.setValue(0);
        progressBar.setIndeterminate(true);
        lblStatus.setForeground(new Color(80, 200, 255));
        lblStatus.setText("Validando ambiente...");

        new Thread(() -> {
            try {
                executarLogicaBackend();
            } finally {
                SwingUtilities.invokeLater(() -> {
                    btnUpdate.setEnabled(true);
                    btnSelectDb.setEnabled(true);
                    progressBar.setIndeterminate(false);
                    progressBar.setValue(100);
                    lblStatus.setText("Processo finalizado.");
                    lblStatus.setForeground(Color.GRAY);
                });
            }
        }).start();
    }

    private void executarLogicaBackend() {
        // Pega a porta validada pelo Main
        String porta = configService.getLastDbPort();
        String caminhoBanco = txtDbPath.getText().trim();

        // Monta a URL dinamicamente
        String url = "jdbc:firebirdsql://localhost:" + porta + "/" + caminhoBanco.replace("\\", "/") + "?encoding=WIN1252";

        logger.info("========================================");
        logger.info("   INICIANDO CICLO DE ATUALIZAÇÃO       ");
        logger.info("========================================");
        logger.info("URL JDBC: {}", url);

        try (Connection conn = DriverManager.getConnection(url, "SYSDBA", "masterkey")) {
            new HistoryService().initHistoryTable(conn);

            // --- LÓGICA DE PASTA INTELIGENTE (UI-01) ---
            final Path[] pastaScriptsRef = {null};
            try {
                SwingUtilities.invokeAndWait(() -> {
                    ScriptFolderManager folderManager = new ScriptFolderManager(MainFrame.this);
                    pastaScriptsRef[0] = folderManager.resolveScriptPath();
                });
            } catch (Exception e) { return; }

            Path pastaScripts = pastaScriptsRef[0];
            if (pastaScripts == null) {
                logger.warn("Pasta de scripts não selecionada. Abortando.");
                return;
            }
            // -------------------------------------------

            List<Path> scripts;
            try (Stream<Path> s = Files.list(pastaScripts)) {
                scripts = s.filter(p -> p.toString().toLowerCase().endsWith(".sql"))
                        .sorted()
                        .collect(Collectors.toList());
            }

            if (scripts.isEmpty()) {
                logger.warn("⚠️ Nenhum script .sql encontrado em: {}", pastaScripts);
                JOptionPane.showMessageDialog(this, "Nenhum arquivo .sql encontrado na pasta selecionada.");
                return;
            }

            logger.info("Encontrados {} scripts. Executando...", scripts.size());

            SwingUtilities.invokeLater(() -> {
                progressBar.setIndeterminate(false);
                progressBar.setMaximum(scripts.size());
            });

            // Callback de Segurança (Senha)
            ScriptExecutor executor = new ScriptExecutor(comandoProibido -> {
                final boolean[] permitido = {false};
                try {
                    SwingUtilities.invokeAndWait(() -> {
                        Toolkit.getDefaultToolkit().beep();
                        String senha = JOptionPane.showInputDialog(
                                MainFrame.this,
                                "<html><h3 style='color:red'>⚠️ SEGURANÇA</h3>" +
                                        "Comando crítico detectado: <b>" + comandoProibido + "</b><br>" +
                                        "Senha de Admin:</html>",
                                "Autorização", JOptionPane.WARNING_MESSAGE
                        );
                        if ("suporte#1234".equals(senha)) permitido[0] = true;
                    });
                } catch (Exception e) { logger.error("Erro UI", e); }
                return permitido[0];
            });

            int count = 0;
            for (Path script : scripts) {
                String scriptName = script.getFileName().toString();
                SwingUtilities.invokeLater(() -> lblStatus.setText("Processando: " + scriptName));

                boolean ok = executor.executeScript(conn, script);
                if (!ok) {
                    logger.error("⛔ ERRO NO SCRIPT: {}", scriptName);
                    JOptionPane.showMessageDialog(this, "Erro ao rodar: " + scriptName, "Falha", JOptionPane.ERROR_MESSAGE);
                    break;
                }

                count++;
                final int prog = count;
                SwingUtilities.invokeLater(() -> progressBar.setValue(prog));
            }

            logger.info("\n--- FIM DO PROCESSO ---");

        } catch (Exception e) {
            logger.error("ERRO FATAL: ", e);
            JOptionPane.showMessageDialog(this, "Erro de Conexão:\n" + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }
}