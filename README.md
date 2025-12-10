# Firebird UpdateTool

Ferramenta automatizada para execução de scripts SQL em bancos de dados Firebird. Desenvolvida em Java (Swing) com foco em portabilidade e facilidade de uso para o usuário final.

## Funcionalidades

* **Interface Gráfica Moderna:** Utiliza FlatLaf para um visual limpo e responsivo.
* **Feedback Visual:** Ícones de status (Sucesso, Erro, Alerta, Ignorado, Pulado) para cada script.
* **Execução Resiliente:** Tratamento inteligente de erros e validação de arquivos vazios ou mal formatados.
* **Logs Detalhados:**
    * Visualização em tempo real na interface.
    * Gravação automática de arquivo `.txt` na pasta `C:\SoluçõesPillar\log_script_att`.
    * Abertura automática do log ao finalizar o processo.
* **Portabilidade:** Executável Windows (`.exe`) com Java (JRE 17) embutido, eliminando a necessidade de instalação prévia do Java no cliente.

## Estrutura do Projeto

* **src/main/java:** Código fonte da aplicação.
* **src/main/resources:** Ícones, configurações de log (logback.xml) e assets.
* **pom.xml:** Gerenciamento de dependências (Maven) e plugins de build.

## Como Compilar (Desenvolvimento)

### Pré-requisitos
* JDK 17 ou superior.
* Maven.

### Gerar o JAR (Fat Jar)
Para gerar o arquivo `.jar` contendo todas as dependências:

1.  Abra o terminal na raiz do projeto.
2.  Execute o comando:
    ```bash
    mvn clean package
    ```
3.  O arquivo gerado estará na pasta `target/` com o nome `fb-update-tool-2.0.0-SNAPSHOT.jar`.

## Como Gerar o Executável (.exe) para Distribuição

Utilizamos o **Launch4j** para criar um wrapper `.exe` que utiliza um JRE local.

1.  Crie uma pasta para distribuição (ex: `InstaladorFB`).
2.  Copie o arquivo `.jar` gerado para esta pasta e renomeie para `UpdateTool.jar`.
3.  Extraia um **JRE 17 Portátil** (Windows x64) para dentro desta pasta e renomeie a pasta extraída para `jre`.
4.  Abra o Launch4j e configure:
    * **Output:** `InstaladorFB\Atualizador.exe`
    * **Jar:** `InstaladorFB\UpdateTool.jar`
    * **Bundled JRE path:** `jre`
5.  Gere o arquivo `.exe`.

## Estrutura de Entrega ao Cliente

Para que o sistema funcione, a pasta entregue ao cliente deve conter obrigatoriamente estes três itens:

```text
Pasta_do_Sistema/
├── Atualizador.exe    (O executável criado)
├── UpdateTool.jar     (O código Java compilado)
└── jre/               (A pasta do Java portátil com as subpastas bin/ e lib/)
