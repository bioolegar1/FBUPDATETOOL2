# 📝 Documento de Contexto: Migração JavaFX e Refatoração de Backup

**Projeto:** FBUpdateTool (Firebird Update Utility)  
**Data:** 12/12/2025  
**Status:** Funcional (com observação sobre caminhos Docker/Mac)

---

## 1. Visão Geral
Nesta sessão, realizamos a migração completa da interface gráfica de **Swing** para **JavaFX**, modernizando o visual e a arquitetura. Além disso, reescrevemos o sistema de Backup para ser mais robusto, visual e controlável pelo usuário.

---

## 2. Alterações Estruturais (Migração JavaFX)

### 📦 Dependências (`pom.xml`)
* **Removido:** `flatlaf`, `ikonli-swing`.
* **Adicionado:**
    * `javafx-controls` e `javafx-fxml` (Versão 17.0.8).
    * `ikonli-javafx` e `ikonli-materialdesign2-pack` (Para ícones vetoriais modernos).
* **Ajuste:** Configuração do plugin `maven-shade-plugin` para garantir que o JAR executável funcione corretamente com o launcher.

### 🏗 Arquitetura de Inicialização
* **`Main.java` (Launcher):** Criada uma classe "porteira" que apenas chama o JavaFX. Isso evita erros de módulo ao criar executáveis `.exe` ou `.jar`.
* **`MainApp.java` (Application):** Nova classe principal que estende `javafx.application.Application`, substituindo o antigo `MainFrame`.
* **Threading:** Implementação rigorosa de `Platform.runLater()` para garantir que alterações na interface (UI) sejam feitas apenas na thread correta, evitando travamentos.

---

## 3. Melhorias no Sistema de Backup (`BackupService`)

### 🛠 Correções Técnicas
* **Correção de `String.format`:** Substituída a formatação do nome do arquivo por concatenação simples (`+`). Isso resolveu o erro crítico `Conversion = '.'` que ocorria ao processar nomes de arquivos com pontos.
* **Stream de Saída:** O método `performBackup` agora aceita um `OutputStream`. Isso permite desviar o log do Jaybird (Gbak) para qualquer lugar.

### 📺 Feedback Visual (Console Hacker)
* **`GuiOutputStream.java`:** Criada classe utilitária que intercepta o texto do console e o escreve em um `TextArea` do JavaFX em tempo real.
* **Popup de Progresso:** Ao iniciar o backup, o sistema abre um *Dialog* com fundo preto e texto verde, mostrando o progresso detalhado do Firebird (tabelas sendo salvas) para tranquilizar o usuário.

### 📂 Tratamento de Erros e Caminhos
* **Retry Logic (Lógica de Retentativa):** Se o backup falhar na pasta padrão (devido a permissões ou erro de caminho), o sistema captura o erro e abre automaticamente um `DirectoryChooser` pedindo para o usuário indicar onde salvar.
* **Compatibilidade Docker:** Ajuste na string de conexão JDBC para diferenciar o caminho local (host) do caminho interno do container (`/firebird/data/`).

---

## 4. Melhorias de Interface e UX

### 🎨 Estilização
* **Cores de Seleção:** Corrigido o bug onde a seleção da lista deixava o texto invisível. Agora, itens selecionados ficam com fundo **Azul Claro (#BBDEFB)** e texto **Preto**.
* **Ícones de Status:** Adicionados ícones visuais para cada estado do script:
    * ✅ Sucesso (Verde)
    * ❌ Erro (Vermelho)
    * ⏳ Rodando (Azul)
    * ⚠️ Aviso (Laranja)
    * ⏭ Pulado (Roxo)

### 🎮 Novos Controles
* **Botão "Apenas Backup":** Permite rodar a rotina de segurança sem executar nenhum script SQL.
* **Botão "Parar":** Implementada flag `volatile boolean stopRequested`. O usuário pode interromper o processo a qualquer momento. O sistema termina a tarefa atual (para não corromper dados) e para antes da próxima.

---

## 5. Pendências Conhecidas (To-Do)

* **Ambiente Docker/Mac:** O mapeamento de volumes entre o Host (Mac) e o Container (Firebird) impede que o serviço de backup (que roda dentro do container) escreva diretamente em pastas do usuário (`/Users/...`) sem configuração extra de volumes.
    * *Solução Provisória:* O sistema tenta, falha, e o usuário deve selecionar uma pasta válida ou aceitar o erro por enquanto.
    * *Solução Futura:* Mapear uma pasta compartilhada fixa no `docker-compose` ou salvar em `/tmp` do container e copiar o arquivo para fora via stream.

---

### Comandos Git Sugeridos

```bash
git add .
git commit -m "refactor: migração JavaFX completa, console de backup visual e correção de paths"
git push origin feature/backup-system