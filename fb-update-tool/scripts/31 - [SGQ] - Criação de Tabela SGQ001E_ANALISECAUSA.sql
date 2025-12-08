



CREATE TABLE SGQ001E_ANALISECAUSA (
    CODIGO_ANALISECAUSA     INTEIRO NOT NULL /* INTEIRO = INTEGER */,
    CODIGO_ACAO             INTEIRO NOT NULL /* INTEIRO = INTEGER */,
    CODIGO_EMPRESA          INTEIRO NOT NULL /* INTEIRO = INTEGER */,
    CODIGO_FILIAL           INTEIRO NOT NULL /* INTEIRO = INTEGER */,
    DESCRICAO_ANALISECAUSA  VC450 /* VC450 = VARCHAR(450) */
);




/******************************************************************************/
/****                             Primary keys                             ****/
/******************************************************************************/

ALTER TABLE SGQ001E_ANALISECAUSA ADD CONSTRAINT SGQE001_PK PRIMARY KEY (CODIGO_ANALISECAUSA, CODIGO_ACAO, CODIGO_EMPRESA, CODIGO_FILIAL);


/******************************************************************************/
/****                             Foreign keys                             ****/
/******************************************************************************/

ALTER TABLE SGQ001E_ANALISECAUSA ADD CONSTRAINT SGQE001_FK FOREIGN KEY (CODIGO_ACAO, CODIGO_EMPRESA, CODIGO_FILIAL) REFERENCES SGQ001_ACAO (CODIGO_ACAO, CODIGO_EMPRESA, CODIGO_FILIAL);
