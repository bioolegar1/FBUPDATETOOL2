



CREATE TABLE SGQ003A_ITEM (
    CODIGO_ITEM          INTEIRO NOT NULL /* INTEIRO = INTEGER */,
    CODIGO_OBJQUALIDADE  INTEIRO NOT NULL /* INTEIRO = INTEGER */,
    CODIGO_EMPRESA       INTEIRO NOT NULL /* INTEIRO = INTEGER */,
    CODIGO_FILIAL        INTEIRO NOT NULL /* INTEIRO = INTEGER */,
    OBJETIVO_ITEM        VC200 /* VC200 = VARCHAR(200) */,
    INDICE_ITEM          VC200 /* VC200 = VARCHAR(200) */,
    FREQUENCIA_ITEM      VC200 /* VC200 = VARCHAR(200) */,
    META_ITEM            VC200 /* VC200 = VARCHAR(200) */,
    SITUACAO_ITEM        VC025 /* VC025 = VARCHAR(25) */
);




/******************************************************************************/
/****                             Primary keys                             ****/
/******************************************************************************/

ALTER TABLE SGQ003A_ITEM ADD CONSTRAINT SGQ003A_PK PRIMARY KEY (CODIGO_ITEM, CODIGO_OBJQUALIDADE, CODIGO_EMPRESA, CODIGO_FILIAL);


/******************************************************************************/
/****                             Foreign keys                             ****/
/******************************************************************************/

ALTER TABLE SGQ003A_ITEM ADD CONSTRAINT SGQ003A_FK01 FOREIGN KEY (CODIGO_OBJQUALIDADE, CODIGO_EMPRESA, CODIGO_FILIAL) REFERENCES SGQ003_OBJQUALIDADE (CODIGO_EMPRESA, CODIGO_FILIAL, CODIGO_OBJQUALIDADE);
