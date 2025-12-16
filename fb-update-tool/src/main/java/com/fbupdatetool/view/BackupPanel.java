package com.fbupdatetool.view;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

public class BackupPanel  extends JPanel {


    private JCheckBox chkCompactar;
    private JButton btnIniciarBackup;
    private JProgressBar progressBar;
    private JTextArea txtLog;
    private JLabel lblStatus;

    public  BackupPanel(){
        initComponents();
    }

    private void initComponents() {

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15,15,15,15));

        JPanel panelControles = new JPanel(new FlowLayout(FlowLayout.LEFT));

        chkCompactar = new JCheckBox("Compactar arquivo final (.zip)?");
        chkCompactar.setSelected(true);
        chkCompactar.setFocusPainted(false);

        btnIniciarBackup = new JButton("Iniciar Backup");
        btnIniciarBackup.addActionListener(e -> System.out.println("Botão clicado (Lógica pendente)"));

        panelControles.add(chkCompactar);
        panelControles.add(btnIniciarBackup);

        txtLog = new JTextArea();
        txtLog.setEditable(false);
        txtLog.setFont(new Font("Monospaced", Font.PLAIN, 12));

        JScrollPane scrollLog = new JScrollPane(txtLog);
        scrollLog.setBorder(BorderFactory.createTitledBorder("Log de Execução"));

        JPanel panelStatus = new JPanel(new GridLayout(0, 1, 5, 5));

        lblStatus = new JLabel("Aguardando Comando...");

        progressBar = new JProgressBar(0, 100);
        progressBar.setValue(0);
        progressBar.setStringPainted(true);

        panelStatus.add(lblStatus);
        panelStatus.add(progressBar);

        add(panelControles, BorderLayout.NORTH);
        add(scrollLog, BorderLayout.CENTER);
        add(panelStatus, BorderLayout.SOUTH);
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());}
            catch (Exception ignored){
        }
        JFrame frame = new JFrame("Teste Issue #1");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600,400);
        frame.setLocationRelativeTo(null);

        frame.add(new BackupPanel());
        frame.setVisible(true);
    }
}
