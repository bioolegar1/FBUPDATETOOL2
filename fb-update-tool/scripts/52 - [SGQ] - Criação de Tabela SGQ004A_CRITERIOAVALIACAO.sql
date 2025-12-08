


CREATE TABLE SGQ004A_CRITERIOAVALIACAO (
    CODIGO_CRITERIOAVALIACAO      INTEIRO NOT NULL /* INTEIRO = INTEGER */,
    CODIGO_CONFIGAVALIACAOFORNEC  INTEIRO NOT NULL /* INTEIRO = INTEGER */,
    CODIGO_EMPRESA                INTEIRO NOT NULL /* INTEIRO = INTEGER */,
    CODIGO_FILIAL                 INTEIRO NOT NULL /* INTEIRO = INTEGER */,
    INFO_CRITERIOAVALIACAO        VC100 /* VC100 = VARCHAR(100) */,
    USRINCLUI_CRITERIOAVALIACAO   VC025 /* VC025 = VARCHAR(25) */,
    DESCRICAO_CRITERIOAVALIACAO   VC100 /* VC100 = VARCHAR(100) */
);




/******************************************************************************/
/****                             Primary keys                             ****/
/******************************************************************************/

ALTER TABLE SGQ004A_CRITERIOAVALIACAO ADD CONSTRAINT SGQ004A_PK PRIMARY KEY (CODIGO_CRITERIOAVALIACAO, CODIGO_CONFIGAVALIACAOFORNEC, CODIGO_EMPRESA, CODIGO_FILIAL);


/******************************************************************************/
/****                             Foreign keys                             ****/
/******************************************************************************/

ALTER TABLE SGQ004A_CRITERIOAVALIACAO ADD CONSTRAINT SGQ004A_FK01 FOREIGN KEY (CODIGO_CONFIGAVALIACAOFORNEC, CODIGO_EMPRESA, CODIGO_FILIAL) REFERENCES SGQ004_CONFIGAVALIACAOFORNEC (CODIGO_CONFIGAVALIACAOFORNEC, CODIGO_EMPRESA, CODIGO_FILIAL);
