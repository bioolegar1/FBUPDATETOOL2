package com.fbupdatetool.service;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * Gerenciador de pasta de scripts para o atualizador de banco de dados Firebird.
 * Responsável por resolver e validar a pasta de scripts SQL a serem executados em sequência.
 *
 * Atualizações nesta versão:
 * - Uso de JavaFX para dialogs (Alert e DirectoryChooser) em vez de Swing.
 * - Verificações de null e existência de paths para evitar erros.
 * - Logs aprimorados para depuração.
 * - Criação automática de pasta padrão no Windows com confirmação do usuário.
 */
public class ScriptFolderManager {

    private static final Logger logger = LoggerFactory.getLogger(ScriptFolderManager.class);
    private final ConfigurationService configService;
    private final Stage parentStage;

    private static final String WINDOWS_DEFAULT_PATH = "C:\\SoluçõesPillar\\scriptsAtt";

    public ScriptFolderManager(Stage parentStage) {
        this.parentStage = parentStage;
        this.configService = new ConfigurationService();
    }

    /**
     * Determina qual pasta de scripts usar baseada nas regras de negócio.
     * @return Path da pasta validada ou null se o usuário cancelar.
     */
    public Path resolveScriptPath() {
        String savedPath = configService.getLastScriptFolder();
        if (!savedPath.isEmpty()) {
            Path path = Paths.get(savedPath);
            if (Files.exists(path) && Files.isDirectory(path)) {
                logger.info("Pasta de scripts recuperada do histórico: {}", path);
                return path;
            }
        }

        // Se não tem histórico, aplica lógica do SO
        Path selectedPath;
        if (isWindows()) {
            selectedPath = handleWindowsLogic();
        } else {
            selectedPath = handleOtherSystemLogic();
        }

        if (selectedPath != null) {
            configService.saveLastScriptFolder(selectedPath.toString());
            logger.info("Nova pasta de scripts definida: {}", selectedPath);
        }

        return selectedPath;
    }

    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    private Path handleWindowsLogic() {
        Path defaultPath = Paths.get(WINDOWS_DEFAULT_PATH);

        if (Files.exists(defaultPath)) {
            return defaultPath;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Configuração Inicial");
        alert.setContentText("A pasta padrão de scripts não foi encontrada: \n"
                + WINDOWS_DEFAULT_PATH
                + "\n\nDeseja criá-la agora automaticamente?");
        Optional<ButtonType> choice = alert.showAndWait();

        if (choice.isPresent() && choice.get() == ButtonType.OK) {
            try {
                File dir = defaultPath.toFile();
                if (dir.mkdirs()) {
                    showInfo("Pasta criada com sucesso! \nColoque seus scripts nela.");
                    return defaultPath;
                } else {
                    showError("Falha ao criar pasta (Permissão Negada?).\nPor favor, escolha uma pasta manualmente.");
                }
            } catch (Exception e) {
                logger.error("Erro ao criar pasta padrão", e);
            }
        }

        return chooseFolderManually();
    }

    private Path handleOtherSystemLogic() {
        showInfo("Como você não está no Windows, precisamos que selecione a pasta onde estão os scripts de atualização.");
        return chooseFolderManually();
    }

    private Path chooseFolderManually() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Selecione a Pasta de Scripts (.sql)");
        chooser.setInitialDirectory(new File(System.getProperty("user.home")));

        File selected = chooser.showDialog(parentStage);
        if (selected != null) {
            return selected.toPath();
        }
        return null;
    }

    // Helpers para dialogs JavaFX
    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Informação");
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erro");
        alert.setContentText(message);
        alert.showAndWait();
    }
}