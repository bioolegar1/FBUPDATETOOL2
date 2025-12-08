


CREATE OR ALTER TRIGGER CPR002A_TAU FOR CPR002A_MATERIAL
ACTIVE AFTER UPDATE POSITION 0
AS
DECLARE VARIABLE codigo_fornecedor inteiro;
DECLARE VARIABLE solicita inteiro;
BEGIN
   FOR SELECT codigo_fornecedor
       FROM cpr002b_fornecedor
       WHERE codigo_empresa    = NEW.codigo_empresa    AND
             codigo_filial     = NEW.codigo_filial     AND
             numero_cotacao    = NEW.numero_cotacao
       INTO :codigo_fornecedor
   DO
       UPDATE cpr002ba_material
         SET
             FINALIDADE_MATERIAL = NEW.finalidade_material,

            qtde_material = CASE WHEN qtde_material IS NULL THEN NEW.qtde_material
                                 ELSE qtde_material + ( CASE WHEN ( OLD.qtde_material IS NULL) THEN NEW.qtde_material
                                                               ELSE NEW.qtde_material - OLD.qtde_material
                                                        END)
                            END
      WHERE codigo_empresa    = NEW.codigo_empresa  AND
            codigo_filial     = NEW.codigo_filial   AND
            numero_cotacao    = NEW.numero_cotacao  AND
            codigo_fornecedor = :codigo_fornecedor  AND
            codigo_material   = NEW.codigo_material;


   IF (( NOT NEW.qtdeexcedente_material IS NULL) AND (NEW.qtdeexcedente_material > 0)) THEN
   BEGIN
       FOR SELECT numero_solicitacao
           FROM cpr002aa_matsolicitado
           WHERE codigo_empresa = NEW.codigo_empresa AND
                 codigo_filial  = NEW.codigo_filial  AND
                 numero_cotacao = NEW.numero_cotacao AND
                 codigo_material= NEW.codigo_material
           INTO :solicita
        DO
           UPDATE cpr001a_material 
           SET qtdeexcedente_material = qtdeexcedente_material + NEW.qtdeexcedente_material
           WHERE codigo_empresa = NEW.codigo_empresa AND
                 codigo_filial  = NEW.codigo_filial  AND
                 numero_solicitacao = :solicita AND
                 codigo_material= NEW.codigo_material;
   END
END