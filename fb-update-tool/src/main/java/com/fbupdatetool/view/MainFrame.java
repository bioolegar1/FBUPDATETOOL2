package com.fbupdatetool.view;

import com.fbupdatetool.service.ConfigurationService;
import com.fbupdatetool.service.HistoryService;
import com.fbupdatetool.service.ScriptExecutor;
import com.fbupdatetool.util.TextAreaAppender;
import com.formdev.flatlaf.FlatClientProperties;
import org.kordamp.ikonli.materialdesign2.*;
import org.kordamp.ikonli.swing.FontIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MainFrame extends JFrame {

    private static final Logger logger = LoggerFactory.getLogger(MainFrame.class);
    private final ConfigurationService configService;

    // Componentes
    private JTextField txtServer, txtPort, txtUser, txtPass;
    private JTextField txtDbPath;
    private JButton btnSelectDb, btnSelectFolder, btnUpdate;
    private JTextArea txtLog;
    private JList<ScriptItem> listScripts;
    private DefaultListModel<ScriptItem> listModel;
    private JProgressBar progressBar;

    // Controles de Lista
    private JLabel lblCurrentFolder;
    private JCheckBox chkSelectAll;

    // Constantes
    private static final String WINDOWS_DEFAULT_PATH = "C:\\SoluçõesPillar\\scriptAtt";

    // Layout e Cores
    private static final int PADDING_LARGE = 20;
    private static final int PADDING_MEDIUM = 15;
    private static final int PADDING_SMALL = 10;

    private final Color COL_PRIMARY = new Color(46, 125, 50);
    private final Color COL_BACKGROUND = new Color(250, 250, 250);
    private final Color COL_CARD = Color.WHITE;
    private final Color COL_TEXT = new Color(33, 33, 33);
    private final Color COL_TEXT_SEC = new Color(117, 117, 117);
    private final Color COL_BORDER = new Color(224, 224, 224);
    private final Color COL_SUCCESS = new Color(76, 175, 80);

    // 1. SUCESSO (Verde - Script Rodou)
    private static final Color BG_SUCCESS = new Color(232, 245, 233);
    private static final Color FG_SUCCESS = new Color(46, 125, 50);

    // 2. ERRO (Vermelho - Falha Crítica)
    private static final Color BG_ERROR = new Color(255, 235, 238);
    private static final Color FG_ERROR = new Color(198, 40, 40);

    // 3. IGNORADO/INFO (Azul - Já Existe / Duplicado)
    private static final Color BG_INFO = new Color(227, 242, 253);
    private static final Color FG_INFO = new Color(21, 101, 192);

    // 4. ALERTA (Laranja - Metadados / Vazio)
    private static final Color BG_WARNING = new Color(255, 243, 224);
    private static final Color FG_WARNING = new Color(230, 81, 0);

    // 5. PULADO (Roxo - Skipped)
    private static final Color BG_SKIPPED = new Color(243, 229, 245);
    private static final Color FG_SKIPPED = new Color(123, 31, 162);

    private final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 28);
    private final Font FONT_SECTION = new Font("Segoe UI", Font.BOLD, 14);
    private final Font FONT_LABEL = new Font("Segoe UI", Font.PLAIN, 11);
    private final Font FONT_FIELD = new Font("Segoe UI", Font.PLAIN, 13);
    private final Font FONT_BUTTON = new Font("Segoe UI", Font.BOLD, 14);




    public MainFrame() {
        this.configService = new ConfigurationService();
        initWindow();
        initComponents();

        TextAreaAppender.setTextArea(txtLog);

        btnSelectDb.addActionListener(e -> escolherBancoDeDados());
        btnSelectFolder.addActionListener(e -> selecionarPastaScripts());
        btnUpdate.addActionListener(e -> iniciarProcessoDeAtualizacao());

        txtDbPath.setText(configService.getLastDbPath());
        txtPort.setText(configService.getLastDbPort());

        SwingUtilities.invokeLater(this::inicializarPastaPadrao);
    }

    private void inicializarPastaPadrao() {
        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
        String pathCarregar = "";

        if (isWindows) {
            Path defaultPath = Paths.get(WINDOWS_DEFAULT_PATH);
            try {
                if (!Files.exists(defaultPath)) {
                    Files.createDirectories(defaultPath);
                    logger.info("Pasta padrão criada: {}", WINDOWS_DEFAULT_PATH);
                }
                pathCarregar = WINDOWS_DEFAULT_PATH;
                configService.saveLastScriptFolder(pathCarregar);
            } catch (Exception e) {
                logger.error("Falha ao criar pasta padrão", e);
            }
        }

        if (pathCarregar.isEmpty()) {
            pathCarregar = configService.getLastScriptFolder();
        }

        if (!pathCarregar.isEmpty()) {
            carregarListaScripts(pathCarregar);
        } else {
            lblCurrentFolder.setText("Nenhuma pasta selecionada");
        }
    }

    private void initWindow() {
        setTitle("Firebird UpdateTool");
        setSize(1200, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(1000, 650));
        getContentPane().setBackground(COL_BACKGROUND);
        setLayout(new BorderLayout());
    }

    private void initComponents() {
        add(criarCabecalho(), BorderLayout.NORTH);

        JPanel mainContainer = new JPanel(new BorderLayout(PADDING_LARGE, PADDING_LARGE));
        mainContainer.setBackground(COL_BACKGROUND);
        mainContainer.setBorder(new EmptyBorder(PADDING_LARGE, PADDING_LARGE, PADDING_LARGE, PADDING_LARGE));

        mainContainer.add(criarPainelListaScripts(), BorderLayout.WEST);
        mainContainer.add(criarPainelDireito(), BorderLayout.CENTER);

        add(mainContainer, BorderLayout.CENTER);
    }

    private JPanel criarCabecalho() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(COL_PRIMARY);
        headerPanel.setBorder(new EmptyBorder(PADDING_LARGE, PADDING_LARGE * 2, PADDING_LARGE, PADDING_LARGE * 2));

        JPanel titleBox = new JPanel();
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));
        titleBox.setOpaque(false);

        JLabel lblTitle = new JLabel("Firebird UpdateTool");
        lblTitle.setFont(FONT_TITLE);
        lblTitle.setForeground(Color.WHITE);
        // Ícone de foguete no título
        lblTitle.setIcon(FontIcon.of(MaterialDesignR.ROCKET, 32, Color.WHITE));
        lblTitle.setIconTextGap(15);

        JLabel lblSubtitle = new JLabel("Sistema de Atualização de Banco de Dados");
        lblSubtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSubtitle.setForeground(new Color(255, 255, 255, 180));

        titleBox.add(lblTitle);
        titleBox.add(Box.createVerticalStrut(3));
        titleBox.add(lblSubtitle);
        headerPanel.add(titleBox, BorderLayout.WEST);
        return headerPanel;
    }

    private JPanel criarPainelListaScripts() {
        // --- 1. CARD EXTERNO ---
        JPanel cardPanel = new JPanel(new BorderLayout());
        cardPanel.setBackground(COL_CARD);
        cardPanel.setPreferredSize(new Dimension(550, 0));
        cardPanel.putClientProperty(FlatClientProperties.STYLE, "arc: 12; border: 1,1,1,1,#E0E0E0,1,12");

        // --- 2. PAINEL DE CONTEÚDO (PADDING) ---
        JPanel contentPanel = new JPanel(new BorderLayout(0, 10));
        contentPanel.setOpaque(false);
        contentPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        // HEADER
        JPanel headerSection = new JPanel(new BorderLayout(0, 5));
        headerSection.setOpaque(false);

        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setOpaque(false);

        JPanel infoBox = new JPanel();
        infoBox.setLayout(new BoxLayout(infoBox, BoxLayout.Y_AXIS));
        infoBox.setOpaque(false);

        JLabel lblTitle = new JLabel("Scripts Disponíveis");
        lblTitle.setFont(FONT_SECTION);
        lblTitle.setForeground(COL_TEXT);

        lblCurrentFolder = new JLabel("...");
        lblCurrentFolder.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        lblCurrentFolder.setForeground(COL_TEXT_SEC);

        infoBox.add(lblTitle);
        infoBox.add(Box.createVerticalStrut(2));
        infoBox.add(lblCurrentFolder);

        // Botão com Ícone de Pasta
        btnSelectFolder = criarBotaoSecundario("Alterar Pasta", MaterialDesignF.FOLDER_OPEN);

        topRow.add(infoBox, BorderLayout.CENTER);
        topRow.add(btnSelectFolder, BorderLayout.EAST);

        JPanel bottomRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        bottomRow.setOpaque(false);

        chkSelectAll = new JCheckBox("Marcar/Desmarcar Todos");
        chkSelectAll.setOpaque(false);
        chkSelectAll.setFont(new Font("Segoe UI", Font.BOLD, 12));
        chkSelectAll.setFocusPainted(false);
        chkSelectAll.setSelected(true);

        chkSelectAll.addActionListener(e -> {
            boolean selecionar = chkSelectAll.isSelected();
            for (int i = 0; i < listModel.size(); i++) {
                listModel.get(i).setSelected(selecionar);
            }
            listScripts.repaint();
        });

        bottomRow.add(chkSelectAll);

        headerSection.add(topRow, BorderLayout.NORTH);
        headerSection.add(bottomRow, BorderLayout.SOUTH);

        // Lista
        listModel = new DefaultListModel<>();
        listScripts = new JList<>(listModel);
        listScripts.setCellRenderer(new ScriptListRenderer()); // NOVO RENDERER
        listScripts.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listScripts.setBorder(new EmptyBorder(5, 5, 5, 5));

        // Evento de clique para marcar/desmarcar
        listScripts.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int index = listScripts.locationToIndex(e.getPoint());
                if (index != -1) {
                    ScriptItem item = listModel.getElementAt(index);
                    item.setSelected(!item.isSelected());
                    listScripts.repaint(listScripts.getCellBounds(index, index));
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(listScripts);
        scrollPane.setBorder(BorderFactory.createLineBorder(COL_BORDER));

        contentPanel.add(headerSection, BorderLayout.NORTH);
        contentPanel.add(scrollPane, BorderLayout.CENTER);
        cardPanel.add(contentPanel, BorderLayout.CENTER);

        return cardPanel;
    }

    private JPanel criarPainelDireito() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.add(criarCardConfiguracao());
        panel.add(Box.createVerticalStrut(15));
        panel.add(criarCardSelecaoBanco());
        panel.add(Box.createVerticalStrut(15));
        panel.add(criarCardLog());
        return panel;
    }

    private JPanel criarCardConfiguracao() {
        JPanel card = criarBaseCard(140);
        JPanel content = new JPanel(new BorderLayout(0, 10));
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel lblTitle = new JLabel("Configuração de Conexão");
        lblTitle.setFont(FONT_SECTION);
        lblTitle.setForeground(COL_TEXT);
        // Ícone de Settings
        lblTitle.setIcon(FontIcon.of(MaterialDesignC.COG, 18, COL_TEXT));
        lblTitle.setIconTextGap(8);

        JPanel gridPanel = new JPanel(new GridLayout(2, 4, PADDING_MEDIUM, PADDING_SMALL));
        gridPanel.setOpaque(false);

        txtServer = criarCampoComLabel(gridPanel, "Servidor", "localhost");
        txtPort = criarCampoComLabel(gridPanel, "Porta", "3050");
        txtUser = criarCampoComLabel(gridPanel, "Usuário", "SYSDBA");
        txtPass = criarCampoSenhaComLabel(gridPanel, "Senha", "masterkey");

        content.add(lblTitle, BorderLayout.NORTH);
        content.add(gridPanel, BorderLayout.CENTER);
        card.add(content, BorderLayout.CENTER);
        return card;
    }

    private JPanel criarCardSelecaoBanco() {
        JPanel card = criarBaseCard(90);
        JPanel content = new JPanel(new BorderLayout(0, 10));
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel lblTitle = new JLabel("Banco de Dados");
        lblTitle.setFont(FONT_SECTION);
        lblTitle.setForeground(COL_TEXT);
        lblTitle.setIcon(FontIcon.of(MaterialDesignD.DATABASE, 18, COL_TEXT));
        lblTitle.setIconTextGap(8);

        JPanel selectionPanel = new JPanel(new BorderLayout(PADDING_SMALL, 0));
        selectionPanel.setOpaque(false);

        txtDbPath = new JTextField();
        txtDbPath.setFont(FONT_FIELD);
        txtDbPath.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
        aplicarPadding(txtDbPath);

        // Ícone de arquivo no botão
        btnSelectDb = criarBotaoSecundario("Selecionar", MaterialDesignF.FILE);

        selectionPanel.add(txtDbPath, BorderLayout.CENTER);
        selectionPanel.add(btnSelectDb, BorderLayout.EAST);

        content.add(lblTitle, BorderLayout.NORTH);
        content.add(selectionPanel, BorderLayout.CENTER);
        card.add(content, BorderLayout.CENTER);
        return card;
    }

    private JPanel criarCardLog() {
        JPanel card = criarBaseCard(0);
        JPanel content = new JPanel(new BorderLayout(0, 10));
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(0, 0, 00, 00));

        //content.setBorder(new EmptyBorder(20, 20, 20, 20));


/**
        JLabel lblTitle = new JLabel("Log de Execução");
        lblTitle.setFont(FONT_SECTION);
        lblTitle.setForeground(COL_TEXT);
        lblTitle.setIcon(FontIcon.of(MaterialDesignC.CLIPBOARD_TEXT, 18, COL_TEXT));
        lblTitle.setIconTextGap(8);
*/
        txtLog = new JTextArea();
        txtLog.setEditable(false);
        txtLog.setFont(new Font("Consolas", Font.PLAIN, 11));
        txtLog.setBackground(new Color(30, 30, 30));
        txtLog.setForeground(new Color(200, 255, 200));
        txtLog.setMargin(new Insets(10, 10, 10, 10));

        JScrollPane scrollPane = new JScrollPane(txtLog);
        scrollPane.setBorder(BorderFactory.createLineBorder(COL_BORDER));

        JPanel bottomPanel = new JPanel(new BorderLayout(0, 10));
        bottomPanel.setOpaque(false);
        bottomPanel.setBorder(new EmptyBorder(10, 0, 0, 0)); // Espaço do topo (separa do log)

        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        progressBar.putClientProperty(FlatClientProperties.STYLE, "arc: 5");

        progressBar.setPreferredSize(new Dimension(100, 15));

        btnUpdate = criarBotaoPrincipal();

        bottomPanel.add(Box.createVerticalStrut(10));
        bottomPanel.add(progressBar);
        bottomPanel.add(Box.createVerticalStrut(10));
        bottomPanel.add(btnUpdate);

        //content.add(lblTitle, BorderLayout.NORTH);
        content.add(scrollPane, BorderLayout.CENTER);
        content.add(bottomPanel, BorderLayout.SOUTH);
        card.add(content, BorderLayout.CENTER);
        return card;
    }

    // --- MÉTODOS AUXILIARES DE UI ---

    private void aplicarPadding(JTextField field) {
        field.setBorder(new CompoundBorder(
                field.getBorder(),
                new EmptyBorder(5, 10, 5, 10)
        ));
    }

    private JPanel criarBaseCard(int height) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(COL_CARD);
        card.putClientProperty(FlatClientProperties.STYLE, "arc: 12; border: 1,1,1,1,#E0E0E0,1,12");
        if(height > 0) card.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
        return card;
    }

    private JTextField criarCampoComLabel(JPanel p, String l, String v) {
        JPanel w = new JPanel(new BorderLayout(4, 5)); w.setOpaque(false);
        JLabel lbl = new JLabel(l); lbl.setFont(FONT_LABEL); lbl.setForeground(COL_TEXT_SEC);
        JTextField t = new JTextField(v); t.setFont(FONT_FIELD); t.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
        aplicarPadding(t);
        w.add(lbl, BorderLayout.NORTH); w.add(t, BorderLayout.CENTER); p.add(w); return t;
    }

    private JTextField criarCampoSenhaComLabel(JPanel p, String l, String v) {
        JPanel w = new JPanel(new BorderLayout(0, 4)); w.setOpaque(false);
        JLabel lbl = new JLabel(l); lbl.setFont(FONT_LABEL); lbl.setForeground(COL_TEXT_SEC);
        JPasswordField t = new JPasswordField(v); t.setFont(FONT_FIELD); t.putClientProperty(FlatClientProperties.STYLE, "arc: 8; showRevealButton: true");
        aplicarPadding(t);
        w.add(lbl, BorderLayout.NORTH); w.add(t, BorderLayout.CENTER); p.add(w); return t;
    }

    // Botão com Ícone
    private JButton criarBotaoSecundario(String t, org.kordamp.ikonli.Ikon ikon) {
        JButton b = new JButton(t);
        b.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        b.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
        if (ikon != null) {
            b.setIcon(FontIcon.of(ikon, 16, Color.DARK_GRAY));
            b.setIconTextGap(8);
        }
        return b;
    }

    private JButton criarBotaoPrincipal() {
        JButton b = new JButton("Iniciar Atualização");
        b.setFont(FONT_BUTTON);
        b.setBackground(COL_SUCCESS);
        b.setForeground(Color.WHITE);
        b.putClientProperty(FlatClientProperties.STYLE, "arc: 10; borderWidth: 0");
        // Altura fixa, largura estica no layout
        b.setPreferredSize(new Dimension(100, 50));
        b.setIcon(FontIcon.of(MaterialDesignP.PLAY_CIRCLE_OUTLINE, 20, Color.WHITE));
        b.setIconTextGap(10);
        return b;
    }

    // =================================================================================
    // LÓGICA DE NEGÓCIO
    // =================================================================================

    private void selectingPastaScripts() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Selecione a pasta de scripts");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        String atual = configService.getLastScriptFolder();
        if(!atual.isEmpty()) chooser.setCurrentDirectory(new File(atual));

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            String novoCaminho = chooser.getSelectedFile().getAbsolutePath();
            configService.saveLastScriptFolder(novoCaminho);
            carregarListaScripts(novoCaminho);
        }
    }

    private void selecionarPastaScripts() { selectingPastaScripts(); }

    private void carregarListaScripts(String pathStr) {
        listModel.clear();
        if (pathStr == null || pathStr.isEmpty()) {
            lblCurrentFolder.setText("Nenhuma pasta selecionada");
            return;
        }

        lblCurrentFolder.setText(pathStr);
        lblCurrentFolder.setToolTipText(pathStr);
        if(chkSelectAll != null) chkSelectAll.setSelected(true);

        Path path = Path.of(pathStr);
        if (Files.exists(path)) {
            try (Stream<Path> stream = Files.list(path)) {
                List<ScriptItem> items = stream
                        .filter(p -> p.toString().toLowerCase().endsWith(".sql"))
                        .sorted(Comparator.comparing(Path::getFileName))
                        .map(p -> new ScriptItem(p, true))
                        .collect(Collectors.toList());

                items.forEach(listModel::addElement);
                if (items.isEmpty()) JOptionPane.showMessageDialog(this, "Nenhum script .sql encontrado em: " + pathStr);

            } catch (Exception e) {
                logger.error("Erro ao ler scripts", e);
            }
        }
    }

    private void escolherBancoDeDados() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Firebird DB", "gdb", "fdb"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            String path = chooser.getSelectedFile().getAbsolutePath();
            txtDbPath.setText(path);
            configService.saveLastDbPath(path);
        }
    }

    private void iniciarProcessoDeAtualizacao() {
        if (txtDbPath.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Selecione o banco de dados!", "Atenção", JOptionPane.WARNING_MESSAGE);
            return;
        }

        boolean temSelecionado = false;
        for(int i=0; i<listModel.size(); i++) {
            if(listModel.get(i).isSelected()) { temSelecionado = true; break; }
        }
        if(!temSelecionado) {
            JOptionPane.showMessageDialog(this, "Selecione pelo menos um script na lista.", "Atenção", JOptionPane.WARNING_MESSAGE);
            return;
        }

        btnUpdate.setEnabled(false);
        txtLog.setText("");
        progressBar.setValue(0);
        new Thread(this::executarLogicaBackend).start();
    }
    /**
    private void executarLogicaBackend() {
        String host = txtServer.getText().trim();
        String port = txtPort.getText().trim();
        String dbPath = txtDbPath.getText().trim();

        String finalPath = dbPath;
        if (!System.getProperty("os.name").toLowerCase().contains("win")) {
            finalPath = "/firebird/data/" + new File(dbPath).getName();
        }

        String url = String.format("jdbc:firebirdsql://%s:%s/%s?encoding=WIN1252", host, port, finalPath.replace("\\", "/"));
        logger.info("Conectando: {}", url);

        try (Connection conn = DriverManager.getConnection(url, txtUser.getText(), new String(((JPasswordField)txtPass).getPassword()))) {
            new HistoryService().initHistoryTable(conn);

            List<ScriptItem> scriptsParaRodar = new ArrayList<>();
            for(int i=0; i<listModel.size(); i++) {
                ScriptItem item = listModel.get(i);
                if(item.isSelected()) {
                    scriptsParaRodar.add(item);
                }
            }

            SwingUtilities.invokeAndWait(() -> {
                for(int i=0; i<listModel.size(); i++) {
                    listModel.get(i).setStatus(ScriptItem.Status.PENDING);
                }
                listScripts.repaint();
                progressBar.setMaximum(scriptsParaRodar.size());
                progressBar.setValue(0);
            });

            ScriptExecutor executor = new ScriptExecutor(cmd -> {
                final boolean[] ok = {false};
                try {
                    SwingUtilities.invokeAndWait(() -> {
                        String s = JOptionPane.showInputDialog(this, "<html><b style='color:red'>COMANDO CRITICO!</b><br>Senha Admin:</html>", "Segurança", JOptionPane.WARNING_MESSAGE);
                        if ("suporte#1234".equals(s)) ok[0] = true;
                    });
                } catch (Exception e) {}
                return ok[0];
            });

            int idx = 0;
            boolean errorOccurred = false;

            for (ScriptItem item : scriptsParaRodar) {
                if (errorOccurred) {
                    item.setStatus(ScriptItem.Status.SKIPPED);
                    listScripts.repaint();
                    continue;
                }

                item.setStatus(ScriptItem.Status.RUNNING);
                listScripts.repaint();

                boolean success = executor.executeScript(conn, item.getPath());

                if (success) {
                    // *** AQUI VOCÊ DEVE INSERIR A LÓGICA DO RETORNO DO EXECUTOR ***
                    // Como não tenho acesso ao retorno detalhado do ScriptExecutor,
                    // estou simulando onde você pode encaixar os status azul/laranja.
                    // Exemplo:
                    // if (resultado.equals("IGNORED")) item.setStatus(ScriptItem.Status.IGNORED);
                    // else if (resultado.equals("WARNING")) item.setStatus(ScriptItem.Status.WARNING);
                    // else
                    item.setStatus(ScriptItem.Status.SUCCESS);
                } else {
                    item.setStatus(ScriptItem.Status.ERROR);
                    errorOccurred = true;
                }
                listScripts.repaint();

                if (!errorOccurred) {
                    idx++;
                    SwingUtilities.invokeLater(() -> progressBar.setValue(progressBar.getValue() + 1));
                }
            }
            logger.info("FIM DO PROCESSO.");

        } catch (Exception e) {
            logger.error("ERRO:", e);
            JOptionPane.showMessageDialog(this, "Erro: " + e.getMessage());
        } finally {
            SwingUtilities.invokeLater(() -> btnUpdate.setEnabled(true));
        }
    }
    */

    private void executarLogicaBackend() {
        String host = txtServer.getText().trim();
        String port = txtPort.getText().trim();
        String dbPath = txtDbPath.getText().trim();

        String finalPath = dbPath;
        if (!System.getProperty("os.name").toLowerCase().contains("win")) {
            finalPath = "/firebird/data/" + new File(dbPath).getName();
        }

        String url = String.format("jdbc:firebirdsql://%s:%s/%s?encoding=WIN1252", host, port, finalPath.replace("\\", "/"));
        logger.info("Conectando: {}", url);

        try (Connection conn = DriverManager.getConnection(url, txtUser.getText(), new String(((JPasswordField)txtPass).getPassword()))) {
            new HistoryService().initHistoryTable(conn);

            List<ScriptItem> scriptsParaRodar = new ArrayList<>();
            for(int i=0; i<listModel.size(); i++) {
                ScriptItem item = listModel.get(i);
                if(item.isSelected()) {
                    scriptsParaRodar.add(item);
                }
            }

            SwingUtilities.invokeAndWait(() -> {
                for(int i=0; i<listModel.size(); i++) {
                    listModel.get(i).setStatus(ScriptItem.Status.PENDING);
                }
                listScripts.repaint();
                progressBar.setMaximum(scriptsParaRodar.size());
                progressBar.setValue(0);
            });

            ScriptExecutor executor = new ScriptExecutor(cmd -> {
                final boolean[] ok = {false};
                try {
                    SwingUtilities.invokeAndWait(() -> {
                        String s = JOptionPane.showInputDialog(this, "<html><b style='color:red'>COMANDO CRITICO!</b><br>Senha Admin:</html>", "Seguranca", JOptionPane.WARNING_MESSAGE);
                        if ("suporte#1234".equals(s)) ok[0] = true;
                    });
                } catch (Exception e) {}
                return ok[0];
            });

            int idx = 0;
            boolean errorOccurred = false;

            for (ScriptItem item : scriptsParaRodar) {
                if (errorOccurred) {
                    item.setStatus(ScriptItem.Status.SKIPPED);
                    listScripts.repaint();
                    continue;
                }

                item.setStatus(ScriptItem.Status.RUNNING);
                listScripts.repaint();

                boolean success = executor.executeScript(conn, item.getPath());

                if (success) {
                    // *** LOGICA DE STATUS SIMPLIFICADA (Você pode expandir depois) ***
                    // Como não temos o retorno detalhado do ScriptExecutor (Warning/Ignored),
                    // vamos assumir sucesso padrão. Se você alterar o ScriptExecutor para retornar um Enum,
                    // pode usar switch case aqui.
                    item.setStatus(ScriptItem.Status.SUCCESS);
                } else {
                    item.setStatus(ScriptItem.Status.ERROR);
                    errorOccurred = true;
                }
                listScripts.repaint();

                if (!errorOccurred) {
                    idx++;
                    SwingUtilities.invokeLater(() -> progressBar.setValue(progressBar.getValue() + 1));
                }
            }
            logger.info("FIM DO PROCESSO.");

        } catch (Exception e) {
            logger.error("ERRO FATAL:", e);
            JOptionPane.showMessageDialog(this, "Erro Fatal: " + e.getMessage());
        } finally {
            // --- NOVO BLOCO: SALVAR E ABRIR LOG ---
            // Captura o texto atual da tela de log
            String conteudoFinal = txtLog.getText();

            // Executa o serviço de arquivo em uma thread separada para não travar a UI
            new Thread(() -> {
                com.fbupdatetool.service.FileLogService logService = new com.fbupdatetool.service.FileLogService();
                File arquivoLog = logService.salvarLogEmArquivo(conteudoFinal);

                // Abre o bloco de notas
                if (arquivoLog != null) {
                    logService.abrirLogNoBlocoDeNotas(arquivoLog);
                }
            }).start();
            // --------------------------------------

            SwingUtilities.invokeLater(() -> btnUpdate.setEnabled(true));
        }
    }


    private static class ScriptItem {
        private final Path path;
        private boolean selected;
        private Status status;

        // Novos Status adicionados (IGNORED, WARNING, SKIPPED)
        enum Status { PENDING, RUNNING, SUCCESS, ERROR, WARNING, IGNORED, SKIPPED }

        public ScriptItem(Path path, boolean selected) {
            this.path = path;
            this.selected = selected;
            this.status = Status.PENDING;
        }
        public Path getPath() { return path; }
        public boolean isSelected() { return selected; }
        public void setSelected(boolean s) { this.selected = s; }
        public Status getStatus() { return status; }
        public void setStatus(Status s) { this.status = s; }
        @Override public String toString() { return path.getFileName().toString(); }
    }

    private static class ScriptListRenderer extends JPanel implements ListCellRenderer<ScriptItem> {
        private JCheckBox checkBox;
        private JLabel lblName;

        public ScriptListRenderer() {
            setLayout(new BorderLayout(5, 0));
            setOpaque(true);
            setBorder(new CompoundBorder(
                    new EmptyBorder(1, 2, 1, 2),
                    new EmptyBorder(4, 4, 4, 4)
            ));

            checkBox = new JCheckBox();
            checkBox.setOpaque(false);

            lblName = new JLabel();
            lblName.setFont(new Font("Consolas", Font.PLAIN, 12));
            lblName.setOpaque(false);

            add(checkBox, BorderLayout.WEST);
            add(lblName, BorderLayout.CENTER);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends ScriptItem> list, ScriptItem item, int index, boolean isSelected, boolean cellHasFocus) {
            checkBox.setSelected(item.isSelected());
            lblName.setText(item.getPath().getFileName().toString());

            Color bgColor;
            Color fgColor;

            if (isSelected) {
                bgColor = list.getSelectionBackground();
                fgColor = list.getSelectionForeground();
                lblName.setIcon(null);
            } else {
                switch (item.getStatus()) {
                    case SUCCESS: // Verde
                        bgColor = BG_SUCCESS;
                        fgColor = FG_SUCCESS;
                        lblName.setIcon(FontIcon.of(MaterialDesignC.CHECK_CIRCLE, 16, FG_SUCCESS));
                        break;
                    case ERROR: // Vermelho
                        bgColor = BG_ERROR;
                        fgColor = FG_ERROR;
                        lblName.setIcon(FontIcon.of(MaterialDesignA.ALERT_CIRCLE, 16, FG_ERROR));
                        break;
                    case IGNORED: // Azul (Já existe/Duplicado)
                        bgColor = BG_INFO;
                        fgColor = FG_INFO;
                        lblName.setIcon(FontIcon.of(MaterialDesignC.CHECK_ALL, 16, FG_INFO));
                        break;
                    case WARNING: // Laranja (Metadados/Vazio)
                        bgColor = BG_WARNING;
                        fgColor = FG_WARNING;
                        lblName.setIcon(FontIcon.of(MaterialDesignA.ALERT, 16, FG_WARNING));
                        break;
                    case SKIPPED: // Roxo (Pulado)
                        bgColor = BG_SKIPPED;
                        fgColor = FG_SKIPPED;
                        lblName.setIcon(FontIcon.of(MaterialDesignS.SKIP_NEXT, 16, FG_SKIPPED));
                        break;
                    case RUNNING: // Executando
                        bgColor = new Color(227, 242, 253);
                        fgColor = new Color(21, 101, 192);
                        lblName.setIcon(FontIcon.of(MaterialDesignP.PROGRESS_CLOCK, 16, fgColor));
                        break;
                    default: // Pendente (Branco)
                        bgColor = list.getBackground();
                        fgColor = list.getForeground();
                        lblName.setIcon(null);
                }
            }
            lblName.setIconTextGap(8);

            setBackground(bgColor);
            lblName.setForeground(fgColor);
            return this;
        }
    }
}