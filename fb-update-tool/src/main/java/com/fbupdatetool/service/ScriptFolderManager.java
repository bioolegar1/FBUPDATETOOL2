package com.fbupdatetool.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


public class ScriptFolderManager {

    private static final  Logger logger = LoggerFactory.getLogger(ScriptFolderManager.class);
    private final ConfigurationService configService;
    private final Component parentComponent;

    private static final String WINDOWS_DEFAULT_PATH = "C:\\SolucõesPillar\\scriptsAtt";

    public ScriptFolderManager(Component parentComponent) {
        this.parentComponent = parentComponent;
        this.configService = new ConfigurationService();
    }


    /**
     * Determina qual pasta de scripts usar baseada nas regras de negócio
     * @return Path da pasta validada ou null se o usuário cancelar
     */
    public Path resolveScriptPath(){
        String savePath = configService.getLastScriptFolder();
        if(!savePath.isEmpty()){
            Path path = Paths.get(savePath);
            if (Files.exists(path) && Files.isDirectory(path)) {
                logger.info("Pasta de scripts recuperada do histórico: {}", path);
                return path;
            }
        }

        //Se nao tem historico, aplica lógica do SO
        Path selectedPath;
        if (isWindows()){
            selectedPath = handleWindowsLogic();
        }else {
            selectedPath = handleOtherSystemmLogic();
        }

        if (selectedPath == null){
            configService.saveLastScriptFolder(selectedPath.toString());
            logger.info("Nova psta de scripts definida: {}", selectedPath);
        }

        return selectedPath;
    }

    private boolean isWindows(){
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    private Path handleWindowsLogic(){
        Path defaultPath = Paths.get(WINDOWS_DEFAULT_PATH);

        if(Files.exists(defaultPath)){
            return defaultPath;
        }

        int choice = JOptionPane.showConfirmDialog(parentComponent,
                "A pasta padrão de scripts não foi encontrada: \n"
                        + WINDOWS_DEFAULT_PATH
                        + "\n\nDeseja criá-la agora automaticamente?",
                        "Configuração Inicial",
                        JOptionPane.YES_OPTION,
                        JOptionPane.QUESTION_MESSAGE
                );

        if (choice == JOptionPane.YES_OPTION) {
            try{
                File dir = defaultPath.toFile();
                if(dir.mkdirs()) {
                    JOptionPane.showMessageDialog(parentComponent,
                            "Pasta criada com sucesso! \nColoque seus scripts nela.");
                    return defaultPath;
                }else {
                    JOptionPane.showMessageDialog(parentComponent,
                            "Falha ao criar pasta(Permissão Negada?).\nPor favor, escolha uma pasta manualmente.",
                            "Erro",
                            JOptionPane.ERROR_MESSAGE);
                }
            }catch (Exception e){
                logger.error("Erro ao criar pasta padrão", e);
            }
        }

        return chooseFolderManually();
    }

    private Path handleOtherSystemmLogic(){
        JOptionPane.showMessageDialog(parentComponent,
                "Como você não esta no Windows, precisamos que selecione a pasta onde estão os scripts de atualização.",
                "Seleção de Scripts",
                JOptionPane.INFORMATION_MESSAGE);

        return chooseFolderManually();
    }

    private Path chooseFolderManually(){
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Selecione o arquivo de Scripts (.sql)");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        chooser.setCurrentDirectory(new File(System.getProperty("user.home")));

        int result = chooser.showOpenDialog(parentComponent);
        if (result == JFileChooser.APPROVE_OPTION) {
            return chooser.getSelectedFile().toPath();
        }
        return null;
    }
}
