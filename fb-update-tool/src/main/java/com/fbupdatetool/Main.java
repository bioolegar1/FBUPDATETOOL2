package com.fbupdatetool;

import com.fbupdatetool.service.*;
import com.fbupdatetool.view.MainFrame;
import com.formdev.flatlaf.FlatDarkLaf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        // Configura o tema visual
        try { FlatDarkLaf.setup(); } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> {
            // 1. Orquestração de Ambiente (Checa se o Firebird está vivo)
            if (!validarAmbienteFirebird()) {
                logger.error("Firebird não detectado ou porta fechada. Encerrando.");
                System.exit(0);
            }

            // 2. Se passou, abre a janela
            logger.info("Ambiente validado. Iniciando Interface Gráfica...");
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }

    /**
     * Detecta processos, decide a porta e verifica se está ouvindo.
     */
    private static boolean validarAmbienteFirebird() {
        FirebirdProcessDetector detector = new FirebirdProcessDetector();
        ConfigurationService config = new ConfigurationService();

        // Correção 1: Declarar a variável antes de usar
        String portaEscolhida = "3050";

        int processos = detector.countFirebirdProcesses();

        if (processos == 0) {
            // Correção 6: Nome do método corrigido
            return mostrarErroFirebirdAusente();
        }
        else if (processos > 1) {
            // Correção 2: Tratamento seguro do input (evita NullPointerException)
            Object inputObj = JOptionPane.showInputDialog(null,
                    "Detectamos múltiplas instâncias do Firebird rodando.\n" +
                            "Qual porta você deseja utilizar para conexão?",
                    "Múltiplos serviços detectados",
                    JOptionPane.QUESTION_MESSAGE,
                    null, null, config.getLastDbPort());

            if (inputObj == null || inputObj.toString().trim().isEmpty()) {
                return false; // Usuário cancelou
            }
            portaEscolhida = inputObj.toString().trim();
        }
        else {
            // Se tem 1 processo, usa a última porta salva ou a padrão
            portaEscolhida = config.getLastDbPort();
        }

        // Teste da porta se ela esta ouvindo (Socket)
        // Correção de lógica: garante que portaEscolhida tem valor
        try {
            if (!DatabaseService.checkFirebirdService(Integer.parseInt(portaEscolhida))) {
                int tentativa = JOptionPane.showConfirmDialog(null,
                        "O serviço do Firebird foi detectado no Windows, mas a porta [" + portaEscolhida + "] está fechada.\n" +
                                "Deseja tentar outra porta?",
                        "Porta Inacessível",
                        JOptionPane.YES_NO_OPTION);

                if (tentativa == JOptionPane.YES_OPTION) {
                    config.saveLastDbPort("3050"); // Reseta para tentar o padrão
                    return validarAmbienteFirebird(); // Tenta de novo (Recursão)
                }
                return false;
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(null, "A porta digitada não é um número válido.");
            return validarAmbienteFirebird();
        }

        // Sucesso: Salva a porta validada
        config.saveLastDbPort(portaEscolhida);
        return true;
    }

    private static boolean mostrarErroFirebirdAusente() {
        // Correção 6: Typos corrigidos (optipons -> options)
        Object[] options = {"Tentar Novamente", "Sair"};

        int escolha = JOptionPane.showOptionDialog(null,
                // Correção 5: HTML corrigido (<b style...>) e typos de texto (services.msc)
                "<html><b style='color:red'>ERRO CRÍTICO: Firebird não encontrado!</b><br>" +
                        "O sistema não detectou nenhum processo 'fbserver.exe' rodando.<br><br>" +
                        "1. Verifique se o Firebird está instalado.<br>" +
                        "2. Inicie o serviço no Windows (services.msc).</html>",
                "Serviço Parado", // Correção: Prado -> Parado
                JOptionPane.YES_NO_OPTION,
                JOptionPane.ERROR_MESSAGE,
                null,
                options,
                options[0]);

        if (escolha == 0) return validarAmbienteFirebird(); // Tenta de novo
        return false;
    }

    // Correção 3 e 4: REMOVIDO o método 'rodarBackendComScriptsReais'.
    // Ele não serve mais para nada, pois a lógica de execução agora pertence ao MainFrame.
}