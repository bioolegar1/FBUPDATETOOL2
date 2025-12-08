



CREATE TABLE SGQ004_CONFIGAVALIACAOFORNEC (
    CODIGO_CONFIGAVALIACAOFORNEC    INTEIRO NOT NULL /* INTEIRO = INTEGER */,
    CODIGO_EMPRESA                  INTEIRO NOT NULL /* INTEIRO = INTEGER */,
    CODIGO_FILIAL                   INTEIRO NOT NULL /* INTEIRO = INTEGER */,
    DATA_CONFIGAVALIACAOFORNEC      DATA /* DATA = DATE */,
    SITUACAO_CONFIGAVALIACAOFORNEC  VC025 /* VC025 = VARCHAR(25) */,
    REPROVA_CONFIGAVALIACAOFORNEC   VC060 /* VC060 = VARCHAR(60) */,
    MEDIA_CONFIGAVALIACAOFORNEC     MOEDA /* MOEDA = NUMERIC(15,4) */
);




/******************************************************************************/
/****                             Primary keys                             ****/
/******************************************************************************/

ALTER TABLE SGQ004_CONFIGAVALIACAOFORNEC ADD CONSTRAINT SGQ004_PK PRIMARY KEY (CODIGO_CONFIGAVALIACAOFORNEC, CODIGO_EMPRESA, CODIGO_FILIAL);


/******************************************************************************/
/****                             Foreign keys                             ****/
/******************************************************************************/

ALTER TABLE SGQ004_CONFIGAVALIACAOFORNEC ADD CONSTRAINT SGQ004_FK01 FOREIGN KEY (CODIGO_EMPRESA, CODIGO_FILIAL) REFERENCES GER021A_FILIAL (CODIGO_EMPRESA, CODIGO_FILIAL);

