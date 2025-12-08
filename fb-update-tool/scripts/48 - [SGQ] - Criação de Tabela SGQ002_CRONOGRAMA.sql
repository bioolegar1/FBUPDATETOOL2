


CREATE TABLE SGQ001G_ANALISECRITICA (
    CODIGO_ANALISECRITICA     INTEIRO NOT NULL /* INTEIRO = INTEGER */,
    CODIGO_ACAO               INTEIRO NOT NULL /* INTEIRO = INTEGER */,
    CODIGO_EMPRESA            INTEIRO NOT NULL /* INTEIRO = INTEGER */,
    CODIGO_FILIAL             INTEIRO NOT NULL /* INTEIRO = INTEGER */,
    CODIGO_PESSOA             INTEIRO /* INTEIRO = INTEGER */,
    DESCRICAO_ANALISECRITICA  VC450 /* VC450 = VARCHAR(450) */
);




/******************************************************************************/
/****                             Primary keys                             ****/
/******************************************************************************/

ALTER TABLE SGQ001G_ANALISECRITICA ADD CONSTRAINT SGQ001G_PK PRIMARY KEY (CODIGO_ANALISECRITICA, CODIGO_ACAO, CODIGO_EMPRESA, CODIGO_FILIAL);


/******************************************************************************/
/****                             Foreign keys                             ****/
/******************************************************************************/

ALTER TABLE SGQ001G_ANALISECRITICA ADD CONSTRAINT SGQ001G_FK01 FOREIGN KEY (CODIGO_ACAO, CODIGO_EMPRESA, CODIGO_FILIAL) REFERENCES SGQ001_ACAO (CODIGO_ACAO, CODIGO_EMPRESA, CODIGO_FILIAL);
ALTER TABLE SGQ001G_ANALISECRITICA ADD CONSTRAINT SGQ001G_FK02 FOREIGN KEY (CODIGO_PESSOA) REFERENCES GER020_PESSOA (CODIGO_PESSOA);
