package com.fbupdatetool;

import com.fbupdatetool.service.ConfigurationService;
import com.fbupdatetool.service.DatabaseService;
import com.fbupdatetool.service.FirebirdProcessDetector;
import com.fbupdatetool.view.MainApp;
import com.formdev.flatlaf.FlatLightLaf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static final int PADDING = 20;
    private static final int SMALL_PADDING = 10;

    public static void main(String[] args) {
        // 1. Configura o tema visual (FlatLaf) para os diálogos Swing
        try {
            FlatLightLaf.setup();
            UIManager.put("Button.arc", 8);
            UIManager.put("Component.arc", 8);
            UIManager.put("TextComponent.arc", 8);
        } catch (Exception e) {
            logger.warn("Não foi possível configurar o tema FlatLightLaf", e);
        }

        // 2. Garante a estrutura de pastas do sistema (NOVO)
        // Cria as pastas necessárias para o BackupService e ScriptExecutor funcionarem
        inicializarEstruturaPastas();

        // 3. Validação do ambiente Firebird (Swing)
        // Bloqueia a abertura se o banco não estiver rodando
        if (!validarAmbienteFirebird()) {
            logger.error("Firebird não detectado ou porta fechada. Encerrando aplicação.");
            System.exit(0);
        }

        // 4. Inicia a Aplicação Principal em JavaFX
        logger.info("Ambiente validado com sucesso. Iniciando Interface JavaFX...");

        try {
            MainApp.main(args);
        } catch (Exception e) {
            logger.error("Erro fatal ao iniciar JavaFX", e);
            JOptionPane.showMessageDialog(null,
                    "Erro ao iniciar a interface gráfica:\n" + e.getMessage(),
                    "Erro Fatal", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Cria as pastas essenciais para o funcionamento das ferramentas de atualização.
     */
    private static void inicializarEstruturaPastas() {
        String[] pastas = {"backups", "scripts", "logs"};
        for (String pasta : pastas) {
            File dir = new File(pasta);
            if (!dir.exists()) {
                if (dir.mkdirs()) {
                    logger.info("Diretório criado: {}", dir.getAbsolutePath());
                }
            }
        }
    }

    /**
     * Detecta processos do Firebird, decide a porta e verifica se está ouvindo.
     */
    private static boolean validarAmbienteFirebird() {
        FirebirdProcessDetector detector = new FirebirdProcessDetector();
        ConfigurationService config = new ConfigurationService();

        // Tenta recuperar a última porta usada, senão usa padrão
        String portaSalva = config.getLastDbPort();
        String portaParaTestar = (portaSalva != null && !portaSalva.isEmpty()) ? portaSalva : "3050";

        int processos = detector.countFirebirdProcesses();

        if (processos == 0) {
            return mostrarErroFirebirdAusente();
        } else if (processos > 1) {
            // Se tiver muitas instâncias, força o usuário a confirmar a porta
            String selecao = mostrarDialogoMultiplasInstancias(portaParaTestar);
            if (selecao == null) {
                return false; // Usuário cancelou
            }
            portaParaTestar = selecao;
        }

        // Verifica se a porta está realmente acessível (Socket connect)
        return validarPortaFirebird(portaParaTestar, config);
    }

    /**
     * Mostra diálogo para seleção de porta quando múltiplas instâncias são detectadas.
     */
    private static String mostrarDialogoMultiplasInstancias(String portaPadrao) {
        JPanel panel = new JPanel(new BorderLayout(0, SMALL_PADDING));
        panel.setBorder(new EmptyBorder(PADDING, PADDING, PADDING, PADDING));

        JLabel mensagem = new JLabel(
                "<html><div style='width: 350px;'>" +
                        "<b>Múltiplas instâncias do Firebird detectadas</b><br><br>" +
                        "O sistema detectou mais de um processo do Firebird rodando.<br>" +
                        "Por favor, confirme a porta de conexão do banco de dados:" +
                        "</div></html>"
        );

        JTextField campoPorta = new JTextField(portaPadrao, 10);
        campoPorta.setBorder(BorderFactory.createCompoundBorder(
                campoPorta.getBorder(),
                new EmptyBorder(5, 8, 5, 8)
        ));

        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        inputPanel.add(new JLabel("Porta TCP: "));
        inputPanel.add(Box.createHorizontalStrut(SMALL_PADDING));
        inputPanel.add(campoPorta);

        panel.add(mensagem, BorderLayout.NORTH);
        panel.add(inputPanel, BorderLayout.CENTER);

        int resultado = JOptionPane.showConfirmDialog(
                null,
                panel,
                "Configuração de Conexão",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );

        if (resultado == JOptionPane.OK_OPTION) {
            String porta = campoPorta.getText().trim();
            return porta.isEmpty() ? null : porta;
        }
        return null;
    }

    /**
     * Valida se a porta do Firebird está acessível e salva na configuração.
     */
    private static boolean validarPortaFirebird(String porta, ConfigurationService config) {
        try {
            int portaNum = Integer.parseInt(porta);

            if (!DatabaseService.checkFirebirdService(portaNum)) {
                return mostrarErroPortaInacessivel(porta, config);
            }

            // Sucesso: salva a porta validada para a próxima vez
            config.saveLastDbPort(porta);
            return true;

        } catch (NumberFormatException e) {
            mostrarErroPortaInvalida();
            return validarAmbienteFirebird(); // Tenta de novo
        }
    }

    /**
     * Mostra erro quando a porta está inacessível.
     */
    private static boolean mostrarErroPortaInacessivel(String porta, ConfigurationService config) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(PADDING, PADDING, PADDING, PADDING));

        JLabel mensagem = new JLabel(
                "<html><div style='width: 380px;'>" +
                        "<b style='color: #d32f2f; font-size: 14px;'>⚠️ Porta Inacessível</b><br><br>" +
                        "O serviço do Firebird está rodando, mas a porta <b>" + porta + "</b> " +
                        "não está aceitando conexões.<br><br>" +
                        "Isso pode ocorrer se:<br>" +
                        "1. O Firebird estiver usando outra porta.<br>" +
                        "2. O Firewall estiver bloqueando.<br><br>" +
                        "Deseja tentar conectar usando a porta padrão <b>3050</b>?" +
                        "</div></html>"
        );

        panel.add(mensagem, BorderLayout.CENTER);

        String[] opcoes = {"Tentar Porta 3050", "Digitar Outra", "Cancelar"};
        int escolha = JOptionPane.showOptionDialog(
                null,
                panel,
                "Erro de Conexão",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE,
                null,
                opcoes,
                opcoes[0]
        );

        if (escolha == 0) { // Tentar 3050
            return validarPortaFirebird("3050", config);
        } else if (escolha == 1) { // Digitar Outra
            String novaPorta = mostrarDialogoMultiplasInstancias("3050");
            if (novaPorta != null) {
                return validarPortaFirebird(novaPorta, config);
            }
        }
        return false;
    }

    /**
     * Mostra erro quando o Firebird não está em execução.
     */
    private static boolean mostrarErroFirebirdAusente() {
        JPanel panel = new JPanel(new BorderLayout(0, PADDING));
        panel.setBorder(new EmptyBorder(PADDING, PADDING, PADDING, PADDING));

        JLabel titulo = new JLabel(
                "<html><b style='color: #d32f2f; font-size: 14px;'>❌ ERRO CRÍTICO: Firebird Parado</b></html>"
        );

        JLabel mensagem = new JLabel(
                "<html><div style='width: 400px;'>" +
                        "O sistema não encontrou o processo <code>fbserver.exe</code> ou <code>firebird.exe</code>.<br><br>" +
                        "<b>Ação Necessária:</b><br>" +
                        "Por favor, inicie o serviço do Firebird no Windows e clique em 'Tentar Novamente'." +
                        "</div></html>"
        );

        panel.add(titulo, BorderLayout.NORTH);
        panel.add(mensagem, BorderLayout.CENTER);

        String[] opcoes = {"Tentar Novamente", "Sair"};
        int escolha = JOptionPane.showOptionDialog(
                null,
                panel,
                "Serviço Não Encontrado",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.ERROR_MESSAGE,
                null,
                opcoes,
                opcoes[0]
        );

        if (escolha == 0) {
            return validarAmbienteFirebird();
        }
        return false;
    }

    private static void mostrarErroPortaInvalida() {
        JOptionPane.showMessageDialog(null,
                "A porta informada não é válida.\nDigite apenas números (Ex: 3050).",
                "Valor Inválido",
                JOptionPane.WARNING_MESSAGE);
    }
}