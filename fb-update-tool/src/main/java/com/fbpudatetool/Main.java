package com.fbpudatetool;

import com.formdev.flatlaf.FlatDarkLaf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {

        try {
            FlatDarkLaf.setup();
            logger.info("Tema FlatLaf configurado com sucesso.");
        }catch (Exception e){
            logger.error("Falha ao configurar tema", e);

        }
        logger.info("[INFO] Iniciando FBUpdateTool...");

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("FBUpdateTool 2.0 - Teste Inicial");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(600, 400);

            frame.add(new JButton("Olá! O ambiente Java 25 esta configurado."), BorderLayout.CENTER);

            frame.setVisible(true);
            logger.info("Janela principal exibida.");
        });

    }
}