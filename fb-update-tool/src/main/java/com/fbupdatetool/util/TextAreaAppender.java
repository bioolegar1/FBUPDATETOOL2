package com.fbupdatetool.util;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import javax.swing.*;
import javax.swing.text.DefaultCaret;

/**
 * REDIRECIONADOR DE LOGS
 * Pega o que o sistema escreve no console e joga para a janelinha do aplicativo.
 */
public class TextAreaAppender extends AppenderBase<ILoggingEvent> {

    private static JTextArea textArea;

    // O MainFrame chama isso para dizer: "Escreva os logs AQUI"
    public static void setTextArea(JTextArea textAreaInstance) {
        textArea = textAreaInstance;

        // Truque para o texto rolar para baixo sozinho (Auto-Scroll)
        if (textArea != null) {
            DefaultCaret caret = (DefaultCaret) textArea.getCaret();
            caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        }
    }

    @Override
    protected void append(ILoggingEvent eventObject) {
        if (textArea == null) return;

        // Formata a mensagem: [INFO] Texto...
        String message = String.format("[%s] %s\n", eventObject.getLevel(), eventObject.getFormattedMessage());

        // O Swing não deixa outras threads mexerem na tela, então pedimos "por favor" (invokeLater)
        SwingUtilities.invokeLater(() -> {
            try {
                textArea.append(message);
            } catch (Exception e) {
                // Ignora erro visual
            }
        });
    }
}