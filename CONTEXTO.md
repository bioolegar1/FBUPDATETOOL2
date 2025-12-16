# Documento de Contexto Técnico: FBUpdateTool v2.0

**Data:** 05/01/2026
**Tecnologia:** Java 17, JavaFX, Firebird SQL (Jaybird), Maven.
**Status:** Em Desenvolvimento (Fase de Testes e Refinamento).

---

## 1. Visão Geral da Arquitetura

O projeto é uma ferramenta de automação para atualizações de banco de dados Firebird. A arquitetura foi migrada de Swing para **JavaFX** para modernizar a interface, mantendo a compatibilidade com o ecossistema Java 17.

### Estrutura de Inicialização (`Main` vs `MainApp`)
Foi adotada uma estratégia de inicialização em duas etapas para garantir robustez:

* **`Main.java` (Launcher):**
    * **Função:** Atua como ponto de entrada da aplicação. Não herda de `Application` do JavaFX.
    * **Lógica:** Realiza validações críticas de ambiente (existência de pastas de logs/backup, verificação se o serviço Firebird está rodando na porta correta) antes de carregar qualquer componente gráfico.
    * **Por que:** Evita erros de "JavaFX runtime components are missing" ao gerar artefatos .jar/.exe e garante que a aplicação só abra se o ambiente estiver saudável.

* **`MainApp.java` (Interface):**
    * **Função:** Classe que herda de `javafx.application.Application`.
    * **Lógica:** Responsável pela construção da UI, carregamento de CSS e orquestração dos serviços de atualização e backup via eventos da interface.

---

## 2. Motor de Atualização e Scripts (`ScriptExecutor`)

O sistema não apenas executa SQL, mas interpreta e corrige cenários comuns de falha em scripts legados.

### A. Análise e Correção de Scripts (`ScriptParser`)
* **Problema:** Scripts antigos salvos em ANSI (Windows-1252) quebram caracteres quando lidos como UTF-8.
* **Solução (Fallback de Encoding):** O parser tenta ler o arquivo como UTF-8. Se detectar erro de formatação (`MalformedInputException`), captura a exceção e relê o arquivo usando ISO-8859-1.
* **Auto-Fix (SET TERM):** O sistema detecta a presença de comandos complexos (Triggers/Procedures) e injeta virtualmente os delimitadores `SET TERM` caso o desenvolvedor tenha esquecido de incluí-los no arquivo original.

### B. Gestão de Dependências (Fila de Espera)
* **Lógica:** Scripts executados em ordem alfabética podem falhar se o Script A tentar criar uma View que depende de uma Tabela do Script B.
* **Solução (`DeferredCommand`):** Se um comando falhar com erros de "Objeto não encontrado" ou "Coluna desconhecida", ele não gera erro fatal imediatamente. O comando é colocado em uma lista de espera. Após processar todos os scripts, o sistema reprocessa essa lista repetidamente (até 20 passadas) para resolver as dependências tardias.

### C. Smart Merge (Idempotência)
* **Objetivo:** Permitir que o mesmo script rode múltiplas vezes sem erro.
* **Lógica:** Antes de executar um `CREATE TABLE` ou `ALTER TABLE`, o sistema consulta os metadados do Firebird (`RDB$RELATION_FIELDS`). Se a tabela ou coluna já existir, o comando é ignorado ou transformado em um ajuste (ex: `ALTER TABLE ADD` se faltar uma coluna), garantindo que a base do cliente convirja para o estado desejado sem duplicidade.

---

## 3. Sistema de Backup Integrado (`BackupService`)

O backup foi desenhado para eliminar dependências externas e fornecer feedback visual.

* **Uso do Jaybird (Nativo):**
    * **Por que:** Substitui a chamada de sistema ao `gbak.exe`. Isso elimina problemas com variáveis de ambiente (PATH) e versões incorretas do utilitário gbak no cliente.
    * **Implementação:** Utiliza a classe `FBBackupManager` do driver JDBC.

* **Streaming de Logs (`GuiOutputStream`):**
    * **Lógica:** A saída padrão (verbose) do processo de backup é redirecionada para uma implementação customizada de `OutputStream`. Esta classe intercepta os bytes de texto e os envia para o componente `TextArea` da interface JavaFX.
    * **Resultado:** O usuário vê o progresso real ("Writing table X...") na tela, em vez de uma tela congelada.

* **Resiliência de Diretório:**
    * O sistema tenta salvar o backup na pasta do banco de dados original. Se houver erro de permissão de escrita (comum em servidores), ele faz fallback automático para `C:\Temp` ou solicita um novo local ao usuário.

---

## 4. Monitoramento e Logs (`HistoryService` e `Logback`)

* **Tabela de Histórico Interna:**
    * O sistema cria e mantém uma tabela `FB_UPDATE_HISTORY` dentro do banco do cliente.
    * Scripts marcados como `SUCCESS` nesta tabela são pulados automaticamente em execuções futuras, economizando tempo.

* **Logs Visuais e em Arquivo:**
    * Utiliza `Logback` configurado com um `TextAreaAppender`. Isso garante que qualquer log gerado pelo sistema (INFO, ERROR) seja exibido tanto no arquivo de texto físico (para auditoria) quanto na interface gráfica (para o operador), sem duplicação de código de log.

---

## 5. Validação de Saúde (`DatabaseHealthService`)

Antes e depois da atualização, o sistema executa um "Deep Scan":
1.  **Views:** Tenta realizar um `SELECT` vazio em todas as views para garantir que nenhuma dependência foi quebrada.
2.  **Procedures/Triggers:** Verifica flags de binário inválido (`RDB$VALID_BLR`) nas tabelas de sistema.
    Isso garante que o banco não seja entregue ao cliente em um estado inconsistente.