
CREATE TABLE SGQ001_ACAO (
    CODIGO_ACAO        INTEIRO NOT NULL /* INTEIRO = INTEGER */,
    CODIGO_EMPRESA     INTEIRO NOT NULL /* INTEIRO = INTEGER */,
    CODIGO_FILIAL      INTEIRO NOT NULL /* INTEIRO = INTEGER */,
    CODIGO_PESSOA      INTEIRO /* INTEIRO = INTEGER */,
    DATA_ACAO          DATA /* DATA = DATE */,
    TIPO_ACAO          VC040 /* VC040 = VARCHAR(60) */,
    SITUACAO_ACAO      VC025 /* VC025 = VARCHAR(25) */,
    DTCADASTRO_ACAO    DATA /* DATA = DATE */,
    USRINCLUI_ACAO     VC025 /* VC025 = VARCHAR(25) */,
    SETORORIGEM_ACAO   VC100 /* VC100 = VARCHAR(100) */,
    ASSUNTO_ACAO       VC100 /* VC100 = VARCHAR(100) */,
    EVENTO_ACAO        VC100 /* VC100 = VARCHAR(100) */,
    INFOPROBLENA_ACAO  VC450 /* VC450 = VARCHAR(450) */,
    CODIGO_OBRA        INTEIRO /* INTEIRO = INTEGER */
);




/******************************************************************************/
/****                             Primary keys                             ****/
/******************************************************************************/

ALTER TABLE SGQ001_ACAO ADD CONSTRAINT SGQ001_PK PRIMARY KEY (CODIGO_ACAO, CODIGO_EMPRESA, CODIGO_FILIAL);


/******************************************************************************/
/****                             Foreign keys                             ****/
/******************************************************************************/

ALTER TABLE SGQ001_ACAO ADD CONSTRAINT SGQ001_FK01 FOREIGN KEY (CODIGO_EMPRESA, CODIGO_FILIAL) REFERENCES GER021A_FILIAL (CODIGO_EMPRESA, CODIGO_FILIAL);
ALTER TABLE SGQ001_ACAO ADD CONSTRAINT SGQ001_FK02 FOREIGN KEY (CODIGO_PESSOA) REFERENCES GER020_PESSOA (CODIGO_PESSOA);
ALTER TABLE SGQ001_ACAO ADD CONSTRAINT SGQ001_FK03 FOREIGN KEY (CODIGO_EMPRESA, CODIGO_FILIAL, CODIGO_OBRA) REFERENCES ENG004_OBRA (CODIGO_EMPRESA, CODIGO_FILIAL, CODIGO_OBRA);
