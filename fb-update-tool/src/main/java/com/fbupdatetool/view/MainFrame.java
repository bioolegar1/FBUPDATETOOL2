package com.fbupdatetool.view;

import com.fbupdatetool.service.HistoryService;
import com.fbupdatetool.service.ScriptExecutor;
import com.fbupdatetool.util.TextAreaAppender;
import com.formdev.flatlaf.FlatClientProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
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

    // Componentes Visuais
    private JTextField txtDbPath;
    private JButton btnUpdate;
    private JTextArea txtLog;
    private JProgressBar progressBar;
    private JLabel lblStatus;

    // Cores Customizadas (Baseadas no tema Dracula/Dark)
    private final Color COLOR_BG_PANEL = new Color(60, 63, 65);
    private final Color COLOR_ACCENT = new Color(75, 110, 175); // Azul bonito
    private final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 22);
    private final Font FONT_NORMAL = new Font("Segoe UI", Font.PLAIN, 14);
    private final Font FONT_MONO = new Font("JetBrains Mono", Font.PLAIN, 12);

    public MainFrame() {
        initWindow();
        initComponents();
        TextAreaAppender.setTextArea(txtLog); // Conecta o Log
        btnUpdate.addActionListener(e -> iniciarProcessoDeAtualizacao());
    }

    private void initWindow() {
        setTitle("FBUpdateTool 2.0 Pro");
        setSize(1000, 750);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        // Fundo geral da janela
        getContentPane().setBackground(new Color(45, 45, 45));
    }

    private void initComponents() {
        // Layout Principal com margens generosas (20px)
        JPanel mainPanel = new JPanel(new BorderLayout(15, 15));
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        mainPanel.setBackground(new Color(45, 45, 45));

        // --- 1. CABEÇALHO (Header) ---
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

        // --- 2. ÁREA CENTRAL (Config + Log) ---
        JPanel centerContainer = new JPanel(new BorderLayout(0, 15));
        centerContainer.setOpaque(false);

        // 2.1 Painel de Configuração (Box bonito)
        JPanel configPanel = createRoundedPanel(" Configuração do Ambiente ");
        configPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        JLabel lblPath = new JLabel("📂 Caminho do Banco (.GDB/.FDB):");
        lblPath.setFont(FONT_NORMAL);

        txtDbPath = new JTextField("TESTE.GDB");
        txtDbPath.setFont(FONT_NORMAL);
        txtDbPath.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Ex: CLIENTE.GDB");

        // Layout do GridBag
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0; gbc.insets = new Insets(0, 0, 5, 0); gbc.anchor = GridBagConstraints.WEST;
        configPanel.add(lblPath, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        configPanel.add(txtDbPath, gbc);

        centerContainer.add(configPanel, BorderLayout.NORTH);

        // 2.2 Painel de Logs (A área preta)
        JPanel logPanel = new JPanel(new BorderLayout(0, 5));
        logPanel.setOpaque(false);

        JLabel lblLogTitle = new JLabel("📜 Auditoria de Execução:");
        lblLogTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblLogTitle.setForeground(Color.WHITE);

        txtLog = new JTextArea();
        txtLog.setEditable(false);
        txtLog.setFont(FONT_MONO);
        txtLog.setBackground(new Color(30, 30, 30)); // Mais escuro que o fundo
        txtLog.setForeground(new Color(200, 200, 200));
        txtLog.setMargin(new Insets(10, 10, 10, 10)); // Espaço interno do texto

        JScrollPane scrollPane = new JScrollPane(txtLog);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(60, 60, 60))); // Borda sutil

        logPanel.add(lblLogTitle, BorderLayout.NORTH);
        logPanel.add(scrollPane, BorderLayout.CENTER);

        centerContainer.add(logPanel, BorderLayout.CENTER);

        mainPanel.add(centerContainer, BorderLayout.CENTER);

        // --- 3. RODAPÉ (Status e Botão) ---
        JPanel footerPanel = createRoundedPanel("");
        footerPanel.setLayout(new BorderLayout(10, 0));

        // Painel de Status
        JPanel statusPanel = new JPanel(new BorderLayout(0, 5));
        statusPanel.setOpaque(false);

        lblStatus = new JLabel("Aguardando início...");
        lblStatus.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblStatus.setForeground(Color.GRAY);

        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        progressBar.setFont(new Font("Segoe UI", Font.BOLD, 12));
        progressBar.setPreferredSize(new Dimension(100, 25));

        statusPanel.add(lblStatus, BorderLayout.NORTH);
        statusPanel.add(progressBar, BorderLayout.CENTER);

        // Botão Principal
        btnUpdate = new JButton("▶ Iniciar Atualização");
        btnUpdate.setFont(new Font("Segoe UI", Font.BOLD, 16));
        btnUpdate.setBackground(new Color(40, 167, 69)); // Verde Sucesso
        btnUpdate.setForeground(Color.WHITE);
        btnUpdate.setFocusPainted(false);
        btnUpdate.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnUpdate.setPreferredSize(new Dimension(220, 50));

        footerPanel.add(statusPanel, BorderLayout.CENTER);
        footerPanel.add(btnUpdate, BorderLayout.EAST);

        mainPanel.add(footerPanel, BorderLayout.SOUTH);

        add(mainPanel);
    }

    // Helper para criar painéis bonitos com borda
    private JPanel createRoundedPanel(String title) {
        JPanel p = new JPanel();
        p.setBackground(COLOR_BG_PANEL);
        p.setBorder(new CompoundBorder(
                new LineBorder(new Color(80, 80, 80), 1, true), // Borda externa
                new EmptyBorder(15, 15, 15, 15) // Espaço interno
        ));
        return p;
    }

    // =================================================================================
    // LÓGICA DE EXECUÇÃO (BACKEND INTEGRADO)
    // =================================================================================

    private void iniciarProcessoDeAtualizacao() {
        btnUpdate.setEnabled(false);
        txtLog.setText("");
        progressBar.setValue(0);
        progressBar.setIndeterminate(true);
        lblStatus.setForeground(new Color(80, 200, 255)); // Azul
        lblStatus.setText("Conectando ao banco de dados...");

        new Thread(() -> {
            try {
                executarLogicaBackend();
            } finally {
                SwingUtilities.invokeLater(() -> {
                    btnUpdate.setEnabled(true);
                    progressBar.setIndeterminate(false);
                    progressBar.setValue(100);
                    lblStatus.setText("Processo finalizado.");
                    lblStatus.setForeground(Color.GRAY);
                });
            }
        }).start();
    }

    private void executarLogicaBackend() {
        String nomeBanco = txtDbPath.getText().trim();
        String url = "jdbc:firebirdsql://localhost:3050//firebird/data/" + nomeBanco + "?encoding=WIN1252";

        logger.info("========================================");
        logger.info("   INICIANDO CICLO DE ATUALIZAÇÃO       ");
        logger.info("========================================");
        logger.info("Conectando em: {}", url);

        try (Connection conn = DriverManager.getConnection(url, "SYSDBA", "masterkey")) {
            new HistoryService().initHistoryTable(conn);

            Path pastaScripts = Paths.get("scripts");
            if (!Files.exists(pastaScripts)) {
                logger.error("❌ ERRO CRÍTICO: Pasta 'scripts' não encontrada!");
                return;
            }

            List<Path> scripts;
            try (Stream<Path> s = Files.list(pastaScripts)) {
                scripts = s.filter(p -> p.toString().toLowerCase().endsWith(".sql"))
                        .sorted()
                        .collect(Collectors.toList());
            }

            if (scripts.isEmpty()) {
                logger.warn("⚠️ Nenhum script .sql encontrado para executar.");
                return;
            }

            logger.info("Encontrados {} scripts. Preparando motor...", scripts.size());

            SwingUtilities.invokeLater(() -> {
                progressBar.setIndeterminate(false);
                progressBar.setMaximum(scripts.size());
            });

            // CALLBACK DE SEGURANÇA (A Janela de Senha)
            ScriptExecutor executor = new ScriptExecutor(comandoProibido -> {
                final boolean[] permitido = {false};
                try {
                    SwingUtilities.invokeAndWait(() -> {
                        Toolkit.getDefaultToolkit().beep();
                        String senha = JOptionPane.showInputDialog(
                                MainFrame.this,
                                "<html><h3 style='color:red'>⚠️ BLOQUEIO DE SEGURANÇA</h3>" +
                                        "O sistema interceptou um comando crítico:<br><br>" +
                                        "<b style='font-size:14px'>" + comandoProibido + "</b><br><br>" +
                                        "Digite a senha de <b>SUPORTE</b> para autorizar:</html>",
                                "Autorização Requerida",
                                JOptionPane.WARNING_MESSAGE
                        );

                        if ("suporte#1234".equals(senha)) {
                            permitido[0] = true;
                        } else if (senha != null) {
                            JOptionPane.showMessageDialog(MainFrame.this, "Senha Incorreta!", "Erro", JOptionPane.ERROR_MESSAGE);
                        }
                    });
                } catch (Exception e) {
                    logger.error("Erro na UI de segurança", e);
                }
                return permitido[0];
            });

            int count = 0;
            int sucessos = 0;
            int ignorados = 0;

            for (Path script : scripts) {
                // Atualiza texto de status
                String scriptName = script.getFileName().toString();
                SwingUtilities.invokeLater(() -> lblStatus.setText("Processando: " + scriptName));

                boolean ok = executor.executeScript(conn, script);

                if (!ok) {
                    logger.error("⛔ EXECUÇÃO INTERROMPIDA NO SCRIPT: {}", scriptName);
                    JOptionPane.showMessageDialog(this,
                            "Erro fatal no script:\n" + scriptName + "\nVerifique o log.",
                            "Falha na Execução", JOptionPane.ERROR_MESSAGE);
                    break;
                }

                count++;
                final int progresso = count;
                SwingUtilities.invokeLater(() -> progressBar.setValue(progresso));

                // Pequena pausa estética (opcional, só pra ver a barra andar se for muito rápido)
                try { Thread.sleep(50); } catch (InterruptedException e) {}
            }

            logger.info("\n--- RESUMO DA OPERAÇÃO ---");
            logger.info("Total Processado: {}", count);
            logger.info("Verifique os detalhes acima.");

        } catch (Exception e) {
            logger.error("ERRO FATAL DE CONEXÃO: ", e);
            JOptionPane.showMessageDialog(this,
                    "Não foi possível conectar ao banco.\nErro: " + e.getMessage(),
                    "Erro de Conexão", JOptionPane.ERROR_MESSAGE);
        }
    }
}