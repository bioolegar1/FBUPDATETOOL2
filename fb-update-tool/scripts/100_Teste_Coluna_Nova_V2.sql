-- Este script tenta recriar a tabela GER080_CARGO
-- Mas adiciona uma coluna que NÃO existe lá
CREATE TABLE GER080_CARGO (
    CODIGO_CARGO INT, -- Coluna que já deve existir
    DESCRICAO VARCHAR(100), -- Coluna que já deve existir
    TESTE_SMART_MERGE VARCHAR(50) -- <--- O JAVA TEM QUE CRIAR SÓ ISSO AQUI
);