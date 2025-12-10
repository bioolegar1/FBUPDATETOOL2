package com.fbupdatetool.util;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

import javax.swing.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class TextAreaAppender extends AppenderBase<ILoggingEvent> {

    private static JTextArea textArea;

    // BUFFER: Guarda as mensagens enquanto a janela não abre
    private static final StringBuilder bufferInicial = new StringBuilder();

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    /**
     * Conecta a interface ao sistema de Log.
     * Quando chamado, despeja tudo o que estava guardado no buffer.
     */
    public static void setTextArea(JTextArea textArea) {
        TextAreaAppender.textArea = textArea;

        // Se temos mensagens guardadas do boot, escreve elas agora
        if (bufferInicial.length() > 0) {
            SwingUtilities.invokeLater(() -> {
                try {
                    textArea.append(bufferInicial.toString());
                    bufferInicial.setLength(0); // Limpa a memória
                    textArea.setCaretPosition(textArea.getDocument().getLength());
                } catch (Exception ignored) {}
            });
        }
    }

    @Override
    protected void append(ILoggingEvent event) {
        // Formata a mensagem: 15:48:00 [INFO] Mensagem...
        String timestamp = formatter.format(Instant.ofEpochMilli(event.getTimeStamp()));
        String nivel = event.getLevel().toString();
        // Limpa o nome do pacote para ficar mais curto (ex: c.f.service.DatabaseService)
        String loggerName = event.getLoggerName();
        String shortLogger = loggerName.substring(loggerName.lastIndexOf('.') + 1);

        String mensagem = event.getFormattedMessage();
        String textoFinal = String.format("%s [%-5s] %s%n", timestamp, nivel, mensagem);

        if (textArea == null) {
            // A janela ainda não abriu? Guarda na memória!
            synchronized (bufferInicial) {
                bufferInicial.append(textoFinal);
            }
        } else {
            // A janela já existe? Escreve nela direto.
            SwingUtilities.invokeLater(() -> {
                try {
                    textArea.append(textoFinal);
                    textArea.setCaretPosition(textArea.getDocument().getLength());
                } catch (Exception ignored) {}
            });
        }
    }
}