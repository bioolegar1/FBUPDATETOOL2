package com.fbupdatetool.util;

import javafx.application.Platform;
import javafx.scene.control.TextArea;

import java.io.IOException;
import java.io.OutputStream;

public class GuiOutputStream extends OutputStream {

    private final TextArea textArea;
    private final StringBuilder buffer = new StringBuilder();

    public GuiOutputStream(TextArea textArea) {
        this.textArea = textArea;
    }
    @Override
    public void write(int b) {
        buffer.append((char) b);
        if(b=='\n'){
            flushToGui();
        }
    }

    private void flushToGui() {
        final String text = buffer.toString();
        buffer.setLength(0);

        Platform.runLater(() -> {
            textArea.appendText(text);
        });
    }
}
