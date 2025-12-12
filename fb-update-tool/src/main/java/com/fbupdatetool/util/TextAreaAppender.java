package com.fbupdatetool.util;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import javafx.application.Platform;
import javafx.scene.control.TextArea; // Note o import de JavaFX

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class TextAreaAppender extends AppenderBase<ILoggingEvent> {

    private static TextArea textArea; // Mudou de JTextArea para TextArea
    private static final StringBuilder bufferInicial = new StringBuilder();

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    public static void setTextArea(TextArea textArea) {
        TextAreaAppender.textArea = textArea;

        if (bufferInicial.length() > 0) {
            Platform.runLater(() -> { // SwingUtilities vira Platform.runLater
                try {
                    textArea.appendText(bufferInicial.toString());
                    bufferInicial.setLength(0);
                } catch (Exception ignored) {}
            });
        }
    }

    @Override
    protected void append(ILoggingEvent event) {
        String timestamp = formatter.format(Instant.ofEpochMilli(event.getTimeStamp()));
        String nivel = event.getLevel().toString();
        String mensagem = event.getFormattedMessage();
        String textoFinal = String.format("%s [%-5s] %s%n", timestamp, nivel, mensagem);

        if (textArea == null) {
            synchronized (bufferInicial) {
                bufferInicial.append(textoFinal);
            }
        } else {
            // Em JavaFX, toda alteração de tela deve ser na Thread da Aplicação
            Platform.runLater(() -> {
                try {
                    textArea.appendText(textoFinal);
                } catch (Exception ignored) {}
            });
        }
    }
}