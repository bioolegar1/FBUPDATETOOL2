



CREATE TABLE GER020O_PIX (
    NUMERO_SEQUENCIA  INTEIRO NOT NULL /* INTEIRO = INTEGER */,
    CODIGO_PESSOA     INTEIRO NOT NULL /* INTEIRO = INTEGER */,
    TIPOCHAVE_PIX     VC040 /* VC040 = VARCHAR(60) */,
    INFO_PIX          VC200 /* VC200 = VARCHAR(200) */,
    SITUACAO_PIX      VC025 /* VC025 = VARCHAR(25) */,
    USRINCLUI_PIX     VC025 /* VC025 = VARCHAR(25) */,
    USRALTERA_PIX     VC025 /* VC025 = VARCHAR(25) */
);



ALTER TABLE GER020O_PIX ADD CONSTRAINT GER020O_PK PRIMARY KEY (NUMERO_SEQUENCIA, CODIGO_PESSOA);


/******************************************************************************/
/****                             Foreign keys                             ****/
/******************************************************************************/

ALTER TABLE GER020O_PIX ADD CONSTRAINT GER020O_FK01 FOREIGN KEY (CODIGO_PESSOA) REFERENCES GER020_PESSOA (CODIGO_PESSOA);
