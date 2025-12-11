# Firebird UpdateTool - SoluçõesPillar

> **PROPRIEDADE CONFIDENCIAL:** Este software e sua documentação contêm informações proprietárias da SoluçõesPillar. O uso, cópia ou distribuição não autorizada é estritamente proibido.

Ferramenta corporativa automatizada para execução e auditoria de scripts SQL em bancos de dados Firebird. Desenvolvida em Java (Swing) com foco em portabilidade, resiliência e facilidade de uso para o usuário final.

---

## 📋 Informações Técnicas
* **Versão Atual:** 2.0.0-SNAPSHOT
* **Desenvolvedor Responsável:** BioOlegari
* **Tecnologia:** Java 17 (Swing/AWT) + FlatLaf
* **Compatibilidade:** Windows 10/11 e Windows Server (x64)
* **Banco de Dados Alvo:** Firebird (2.5 / 3.0 / 4.0)

---

## 🚀 Funcionalidades Principais

* **Interface Gráfica Moderna:** Utiliza FlatLaf para um visual limpo e responsivo.
* **Feedback Visual:** Ícones de status (Sucesso, Erro, Alerta, Ignorado, Pulado) para cada script.
* **Execução Resiliente:** Tratamento inteligente de erros e validação de arquivos vazios ou mal formatados.
* **Logs Detalhados:**
    * Visualização em tempo real na interface.
    * Gravação automática de arquivo `.txt` na pasta `C:\SoluçõesPillar\log_script_att`.
    * Abertura automática do log ao finalizar o processo.
* **Portabilidade:** Executável Windows (`.exe`) com Java (JRE 17) embutido, eliminando a necessidade de instalação prévia do Java no cliente.

---

## 📂 Estrutura do Projeto (Código Fonte)

* **src/main/java:** Código fonte da aplicação.
* **src/main/resources:** Ícones, configurações de log (logback.xml) e assets.
* **pom.xml:** Gerenciamento de dependências (Maven) e plugins de build.

---

## ⚙️ Configuração e Instalação (Ambiente Cliente)

### Estrutura de Entrega
Para que o sistema funcione, a pasta entregue ao cliente deve conter obrigatoriamente estes três itens:

```text
Pasta_do_Sistema/
├── Atualizador.exe    (O executável criado)
├── UpdateTool.jar     (O código Java compilado)
└── jre/               (A pasta do Java portátil com as subpastas bin/ e lib/)
```

### Configuração de Banco de Dados
A ferramenta busca automaticamente o caminho do banco de dados conforme o arquivo de configuração local (ex: `firebird.conf` ou parâmetros de inicialização).

> **Nota:** Certifique-se de que o serviço do Firebird esteja rodando na porta padrão (3050) antes de iniciar o atualizador.

---

## 🛠️ Guia de Desenvolvimento

### Pré-requisitos
* **JDK:** 17 LTS ou superior.
* **Gerenciador de Build:** Maven.

### Como Compilar (Gerar o JAR)
Para gerar o arquivo `.jar` contendo todas as dependências:

1. Abra o terminal na raiz do projeto.
2. Execute o comando:
   ```bash
   mvn clean package
   ```
3. O arquivo gerado estará na pasta `target/` com o nome `fb-update-tool-2.0.0-SNAPSHOT.jar`.

### Como Gerar o Executável (.exe) para Distribuição
Utilizamos o **Launch4j** para criar um wrapper `.exe` que utiliza um JRE local.

1. Crie uma pasta para distribuição (ex: `InstaladorFB`).
2. Copie o arquivo `.jar` gerado para esta pasta e renomeie para `UpdateTool.jar`.
3. Extraia um **JRE 17 Portátil** (Windows x64) para dentro desta pasta e renomeie a pasta extraída para `jre`.
4. Abra o Launch4j e configure:
    * **Output:** `InstaladorFB\Atualizador.exe`
    * **Jar:** `InstaladorFB\UpdateTool.jar`
    * **Bundled JRE path:** `jre`
5. Gere o arquivo `.exe`.

---

## 📅 Histórico de Versões (Changelog)

Manter este registro atualizado a cada modificação crítica.

* **v2.0.0 (Atual)**
    * Migração completa para Java 17.
    * Implementação da nova UI com FlatLaf.
    * Adição de logs detalhados em arquivo físico.
* **v1.5.0**
    * Correção no encoding de arquivos ANSI/UTF-8.
* **v1.0.0**
    * Lançamento inicial (Versão Legada).

---

## 📞 Suporte e Manutenção

Em caso de falhas críticas ou necessidade de refatoração, contatar o responsável técnico:

* **Responsável:** BioOlegari
* **Departamento:** Desenvolvimento / TI
* **Email Corporativo:** bioolegari@gmail.com
* **WhatsApp Corporativo:** (62) 9 8289-2166

---

## ⚖️ Licença e Termos de Uso

**Copyright © 2025 SoluçõesPillar. Todos os direitos reservados.**

Este programa é um software proprietário.

1. É proibida a engenharia reversa, descompilação ou desmontagem deste software.
2. A redistribuição deste software fora dos clientes da SoluçõesPillar constitui violação de propriedade intelectual.
3. Este software é fornecido "como está", garantindo as funcionalidades descritas para o ambiente homologado.
