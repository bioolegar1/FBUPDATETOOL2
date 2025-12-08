



CREATE TABLE SGQ001D_ABRANGENCIA (
    CODIGO_ABRANGENCIA     INTEIRO NOT NULL /* INTEIRO = INTEGER */,
    CODIGO_ACAO         INTEIRO NOT NULL /* INTEIRO = INTEGER */,
    CODIGO_EMPRESA      INTEIRO NOT NULL  /* INTEIRO = INTEGER */,
    CODIGO_FILIAL       INTEIRO NOT NULL  /* INTEIRO = INTEGER */,
    DESCRICAO_ABRANGENCIA  VC450 /* VC450 = VARCHAR(450) */
);




/******************************************************************************/
/****                             Primary keys                             ****/
/******************************************************************************/

ALTER TABLE SGQ001D_ABRANGENCIA ADD CONSTRAINT SGQ001D_PK PRIMARY KEY (CODIGO_ABRANGENCIA, CODIGO_ACAO, CODIGO_EMPRESA, CODIGO_FILIAL);


/******************************************************************************/
/****                             Foreign keys                             ****/
/******************************************************************************/

ALTER TABLE SGQ001D_ABRANGENCIA ADD CONSTRAINT SGQ001D_FK01 FOREIGN KEY (CODIGO_ACAO, CODIGO_EMPRESA, CODIGO_FILIAL) REFERENCES SGQ001_ACAO (CODIGO_ACAO, CODIGO_EMPRESA, CODIGO_FILIAL);


/******************************************************************************/
/****                              Privileges                              ****/
/******************************************************************************/
