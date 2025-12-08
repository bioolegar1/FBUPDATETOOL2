



CREATE TABLE GER081_FUNCAO (
    CODIGO_EMPRESA    INTEIRO NOT NULL /* INTEIRO = INTEGER */,
    CODIGO_FILIAL     INTEIRO NOT NULL /* INTEIRO = INTEGER */,
    CODIGO_FUNCAO     INTEIRO NOT NULL /* INTEIRO = INTEGER */,
    DESCRICAO_FUNCAO  VC100 /* VC100 = VARCHAR(100) */,
    SITUACAO_FUNCAO   VC020 /* VC020 = VARCHAR(20) */
);



ALTER TABLE GER081_FUNCAO ADD CONSTRAINT GER081_PK PRIMARY KEY (CODIGO_EMPRESA, CODIGO_FILIAL, CODIGO_FUNCAO);


/******************************************************************************/
/****                             Foreign keys                             ****/
/******************************************************************************/

ALTER TABLE GER081_FUNCAO ADD CONSTRAINT GER081_FK01 FOREIGN KEY (CODIGO_EMPRESA, CODIGO_FILIAL) REFERENCES GER021A_FILIAL (CODIGO_EMPRESA, CODIGO_FILIAL);

