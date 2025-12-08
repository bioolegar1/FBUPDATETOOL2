




CREATE TABLE GER080_CARGO (
    CODIGO_EMPRESA   INTEIRO NOT NULL /* INTEIRO = INTEGER */,
    CODIGO_FILIAL    INTEIRO NOT NULL /* INTEIRO = INTEGER */,
    CODIGO_CARGO     INTEIRO NOT NULL /* INTEIRO = INTEGER */,
    DESCRICAO_CARGO  VC100 /* VC100 = VARCHAR(100) */
);



ALTER TABLE GER080_CARGO ADD CONSTRAINT GER080_PK01 PRIMARY KEY (CODIGO_EMPRESA, CODIGO_FILIAL, CODIGO_CARGO);


/******************************************************************************/
/****                             Foreign keys                             ****/
/******************************************************************************/

ALTER TABLE GER080_CARGO ADD CONSTRAINT GER080_FK01 FOREIGN KEY (CODIGO_EMPRESA, CODIGO_FILIAL) REFERENCES GER021A_FILIAL (CODIGO_EMPRESA, CODIGO_FILIAL);

