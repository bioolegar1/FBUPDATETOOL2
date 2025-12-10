package com.fbupdatetool;

import com.fbupdatetool.service.*;
import com.fbupdatetool.view.MainFrame;
import com.formdev.flatlaf.FlatLightLaf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static final int PADDING = 20;
    private static final int SMALL_PADDING = 10;

    public static void main(String[] args) {
        // Configura o tema visual claro
        try {
            FlatLightLaf.setup();
            UIManager.put("Button.arc", 8);
            UIManager.put("Component.arc", 8);
            UIManager.put("TextComponent.arc", 8);
        } catch (Exception e) {
            logger.warn("Não foi possível configurar o tema FlatLightLaf", e);
        }

        SwingUtilities.invokeLater(() -> {
            // Validação do ambiente Firebird
            if (!validarAmbienteFirebird()) {
                logger.error("Firebird não detectado ou porta fechada. Encerrando.");
                System.exit(0);
            }

            // Inicia a interface gráfica
            logger.info("Ambiente validado. Iniciando Interface Gráfica...");
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }

    /**
     * Detecta processos do Firebird, decide a porta e verifica se está ouvindo.
     */
    private static boolean validarAmbienteFirebird() {
        FirebirdProcessDetector detector = new FirebirdProcessDetector();
        ConfigurationService config = new ConfigurationService();

        String portaEscolhida = "3050";
        int processos = detector.countFirebirdProcesses();

        if (processos == 0) {
            return mostrarErroFirebirdAusente();
        }
        else if (processos > 1) {
            portaEscolhida = mostrarDialogoMultiplasInstancias(config);
            if (portaEscolhida == null) {
                return false; // Usuário cancelou
            }
        }
        else {
            // Se tem 1 processo, usa a última porta salva ou a padrão
            portaEscolhida = config.getLastDbPort();
        }

        // Verifica se a porta está acessível
        return validarPortaFirebird(portaEscolhida, config);
    }

    /**
     * Mostra diálogo para seleção de porta quando múltiplas instâncias são detectadas.
     */
    private static String mostrarDialogoMultiplasInstancias(ConfigurationService config) {
        JPanel panel = new JPanel(new BorderLayout(0, SMALL_PADDING));
        panel.setBorder(new EmptyBorder(PADDING, PADDING, PADDING, PADDING));

        JLabel mensagem = new JLabel(
                "<html><div style='width: 350px;'>" +
                        "<b>Múltiplas instâncias do Firebird detectadas</b><br><br>" +
                        "Foram encontrados vários processos do Firebird em execução. " +
                        "Por favor, informe a porta que deseja utilizar para a conexão:" +
                        "</div></html>"
        );

        JTextField campoPorta = new JTextField(config.getLastDbPort(), 10);
        campoPorta.setBorder(BorderFactory.createCompoundBorder(
                campoPorta.getBorder(),
                new EmptyBorder(5, 8, 5, 8)
        ));

        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        inputPanel.add(new JLabel("Porta: "));
        inputPanel.add(Box.createHorizontalStrut(SMALL_PADDING));
        inputPanel.add(campoPorta);

        panel.add(mensagem, BorderLayout.NORTH);
        panel.add(inputPanel, BorderLayout.CENTER);

        int resultado = JOptionPane.showConfirmDialog(
                null,
                panel,
                "Seleção de Porta",
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
     * Valida se a porta do Firebird está acessível.
     */
    private static boolean validarPortaFirebird(String porta, ConfigurationService config) {
        try {
            int portaNum = Integer.parseInt(porta);

            if (!DatabaseService.checkFirebirdService(portaNum)) {
                return mostrarErroPortaInacessivel(porta, config);
            }

            // Sucesso: salva a porta validada
            config.saveLastDbPort(porta);
            return true;

        } catch (NumberFormatException e) {
            mostrarErroPortaInvalida();
            return validarAmbienteFirebird();
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
                        "<b style='color: #d32f2f;'>Porta Inacessível</b><br><br>" +
                        "O serviço do Firebird foi detectado no Windows, mas a porta <b>" + porta + "</b> " +
                        "não está respondendo.<br><br>" +
                        "Deseja tentar conectar usando a porta padrão <b>3050</b>?" +
                        "</div></html>"
        );

        panel.add(mensagem, BorderLayout.CENTER);

        String[] opcoes = {"Tentar Porta 3050", "Cancelar"};
        int escolha = JOptionPane.showOptionDialog(
                null,
                panel,
                "Erro de Conexão",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE,
                null,
                opcoes,
                opcoes[0]
        );

        if (escolha == 0) {
            config.saveLastDbPort("3050");
            return validarAmbienteFirebird();
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
                "<html><b style='color: #d32f2f; font-size: 13px;'>ERRO CRÍTICO: Firebird não encontrado!</b></html>"
        );

        JLabel mensagem = new JLabel(
                "<html><div style='width: 400px;'>" +
                        "O sistema não detectou nenhum processo <code>fbserver.exe</code> em execução.<br><br>" +
                        "<b>Possíveis soluções:</b><br>" +
                        "• Verifique se o Firebird está instalado corretamente<br>" +
                        "• Inicie o serviço através do Gerenciador de Serviços do Windows (<code>services.msc</code>)<br>" +
                        "• Certifique-se de que o serviço não está bloqueado pelo firewall" +
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

    /**
     * Mostra erro quando a porta digitada não é válida.
     */
    private static void mostrarErroPortaInvalida() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(PADDING, PADDING, PADDING, PADDING));

        JLabel mensagem = new JLabel(
                "<html><div style='width: 320px;'>" +
                        "A porta informada não é um número válido.<br><br>" +
                        "Por favor, digite apenas números (ex: <b>3050</b>)." +
                        "</div></html>"
        );

        panel.add(mensagem, BorderLayout.CENTER);

        JOptionPane.showMessageDialog(
                null,
                panel,
                "Porta Inválida",
                JOptionPane.ERROR_MESSAGE
        );
    }
}