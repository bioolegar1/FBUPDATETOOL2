



CREATE TABLE SGQ003_OBJQUALIDADE (
    CODIGO_EMPRESA          INTEIRO NOT NULL /* INTEIRO = INTEGER */,
    CODIGO_FILIAL           INTEIRO NOT NULL /* INTEIRO = INTEGER */,
    CODIGO_OBJQUALIDADE     INTEIRO NOT NULL /* INTEIRO = INTEGER */,
    INFO_OBJQUALIDADE       VC400 /* VC400 = VARCHAR(400) */,
    DATA_OBJQUALIDADE       DATA /* DATA = DATE */,
    USRINCLUI_OBJQUALIDADE  VC025 /* VC025 = VARCHAR(25) */,
    SITUACAO_OBJQUALIDADE   VC025 /* VC025 = VARCHAR(25) */
);




/******************************************************************************/
/****                             Primary keys                             ****/
/******************************************************************************/

ALTER TABLE SGQ003_OBJQUALIDADE ADD CONSTRAINT SGQ003_PK PRIMARY KEY (CODIGO_EMPRESA, CODIGO_FILIAL, CODIGO_OBJQUALIDADE);


/******************************************************************************/
/****                             Foreign keys                             ****/
/******************************************************************************/

ALTER TABLE SGQ003_OBJQUALIDADE ADD CONSTRAINT SGQ003_FK01 FOREIGN KEY (CODIGO_EMPRESA, CODIGO_FILIAL) REFERENCES GER021A_FILIAL (CODIGO_EMPRESA, CODIGO_FILIAL);
