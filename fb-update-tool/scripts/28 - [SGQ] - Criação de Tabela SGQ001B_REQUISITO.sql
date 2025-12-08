



CREATE TABLE SGQ001B_REQUISITO (
    CODIGO_REQUISITO     INTEIRO NOT NULL /* INTEIRO = INTEGER */,
    CODIGO_ACAO          INTEIRO NOT NULL /* INTEIRO = INTEGER */,
    CODIGO_EMPRESA       INTEIRO NOT NULL /* INTEIRO = INTEGER */,
    CODIGO_FILIAL        INTEIRO NOT NULL/* INTEIRO = INTEGER */,
    DESCRICAO_REQUISITO VC450 /* VC450 = VARCHAR(450) */
);





/******************************************************************************/
/****                             Primary keys                             ****/
/******************************************************************************/

ALTER TABLE SGQ001B_REQUISITO ADD CONSTRAINT SGQ001B_PK PRIMARY KEY (CODIGO_REQUISITO, CODIGO_ACAO, CODIGO_EMPRESA, CODIGO_FILIAL);


/******************************************************************************/
/****                             Foreign keys                             ****/
/******************************************************************************/

ALTER TABLE SGQ001B_REQUISITO ADD CONSTRAINT SGQ001B_FK FOREIGN KEY (CODIGO_ACAO, CODIGO_EMPRESA, CODIGO_FILIAL) REFERENCES SGQ001_ACAO (CODIGO_ACAO, CODIGO_EMPRESA, CODIGO_FILIAL);

