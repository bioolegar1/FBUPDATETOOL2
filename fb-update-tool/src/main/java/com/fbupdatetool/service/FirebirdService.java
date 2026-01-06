package com.fbupdatetool.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class FirebirdService {

    /**
     * Realiza o backup de um banco de dados (.FDB/.GDB) para um arquivo de backup (.GBK/.FBK).
     */
    public void realizarBackup(String caminhoGbak, String usuario, String senha,
                               File bancoOrigem, File arquivoDestinoGbk,
                               Consumer<String> logger) throws Exception {

        if (!new File(caminhoGbak).exists()) {
            throw new Exception("Executável GBAK não encontrado em: " + caminhoGbak);
        }

        // Comando: gbak -v -t -user SYSDBA -password masterkey origem.fdb destino.gbk
        List<String> command = new ArrayList<>();
        command.add(caminhoGbak);

        // Opcional: Também podemos aumentar o buffer no backup para performance
        command.add("-bu");
        command.add("2048");

        command.add("-v"); // Verbose (detalhado)
        command.add("-t"); // Transportable (backup portável)
        command.add("-user");
        command.add(usuario);
        command.add("-password");
        command.add(senha);
        command.add(bancoOrigem.getAbsolutePath());
        command.add(arquivoDestinoGbk.getAbsolutePath());

        logger.accept("Iniciando comando de Backup...");
        executarProcesso(command, logger);
    }

    /**
     * Restaura um backup (.GBK/.FBK) para um arquivo de banco de dados (.FDB/.GDB).
     */
    public void restaurarBackup(String caminhoGbak, String usuario, String senha,
                                File arquivoBackupOrigem, File bancoDestino, boolean substituir,
                                Consumer<String> logger) throws Exception {

        if (!new File(caminhoGbak).exists()) {
            throw new Exception("Executável GBAK não encontrado em: " + caminhoGbak);
        }

        // Validação de segurança
        if (bancoDestino.exists() && !substituir) {
            throw new Exception("O banco de destino já existe e a opção 'Substituir' não foi marcada.\n" +
                    "Destino: " + bancoDestino.getAbsolutePath());
        }

        List<String> command = new ArrayList<>();
        command.add(caminhoGbak);

        // === OTIMIZAÇÃO DE PERFORMANCE ===
        // Aumenta o buffer de páginas para 3000.
        // Isso reduz o I/O de disco e acelera a restauração.
        command.add("-bu");
        command.add("3000");
        // =================================

        command.add("-v"); // Verbose

        // Define o modo de restauração:
        // -c (Create): Cria um novo banco. Falha se já existir.
        // -rep (Replace): Substitui o banco se já existir.
        if (substituir) {
            command.add("-rep");
            logger.accept("Modo: SUBSTITUIR banco existente (-rep).");
        } else {
            command.add("-c");
            logger.accept("Modo: CRIAR novo banco (-c).");
        }

        command.add("-user");
        command.add(usuario);
        command.add("-password");
        command.add(senha);

        // Na restauração, a ordem é inversa ao backup:
        // 1º O arquivo de Backup (.GBK)
        // 2º O arquivo de Banco Destino (.FDB/.GDB)
        command.add(arquivoBackupOrigem.getAbsolutePath());
        command.add(bancoDestino.getAbsolutePath());

        logger.accept("Iniciando comando de Restauração (Buffers otimizados: 3000)...");
        executarProcesso(command, logger);
    }

    /**
     * Método auxiliar privado para rodar o processo no Windows e capturar o log.
     * Evita repetir código entre o backup e o restore.
     */
    private void executarProcesso(List<String> command, Consumer<String> logger) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true); // Joga erros no mesmo fluxo de texto

        Process process = pb.start();

        // Lê a saída do GBAK linha por linha e envia para a tela
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (logger != null) {
                    logger.accept("GBAK: " + line);
                }
            }
        }

        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new Exception("O GBAK terminou com erro. Código de saída: " + exitCode +
                    "\nVerifique o log para detalhes (senha errada, arquivo em uso, etc).");
        }
    }
}