


/* View: VWCPR004_PENDENCIACOMPRA */
CREATE OR ALTER VIEW VWCPR004_PENDENCIACOMPRA(
    CODIGO_EMPRESA,
    CODIGO_FILIAL,
    NUMERO_SOLICITACAO,
    DATA_SOLICITACAO,
    CODIGO_COMPRADOR,
    CODIGO_SOLICITANTE,
    CODIGO_OBRA,
    CTRLPLNOBRA_SOLICITACAO,
    CODIGO_DEPARTAMENTO,
    CODIGO_MATERIAL,
    NUMEROITEM_PLANEJAMENTO,
    SITUACAO_MATERIAL,
    PRIORIDADE_MATERIAL,
    STAUTORIZACAO_MATERIAL,
    DTLIMITE_MATERIAL,
    OBSERVACAO_MATERIAL,
    OBSCOMPRADOR_MATERIAL,
    SIGLA_UNDMEDIDA,
    CODIGO_MARCA,
    QTDESOLICITADA_MATERIAL,
    QTDECANCELADA_MATERIAL,
    QTDENAUTORIZ_MATERIAL,
    QTDEAUTORIZADA_MATERIAL,
    SALDOAUTORIZACAO_MATERIAL,
    QTDEEXCEDENTE_MATERIAL,
    QTDECOTANDO_MATERIAL,
    QTDEEXECEDCOTANDO_MATERIAL,
    QTDECOMPRANDO_MATERIAL,
    QTDEEXCEDCOMPRANDO_MATERIAL,
    QTDEEMCOMPRA_MATERIAL,
    QTDEATENDIDA_MATERIAL,
    SALDOCOMPRA_MATERIAL,
    DTAUTORIZACAO_MATERIAL,
    FINALIDADE_MATERIAL,
    USRSOLICITANTE_SOLICITACAO)
AS
select CODIGO_EMPRESA,           CODIGO_FILIAL,              NUMERO_SOLICITACAO,
       DATA_SOLICITACAO,         CODIGO_COMPRADOR,           CODIGO_SOLICITANTE,
       CODIGO_OBRA,              ctrlplnobra_solicitacao,    CODIGO_DEPARTAMENTO,
       CODIGO_MATERIAL,          numeroitem_planejamento,    SITUACAO_MATERIAL,
       prioridade_material,      STAUTORIZACAO_MATERIAL,     dtlimite_material,
       observacao_material,      obscomprador_material,      sigla_undmedida,
       codigo_marca,             QTDESOLICITADA_MATERIAL,    QTDECANCELADA_MATERIAL,
       QTDENAUTORIZ_MATERIAL,    QTDEAUTORIZADA_MATERIAL,    SALDOAUTORIZACAO_MATERIAL,
       QTDEEXCEDENTE_MATERIAL,

       SUM( COALESCE( qtdecotando_material, 0.0000)) qtdecotando_material,
       SUM( COALESCE( qtdeexcedente_material, 0.0000)) qtdeexecedcotando_material,
       SUM( COALESCE( qtdecomprando_material, 0.0000)) qtdecomprando_material,
       sum( coalesce( qtdeexcedcomprando_material, 0.0000)) qtdeexcedcomprando_material,

       SUM(( COALESCE( qtdecotando_material, 0.0000)   -
             COALESCE( qtdeexcedente_material, 0.0000) +
             COALESCE( qtdecomprando_material, 0.0000))) qtdeemcompra_material,

       SUM( COALESCE( qtdeatendida_material, 0.0000)) qtdeatendida_material,

       ( coalesce( qtdeautorizada_material, 0.0000) -
         SUM( COALESCE( qtdecotando_material, 0.0000)   -
              COALESCE( qtdeexcedente_material, 0.0000) +
              COALESCE( qtdecomprando_material, 0.0000) +
              COALESCE( qtdeatendida_material, 0.0000)) -
              COALESCE( QTDECANCELADA_MATERIAL, 0.0000)    ) saldocompra_material,

       dtautorizacao_material  ,
       FINALIDADE_MATERIAL  ,
       USRSOLICITANTE_SOLICITACAO


from ( select cpr001.codigo_empresa,
              cpr001.codigo_filial,
              cpr001.numero_solicitacao,

              CPR001.DATA_SOLICITACAO,
              CPR001.CODIGO_COMPRADOR,
              CPR001.CODIGO_SOLICITANTE,
              cpr001.codigo_obra,
              cpr001.ctrlplnobra_solicitacao,
              cpr001.CODIGO_DEPARTAMENTO,
              cpr001a.CODIGO_MATERIAL,
              cpr001ab.numeroitem_planejamento,
              cpr001a.SITUACAO_MATERIAL,
              cpr001a.prioridade_material,
              cpr001a.dtlimite_material,
              cpr001a.observacao_material,
              cpr001a.obscomprador_material,
              cpr001a.sigla_undmedida,
              cpr001a.codigo_marca,
              cpr001a.dtautorizacao_material,
              cpr001a.finalidade_material,

              cpr001.USRSOLICITANTE_SOLICITACAO,

              cpr002aa.numero_cotacao,
              cpr002.situacao_cotacao,
              cpr003.numero_ordemcompra,
              cpr003.situacao_ordemcompra,
              cpr005.numero_ordemservico,
              cpr005.situacao_ordemservico,

              case when coalesce( cpr001ab.numeroitem_planejamento, 0) > 0
                 then coalesce( cpr001ab.quantidade_origem, 0.0000)
                 else case when (( case when (( case when eng004.inictrlplan_obra = 'Dt Especificada'
                                                   then eng004.dtinictrlplan_obra
                                                   else eng004.dtinicio_obra
                                                end) <= cpr001.data_solicitacao) and
                                               ( eng004.ctrlplanejamento_obra = 'Sim')
                                       then 'Sim'
                                       else 'Não'
                                    end) = 'Sim')
                         then 0.0000
                         else coalesce( cpr001a.qtdesolicitada_material, 0.0000)
                      end
              end qtdesolicitada_material,

              case when coalesce( cpr001ab.numeroitem_planejamento, 0) > 0
                 then coalesce( cpr001ab.qtdecancelada_origem, 0.0000)
                 else case when (( case when (( case when eng004.inictrlplan_obra = 'Dt Especificada'
                                                   then eng004.dtinictrlplan_obra
                                                   else eng004.dtinicio_obra
                                                end) <= cpr001.data_solicitacao) and
                                               ( eng004.ctrlplanejamento_obra = 'Sim')
                                       then 'Sim'
                                       else 'Não'
                                    end) = 'Sim')
                         then 0.0000
                         else coalesce( cpr001a.qtdecancelada_material, 0.0000)
                      end
              end qtdecancelada_material,

              case when coalesce( cpr001ab.numeroitem_planejamento, 0) > 0
                 then coalesce( cpr001ab.qtdenegada_origem, 0.0000)
                 else case when (( case when (( case when eng004.inictrlplan_obra = 'Dt Especificada'
                                                   then eng004.dtinictrlplan_obra
                                                   else eng004.dtinicio_obra
                                                end) <= cpr001.data_solicitacao) and
                                               ( eng004.ctrlplanejamento_obra = 'Sim')
                                       then 'Sim'
                                       else 'Não'
                                    end) = 'Sim')
                         then 0.0000
                         else coalesce( cpr001a.qtdenautoriz_material, 0.0000)
                      end
              end qtdenautoriz_material,

              case when coalesce( cpr001ab.numeroitem_planejamento, 0) > 0
                 then coalesce( cpr001ab.qtdeautorizada_origem, 0.0000)
                 else case when (( case when (( case when eng004.inictrlplan_obra = 'Dt Especificada'
                                                   then eng004.dtinictrlplan_obra
                                                   else eng004.dtinicio_obra
                                                end) <= cpr001.data_solicitacao) and
                                               ( eng004.ctrlplanejamento_obra = 'Sim')
                                       then 'Sim'
                                       else 'Não'
                                    end) = 'Sim')
                         then 0.0000
                         else coalesce( cpr001a.qtdeautorizada_material, 0.0000)
                      end
              end qtdeautorizada_material,

              case when (  coalesce(cpr001a.qtdesolicitada_material, 0) = coalesce(cpr001a.qtdecancelada_material, 0)  )
              then 0.00
                   else
                         case when coalesce( cpr001ab.numeroitem_planejamento, 0) > 0
                             then coalesce( cpr001ab.quantidade_origem, 0.0000) -
                                  coalesce( cpr001ab.qtdeautorizada_origem, 0.0000) -
                                  coalesce( cpr001ab.qtdecancelada_origem, 0.0000) -
                                  coalesce( cpr001ab.qtdenegada_origem, 0.0000)
                             else case when (( case when (( case when eng004.inictrlplan_obra = 'Dt Especificada'
                                                               then eng004.dtinictrlplan_obra
                                                               else eng004.dtinicio_obra
                                                             end) <= cpr001.data_solicitacao) and
                                                           ( eng004.ctrlplanejamento_obra = 'Sim')
                                                   then 'Sim'
                                                   else 'Não'
                                                end) = 'Sim')
                                     then 0.0000
                                     else
            
                                          cast( ( coalesce(cpr001a.qtdesolicitada_material, 0)   -
                                               coalesce(cpr001a.qtdeautorizada_material, 0)
                                             )                                                -
                                             (  coalesce(cpr001a.qtdecancelada_material, 0)   +
                                                coalesce(cpr001a.qtdenautoriz_material, 0)) as numeric( 15, 4))
                                  end
                          end
               end


               saldoautorizacao_material,

              case when coalesce( cpr001ab.numeroitem_planejamento, 0) > 0
                  then coalesce( cpr001ab.qtdeexecedente_origem, 0.0000)
                  else case when (( case when (( case when eng004.inictrlplan_obra = 'Dt Especificada'
                                                    then eng004.dtinictrlplan_obra
                                                    else eng004.dtinicio_obra
                                                  end) <= cpr001.data_solicitacao) and
                                                ( eng004.ctrlplanejamento_obra = 'Sim')
                                        then 'Sim'
                                        else 'Não'
                                     end) = 'Sim')
                          then 0.0000
                          else coalesce( cpr001a.qtdeexcedente_material, 0.0000)
                       end
              end qtdeexcedente_material,

              case when coalesce( cpr001ab.numeroitem_planejamento, 0) > 0
                 then case
                         when ( cpr001a.situacao_material = 'Cancelado') then 'Indisponível'

                         when ( cpr001a.situacao_material = 'Bloqueado') and
                              (( coalesce( cpr001ab.quantidade_origem, 0.0000) -
                                 coalesce( cpr001ab.qtdecancelada_origem, 0.0000) -
                                 coalesce( cpr001ab.qtdenegada_origem, 0.0000) -
                                 coalesce( cpr001ab.qtdeautorizada_origem, 0.0000)) > 0)  then 'Indisponível'

                         when ( coalesce( cpr001ab.quantidade_origem, 0.0000) -
                                coalesce( cpr001ab.qtdecancelada_origem, 0.0000) -
                                coalesce( cpr001ab.qtdenegada_origem, 0.0000)) =

                              ( coalesce( cpr001ab.quantidade_origem, 0.0000) -
                                coalesce( cpr001ab.qtdecancelada_origem, 0.0000) -
                                coalesce( cpr001ab.qtdenegada_origem, 0.0000) -
                                coalesce( cpr001ab.qtdeautorizada_origem, 0.0000)) then 'Pendente'

                         when ( coalesce( cpr001ab.quantidade_origem, 0.0000) -
                                coalesce( cpr001ab.qtdecancelada_origem, 0.0000)) =
                              ( coalesce( cpr001ab.qtdenegada_origem, 0.0000)) then 'Não Autorizado'

                         when ( coalesce( cpr001ab.quantidade_origem, 0.0000) -
                                coalesce( cpr001ab.qtdecancelada_origem, 0.0000) -
                                coalesce( cpr001ab.qtdenegada_origem, 0.0000) -
                                coalesce( cpr001ab.qtdeautorizada_origem, 0.0000))= 0 then 'Autorizado'
                         else 'Parcial'
                      end
                  else case when (( case when (( case when eng004.inictrlplan_obra = 'Dt Especificada'
                                                    then eng004.dtinictrlplan_obra
                                                    else eng004.dtinicio_obra
                                                 end) <= cpr001.data_solicitacao) and
                                               ( eng004.ctrlplanejamento_obra = 'Sim')
                                       then 'Sim'
                                       else 'Não'
                                    end) = 'Sim')
                          then 'Pendente'
                          else case
                                 when ( cpr001a.situacao_material = 'Cancelado') then 'Indisponível'

                                 when ( cpr001a.situacao_material = 'Bloqueado') and
                                      (( coalesce( cpr001a.qtdesolicitada_material, 0.0000) -
                                         coalesce( cpr001a.qtdecancelada_material, 0.0000) -
                                         coalesce( cpr001a.qtdenautoriz_material, 0.0000) -
                                         coalesce( cpr001a.qtdeautorizada_material, 0.0000)) > 0)  then 'Indisponível'

                                 when ( coalesce( cpr001a.qtdesolicitada_material, 0.0000) -
                                        coalesce( cpr001a.qtdecancelada_material, 0.0000) -
                                        coalesce( cpr001a.qtdenautoriz_material, 0.0000)) =

                                      ( coalesce( cpr001a.qtdesolicitada_material, 0.0000) -
                                        coalesce( cpr001a.qtdecancelada_material, 0.0000) -
                                        coalesce( cpr001a.qtdenautoriz_material, 0.0000) -
                                        coalesce( cpr001a.qtdeautorizada_material, 0.0000)) then 'Pedente'

                                 when ( coalesce( cpr001a.qtdesolicitada_material, 0.0000) -
                                        coalesce( cpr001a.qtdecancelada_material, 0.0000)) =
                                      ( coalesce( cpr001a.qtdenautoriz_material, 0.0000)) then 'Não Autorizado'

                                 when ( coalesce( cpr001a.qtdesolicitada_material, 0.0000) -
                                        coalesce( cpr001a.qtdecancelada_material, 0.0000) -
                                        coalesce( cpr001a.qtdenautoriz_material, 0.0000) -
                                        coalesce( cpr001a.qtdeautorizada_material, 0.0000))= 0 then 'Autorizado'
                                 else 'Parcial'
                              end
                       end
              end stautorizacao_material,

              case when (coalesce( cpr002ab.quantidade_origem, 0.0000) > 0.0000 ) and (cpr003.situacao_ordemcompra <> 'Finalizada')
                  then coalesce( cpr002ab.quantidade_origem, 0.0000)

                  when
                     (coalesce( cpr002ab.quantidade_origem, 0.0000) <= 0.0000 ) and (cpr003.situacao_ordemcompra <> 'Finalizada')
                       then
                         coalesce( cpr002aa.qtde_material, 0.0000)
              end qtdecotando_material,

              coalesce( cpr002a.qtdeexcedente_material, 0.0000) qtdeexcedcomprando_material,

              /* ********* */
              ( case when cpr003.situacao_ordemcompra <> 'Finalizada'
                   then case when coalesce( cpr003ab.quantidade_origem, 0.0000) > 0.0000
                           then coalesce( cpr003ab.quantidade_origem, 0.0000)
                           else coalesce( cpr003aa.qtde_matsolicitado, 0.0000)
                         end
                   else 0.0000
                end) +

              ( case when cpr005.situacao_ordemservico <> 'Finalizada'
                   then case when coalesce( cpr005ab.quantidade_origem, 0.0000) > 0.0000
                           then coalesce( cpr005ab.quantidade_origem, 0.0000)
                           else coalesce( cpr005aa.qtde_matsolicitado, 0.0000)
                         end
                   else 0.0000
                end) qtdecomprando_material,
              /* ********* */

              /* ********* */
              (( case when cpr003.situacao_ordemcompra = 'Finalizada'
                   then case when coalesce( cpr003ab.quantidade_origem, 0.0000) > 0.0000
                           then coalesce( cpr003ab.quantidade_origem, 0.0000)
                           else coalesce( cpr003aa.qtde_matsolicitado, 0.0000)
                         end
                   else 0.0000
                end) +

              ( case when cpr005.situacao_ordemservico = 'Finalizada'
                   then case when coalesce( cpr005ab.quantidade_origem, 0.0000) > 0.0000
                           then coalesce( cpr005ab.quantidade_origem, 0.0000)
                           else coalesce( cpr005aa.qtde_matsolicitado, 0.0000)
                         end
                   else 0.0000
                end)) qtdeatendida_material
              /* ********* */

       from cpr001_solicitacao cpr001
          left join eng004_obra eng004 on
             eng004.codigo_empresa = cpr001.codigo_empresa and
             eng004.codigo_filial  = cpr001.codigo_filial  and
             eng004.codigo_obra    = cpr001.codigo_obra
          inner join cpr001a_material cpr001a on
             cpr001a.codigo_empresa      = cpr001.codigo_empresa     and
             cpr001a.codigo_filial       = cpr001.codigo_filial      and
             cpr001a.numero_solicitacao  = cpr001.numero_solicitacao and
             cpr001a.situacao_material  <> 'Cancelado'
          inner join ger038_material ger038 on
             ger038.codigo_material = cpr001a.codigo_material
          left join cpr001ab_origem cpr001ab on
             cpr001ab.codigo_empresa     = cpr001a.codigo_empresa     and
             cpr001ab.codigo_filial      = cpr001a.codigo_filial      and
             cpr001ab.numero_solicitacao = cpr001a.numero_solicitacao and
             cpr001ab.codigo_material    = cpr001a.codigo_material

          left join cpr002aa_matsolicitado cpr002aa on
             cpr002aa.codigo_empresa     = cpr001a.codigo_empresa     and
             cpr002aa.codigo_filial      = cpr001a.codigo_filial      and
             cpr002aa.numero_solicitacao = cpr001a.numero_solicitacao and
             cpr002aa.codigo_material    = cpr001a.codigo_material
          left join cpr002_cotacao cpr002 on
             cpr002.codigo_empresa = cpr002aa.codigo_empresa and
             cpr002.codigo_filial  = cpr002aa.codigo_filial  and
             cpr002.numero_cotacao = cpr002aa.numero_cotacao and
            -- cpr002.situacao_cotacao not in ( 'Finalizada', 'Cancelada')
             cpr002.situacao_cotacao = 'Pendente'
          left join cpr002a_material cpr002a on
             cpr002a.codigo_empresa          = cpr001ab.codigo_empresa          and
             cpr002a.codigo_filial           = cpr001ab.codigo_filial           and
             cpr002a.numero_cotacao          = cpr002aa.numero_cotacao          and
             cpr002a.codigo_material         = cpr001ab.codigo_material
          left join cpr002ab_origem cpr002ab on
             cpr002ab.codigo_empresa          = cpr001ab.codigo_empresa          and
             cpr002ab.codigo_filial           = cpr001ab.codigo_filial           and
             cpr002ab.numero_cotacao          = cpr002aa.numero_cotacao          and
             cpr002ab.codigo_material         = cpr001ab.codigo_material         and
             cpr002ab.codigo_obra             = cpr001ab.codigo_obra             and
             cpr002ab.numeroitem_planejamento = cpr001ab.numeroitem_planejamento


          left join cpr003aa_matsolicitado cpr003aa on
             cpr003aa.codigo_empresa     = cpr001a.codigo_empresa     and
             cpr003aa.codigo_filial      = cpr001a.codigo_filial      and
             cpr003aa.numero_solicitacao = cpr001a.numero_solicitacao and
             cpr003aa.codigo_material    = cpr001a.codigo_material
          left join cpr003_ordemcompra cpr003 on
             cpr003.codigo_empresa     = cpr003aa.codigo_empresa     and
             cpr003.codigo_filial      = cpr003aa.codigo_filial      and
             cpr003.numero_ordemcompra = cpr003aa.numero_ordemcompra and
             cpr003.situacao_ordemcompra <> 'Cancelada'
          left join cpr003ab_origem cpr003ab on
             cpr003ab.codigo_empresa          = cpr001ab.codigo_empresa          and
             cpr003ab.codigo_filial           = cpr001ab.codigo_filial           and
             cpr003ab.numero_ordemcompra      = cpr003.numero_ordemcompra      and
             cpr003ab.codigo_material         = cpr001ab.codigo_material         and
             cpr003ab.codigo_obra             = cpr001ab.codigo_obra             and
             cpr003ab.numeroitem_planejamento = cpr001ab.numeroitem_planejamento


          left join cpr005aa_matsolicitado cpr005aa on
             cpr005aa.codigo_empresa     = cpr001a.codigo_empresa     and
             cpr005aa.codigo_filial      = cpr001a.codigo_filial      and
             cpr005aa.numero_solicitacao = cpr001a.numero_solicitacao and
             cpr005aa.codigo_material    = cpr001a.codigo_material
          left join cpr005_ordemservico cpr005 on
             cpr005.codigo_empresa         = cpr005aa.codigo_empresa      and
             cpr005.codigo_filial          = cpr005aa.codigo_filial       and
             cpr005.numero_ordemservico    = cpr005aa.numero_ordemservico and
             cpr005.situacao_ordemservico <> 'Cancelada'
          left join cpr005ab_origem cpr005ab on
             cpr005ab.codigo_empresa          = cpr001ab.codigo_empresa          and
             cpr005ab.codigo_filial           = cpr001ab.codigo_filial           and
             cpr005ab.numero_ordemservico     = cpr005aa.numero_ordemservico     and
             cpr005ab.codigo_material         = cpr001ab.codigo_material         and
             cpr005ab.codigo_obra             = cpr001ab.codigo_obra             and
             cpr005ab.numeroitem_planejamento = cpr001ab.numeroitem_planejamento

       where cpr001.situacao_solicitacao <> 'Cancelada' and

             (/* solicitacao sem processo de compra */
              (( cpr002.numero_cotacao is null) and
               (( cpr003.numero_ordemcompra is null) or
                ( cpr003aa.numero_ordemcompra is null))and
               (( cpr005.numero_ordemservico is null) or
                ( cpr005aa.numero_ordemservico is null))) or

              /* solicitacao sem cotacao e com ordem de compra */
              (( cpr002.numero_cotacao is null) and
               ( cpr003.numero_ordemcompra is not null) and
               ( cpr003.situacao_ordemcompra <> 'Cancelada')) or

              /* solicitacao com cotacao e com ordem de compra */
              (( cpr002.numero_cotacao is not null) and
               ( cpr003.numero_ordemcompra is not null) and
               ( cpr003.situacao_ordemcompra <> 'Cancelada')) or

              /* solicitacao com cotacao e sem ordem de compra */
              (( cpr002.numero_cotacao is not null) and
               ( cpr003.numero_ordemcompra is null)) or
              -- ( cpr003.situacao_ordemcompra <> 'Cancelada')) or

              /* solicitacao sem cotacao e com ordem de servico */
              (( cpr002.numero_cotacao is null) and
               ( cpr005.numero_ordemservico is not null) and
               ( cpr005.situacao_ordemservico <> 'Cancelada')) or

              /* solicitacao com cotacao e com ordem de servico */
              (( cpr002.numero_cotacao is not null) and
               ( cpr005.numero_ordemservico is not null) and
               ( cpr005.situacao_ordemservico <> 'Cancelada')) or

              /* solicitacao com cotacao e sem ordem de servico */
              (( cpr002.numero_cotacao is not null) and
               ( cpr005.numero_ordemservico is null) and
               ( cpr005.situacao_ordemservico <> 'Cancelada'))))

group by CODIGO_EMPRESA,           CODIGO_FILIAL,              NUMERO_SOLICITACAO,
         DATA_SOLICITACAO,         CODIGO_COMPRADOR,           CODIGO_SOLICITANTE,
         CODIGO_OBRA,              ctrlplnobra_solicitacao,    CODIGO_DEPARTAMENTO,
         CODIGO_MATERIAL,          numeroitem_planejamento,    SITUACAO_MATERIAL,
         prioridade_material,      STAUTORIZACAO_MATERIAL,     dtlimite_material,
         observacao_material,      obscomprador_material,      sigla_undmedida,
         codigo_marca,             QTDESOLICITADA_MATERIAL,    QTDECANCELADA_MATERIAL,
         QTDENAUTORIZ_MATERIAL,    QTDEAUTORIZADA_MATERIAL,    SALDOAUTORIZACAO_MATERIAL,
         QTDEEXCEDENTE_MATERIAL,   dtautorizacao_material,     FINALIDADE_MATERIAL ,USRSOLICITANTE_SOLICITACAO
;




/******************************************************************************/
/****                              Privileges                              ****/
/******************************************************************************/
