



CREATE TABLE SGQ001F_ACAOEXECUTADA (
    CODIGO_ACAOEXECUTADA     INTEIRO NOT NULL /* INTEIRO = INTEGER */,
    CODIGO_ACAO              INTEIRO NOT NULL /* INTEIRO = INTEGER */,
    CODIGO_EMPRESA           INTEIRO NOT NULL /* INTEIRO = INTEGER */,
    CODIGO_FILIAL            INTEIRO NOT NULL /* INTEIRO = INTEGER */,
    DESCRICAO_ACAOEXECUTADA  VC450 /* VC450 = VARCHAR(450) */
);




/******************************************************************************/
/****                             Primary keys                             ****/
/******************************************************************************/

ALTER TABLE SGQ001F_ACAOEXECUTADA ADD CONSTRAINT SGQ001F_PK PRIMARY KEY (CODIGO_ACAOEXECUTADA, CODIGO_ACAO, CODIGO_EMPRESA, CODIGO_FILIAL);


/******************************************************************************/
/****                             Foreign keys                             ****/
/******************************************************************************/

ALTER TABLE SGQ001F_ACAOEXECUTADA ADD CONSTRAINT SGQ001F_FK01 FOREIGN KEY (CODIGO_ACAO, CODIGO_EMPRESA, CODIGO_FILIAL) REFERENCES SGQ001_ACAO (CODIGO_ACAO, CODIGO_EMPRESA, CODIGO_FILIAL);
