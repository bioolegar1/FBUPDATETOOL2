


/* View: VWFIN018_COMPROMISSO */
CREATE OR ALTER VIEW VWFIN018_COMPROMISSO(
    CODIGO_EMPRESA,
    CODIGO_FILIAL,
    NUMERO_COMPROMISSO,
    CODIGO_ROTINA,
    NOME_ROTINA,
    TIPO_COMPROMISSO,
    ORIGEM_COMPROMISSO,
    NUMERO_ADTVPARCELA,
    NUMERO_ADITIVO,
    PARCELA_CONTRATO,
    PARCELA_ADTVPARCELA,
    PARCELA_CREDITO,
    TIPO_CREDITO,
    PARCELACARTAO_COMPROMISSO,
    ROTINA_ORIGEM,
    NOMEROTINA_ORIGEM,
    CODIGO_PESSOA,
    RAZAOSOCIAL_PESSOA,
    NOMEFANTASIA_PESSOA,
    DOCUMENTO_PESSOA,
    TELEFONE_PESSOA,
    EMAIL_PESSOA,
    ENDERECO_PESSOA,
    NUMERO_PESSOA,
    BAIRRO_PESSOA,
    CEP_PESSOA,
    NOMECIDADE_PESSOA,
    SIGLAESTADO_PESSOA,
    NOMETITULARCC_PESSOA,
    DOCTITULARCC_PESSOA,
    NUMERO_BANCO,
    NOME_BANCOPESSOA,
    CODIGOAGENCIA_PESSOA,
    NUMEROCC_PESSOA,
    DVAGENCIA_PESSOA,
    DVNUMEROCC_PESSOA,
    DOCUMENTO_COMPROMISSO,
    NIVELALOCADO_COMPROMISSO,
    ALOCADO_COMPROMISSO,
    DTLCTO_COMPROMISSO,
    DTEMISSAO_COMPROMISSO,
    DTVENCIMENTO_COMPROMISSO,
    DTPREVISAO_COMPROMISSO,
    CODIGO_OBRA,
    NOME_OBRA,
    SIGLA_OBRA,
    NUMEROITEM_PLANEJAMENTO,
    REFMASCARA_PLANEJAMENTO,
    REFERENCIA_PLANEJAMENTO,
    NOME_PLANEJAMENTO,
    CODIGO_TIPODOCUMENTO,
    NOME_TIPODOCUMENTO,
    TIPOSERVICO_TIPODOCUMENTO,
    FORMALANCAMENTO_TIPODOCUMENTO,
    CODIGO_PORTADOR,
    NOME_PORTADOR,
    CODIGO_DEPARTAMENTO,
    NOME_DEPARTAMENTO,
    CODIGO_PLANOCONTA,
    DESCRICAO_PLANOCONTA,
    REFMASCARA_PLANOCONTA,
    CODIGO_DIVISAO,
    SIGLA_DIVISAO,
    CODIGO_UNIDADE,
    SIGLA_TIPOUNIDADE,
    DESCRICAO_UNIDADE,
    NUMERO_UNIDADE,
    AREACONSTRUIDA_UNIDADE,
    AREACOMUM_UNIDADE,
    AREATERRENO_UNIDADE,
    FRACAOIDEAL_UNIDADE,
    SITUACAO_UNIDADE,
    HISTORICO_COMPROMISSO,
    REFERENCIA_COMPROMISSO,
    CALCULADOATE_COMPROMISSO,
    CODIGO_CONTA,
    DESCRICAO_CONTA,
    NUMCC_CONTA,
    DVCC_CONTA,
    CODAGENCIA_CONTA,
    DVAGENCIA_CONTA,
    NOME_BANCO,
    EMITEBOLETO_CONTA,
    NUMBANCO_CONTA,
    CONVENIO_CONTA,
    DVCONVENIO_CONTA,
    CONVENIOPGTO_CONTA,
    DVCONVENIOPGTO_CONTA,
    CODCEDENTE_CONTA,
    DVCODCEDENTE_CONTA,
    CARTEIRA_CONTA,
    VARIACAOCARTEIRA_CONTA,
    SISCOBRANCA_CONTA,
    VLRCONTRATO_COMPROMISSO,
    VLRORIGINAL_COMPROMISSO,
    VLRCORRECAO_COMPROMISSO,
    VLRCORRPOSVCTO_COMPROMISSO,
    VLRJUROCOMP_COMPROMISSO,
    VLRJCOMPPVCTO_COMPROMISSO,
    VLRDESCONTO_COMPROMISSO,
    VLRINSS_COMPROMISSO,
    VLRCSLL_COMPROMISSO,
    VLRIRPF_COMPROMISSO,
    VLRISS_COMPROMISSO,
    VLRPIS_COMPROMISSO,
    VLRCOFINS_COMPROMISSO,
    VLRIOF_COMPROMISSO,
    VLROUTROS_COMPROMISSO,
    TARIFABOLETO_COMPROMISSO,
    VLRFINAL_COMPROMISSO,
    DTBAIXA_COMPROMISSO,
    USRBAIXA_COMPROMISSO,
    NOME_USUARIOBAIXA,
    DTDIABAIXA_COMPROMISSO,
    JUROBAIXA_COMPROMISSO,
    MULTABAIXA_COMPROMISSO,
    DESCBAIXA_COMPROMISSO,
    VLRACRESCIMO_COMPROMISSO,
    VLRBAIXA_COMPROMISSO,
    VLRRESIDUO_COMPROMISSO,
    ZERARESIDUO_COMPROMISSO,
    TIPO_COBRANCA,
    NUMERO_COBRANCA,
    NUMERO_BOLETO,
    NOSSONUMERO_BOLETO,
    SEUNUMERO_BOLETO,
    DTVCTO_BOLETO,
    TARIFA_BOLETO,
    SITUACAO_BOLETO,
    VALOR_BOLETO,
    REMESSAGERADA_BOLETO,
    CONTA_BOLETO,
    CODAGENCIA_BOLETO,
    DVAGENCIA_BOLETO,
    NUMCC_BOLETO,
    DVCC_BOLETO,
    TPORIGEM_COBRANCA,
    LINHADIGITAVEL_BOLETO,
    CODIGOBARRA_BOLETO,
    DTCRIACAO_REMESSA,
    NUMERO_REMESSA,
    CONTA_REMESSA,
    CODAGENCIA_CONTAREMESSA,
    DVAGENCIA_CONTAREMESSA,
    NUMCC_CONTAREMESSA,
    DVCC_CONTAREMESSA,
    SITUACAO_REMESSA,
    USRCONFIRMACAO_REMESSA,
    DTCONFIRMACAO_REMESSA,
    OCORRENCIA_HISTORICO,
    TIPOOCORRENCIA_HISTORICO,
    DESCOCORRENCIA_HISTORICO,
    DATA_HISTORICO,
    TEMCHEQUE_COMPROMISSO,
    CODIGO_CHEQUE,
    DTEMISSAO_CHEQUE,
    NUMERO_CHEQUE,
    PARADT_CHEQUE,
    SITUACAO_CHEQUE,
    EMITEBOLETO_CONTRATO,
    SISCORRECAO_CONTRATO,
    INDICE1_MOEDA,
    INDICE2_MOEDA,
    MESANTID1_CONTRATO,
    MESANTID2_CONTRATO,
    EMITEBOLETO_CONTRATO1,
    PERIODBOLETO_CONTRATO,
    TPENVIOBOLETO_CONTRATO,
    BLOQUEADOBOLETO_CONTRATO,
    USRBLOQBOLETO_CONTRATO,
    DTBLOQBOLETO_CONTRATO,
    BLOQCOBRANCA_CONTRATO,
    USRBLOQCOBRANCA_CONTRATO,
    DTBLOQCOBRANCA_CONTRATO,
    USRINCLUIU_COMPROMISSO,
    NOME_USUARIOINCLUIU,
    USRALTEROU_COMPROMISSO,
    NOME_USUARIOALTEROU,
    DTLIBERACAO_COMPROMISSO,
    USRLIBERACAO_COMPROMISSO,
    NOME_USUARIOLIBERACAO,
    STLIBERACAO_COMPROMISSO,
    DTCONFIRMACAO_COMPROMISSO,
    HRCONFIRMACAO_COMPROMISSO,
    USRCONFIRMACAO_COMPROMISSO,
    NOME_USUARIOCONFIRMACAO,
    STCONFIRMACAO_COMPROMISSO,
    DTCONCILIACAO_COMPROMISSO,
    USRCONCILIACAO_COMPROMISSO,
    CONCILIADO_COMPROMISSO,
    SITUACAO_COMPROMISSO,
    TEMRATEIO_COMPROMISSO,
    VINCULO_COMPROMISSO,
    ROTINACANCELA_COMPROMISSO,
    OBSERVACAO_COMPROMISSO,
    CODIGO_BEM,
    NOME_BEM,
    NUMEROFROTA_BEM,
    PLACAVEICULO_BEM,
    JUROCTRL_ADITIVO,
    CORRECAO_ADITIVO,
    NOMEARQUIVO_COMPROMISSO,
    NOMEARQUIVO2_COMPROMISSO,
    BAIXA_ROTINA,
    MANTEMVINCULO_BOLETO,
    CODIGO_CONTABOLETO,
    STOPERACAO_BOLETO,
    TXMULTA_COMPROMISSO,
    INADIMPLENCIA_ADITIVO,
    HRBAIXA_COMPROMISSO,
    USRCANCELOU_COMPROMISSO,
    DTCANCELAMENTO_COMPROMISSO,
    TEMBOLETO_COMPROMISSO,
    HRLIBERACAO_COMPROMISSO,
    DTCORRECAO_COMPROMISSO,
    DTCORRECAOVCTO_COMPROMISSO,
    DTCORRECAOBX_COMPROMISSO,
    DTAUTRESIDUO_COMPROMISSO,
    USRAUTRESIDUO_COMPROMISSO,
    VLRJINAD_COMPROMISSO,
    CODIGO_FASE,
    CODIGO_SERVICO,
    TXJURO_COMPROMISSO,
    TXPRORATA_COMPROMISSO,
    PARCELA_COMPROMISSO,
    MESREFERENCIA_COMPROMISSO,
    BXPARCIAL_COMPROMISSO,
    OBSERVACAO2_COMPROMISSO)
AS
Select fin009.codigo_empresa,
       fin009.codigo_filial,
       fin009.numero_compromisso,
       fin009.codigo_rotina,
       sis001a.nome_rotina,
       fin009.tipo_compromisso,
       fin009.origem_compromisso,
       fin009.numero_adtvparcela,
       fin009.numero_aditivo,
       fin009.parcela_contrato,
       fin009.parcela_adtvparcela,
       fin009.parcela_credito,

       Case when vde002l_1.tipo_credito is null
          then vde002l_2.tipo_credito
          else vde002l_1.tipo_credito
       end tipo_credito,

       fin009.parcelacartao_compromisso,
       fin009.rotina_origem,
       sis001a_1.nome_rotina nomerotina_origem,

       fin009.codigo_pessoa,
       ger020.RAZAOSOCIAL_PESSOA,                                              
       ger020.nomefantasia_pessoa,
       ger020.documento_pessoa,

       ( Select first 1 numero_telefone
         from ger020d_telefone
         where situacao_telefone  = 'Ativo' and
               principal_telefone = 'Sim' and
               codigo_pessoa      = ger020.codigo_pessoa
         order by codigo_pessoa,
                  codigo_telefone,
                  case tipo_telefone
                     when 'Residencial'          then 1
                     when 'Empresarial'          then 2
                     when 'Profissional'         then 3
                     when 'Celular'              then 4
                     when 'Celular Profissional' then 5
                     when 'Recado'               then 6
                     when 'Celular Recado'       then 7
                     when 'Rádio'                then 8
                     when 'Rádio Profissional'   then 9
                     when 'Fax'                  then 10
                     when 'Fax Profissional'     then 11
                  end) telefone_pessoa,

       ( Select first 1 conta_email
         from ger020e_email
         where situacao_email  = 'Ativo' and
               principal_email = 'Sim' and
               codigo_pessoa      = ger020.codigo_pessoa
         order by codigo_pessoa,
                  codigo_email,
                  case tipo_email
                     when 'Pessoal'      then 1
                     when 'Profissional' then 2
                     when 'Contato'      then 3
                  end) email_pessoa,

       case when ger020a_1.chave_endereco is not null
          then ( coalesce( ger020a_1.logradouro_endereco,   '') || '' ||
                 coalesce( ger020a_1.complemento_endereco,  ''))
          else ( coalesce( ger020a_2.logradouro_endereco,   '') || '' ||
                 coalesce( ger020a_2.complemento_endereco,  ''))
       end endereco_pessoa,

       case when ger020a_1.chave_endereco is not null
          then ger020a_1.numero_endereco
          else ger020a_2.numero_endereco
       end numero_pessoa,

       case when ger020a_1.chave_endereco is not null
          then ger020a_1.bairro_endereco
          else ger020a_2.bairro_endereco
       end bairro_pessoa,

       case when ger020a_1.chave_endereco is not null
          then ger020a_1.cep_endereco
          else ger020a_2.cep_endereco
       end cep_pessoa,

       case when ger020a_1.chave_endereco is not null
          then ger003aa_1.nome_cidade
          else ger003aa_2.nome_cidade
       end nomecidade_pessoa,

       case when ger020a_1.chave_endereco is not null
          then ger003aa_1.sigla_estado
          else ger003aa_2.sigla_estado
       end siglaestado_pessoa,

       ger020.nometitularcc_pessoa,
       ger020.doctitularcc_pessoa,          
       ger020.NUMERO_BANCO,
       fin005_1.nome_banco nome_bancopessoa,
       ger020.codigoagencia_pessoa,
       ger020.numerocc_pessoa,              
       ger020.dvagencia_pessoa,
       ger020.dvnumerocc_pessoa,            

       fin009.documento_compromisso,
       fin009.nivelalocado_compromisso,
       fin009.alocado_compromisso,
       fin009.dtlcto_compromisso,
       fin009.dtemissao_compromisso,
       fin009.dtvencimento_compromisso,
       fin009.dtprevisao_compromisso,

       fin009.codigo_obra,
       eng004.nome_obra,
       eng004.sigla_obra,

       fin009.numeroitem_planejamento,
       vwpln002.refmascara_planejamento,                                    
       vwpln002.referencia_planejamento,
       vwpln002.nome_planejamento,

       fin009.codigo_tipodocumento,
       fin001.nome_tipodocumento,
       fin001.tiposervico_tipodocumento,
       fin001.formalancamento_tipodocumento, 

       fin009.codigo_portador,
       fin003.nome_portador,

       fin009.codigo_departamento,
       ger021aa.nome_departamento,                

       fin009.codigo_planoconta,
       fin004.descricao_planoconta,
       fin004.REFMASCARA_PLANOCONTA,

       fin009.codigo_divisao,
       (Select *                                                               
        from prdeng001_dvunidade ( eng005.codigo_empresa,                      
                                   eng005.codigo_filial,                       
                                   eng005.codigo_unidade, '')) sigla_divisao,

       fin009.codigo_unidade,
       eng001.sigla_tipounidade,
       eng005.descricao_unidade,
       eng005.numero_unidade,
       eng005.areaconstruida_unidade,
       eng005.areacomum_unidade,
       eng005.areaterreno_unidade,
       eng005.fracaoideal_unidade,
       eng005.situacao_unidade,

       fin009.historico_compromisso,
       fin009.referencia_compromisso,
       fin009.calculadoate_compromisso,

       fin009.codigo_conta,
       fin006.descricao_conta,                                              
       fin006.NUMCC_CONTA,
       fin006.DVCC_CONTA,
       fin006.CODAGENCIA_CONTA,
       fin006.DVAGENCIA_CONTA,
       fin005.nome_banco,
       fin006.emiteboleto_conta,
       fin006.numbanco_conta,
       fin006.convenio_conta,
       fin006.dvconvenio_conta,
       fin006.conveniopgto_conta,
       fin006.dvconveniopgto_conta,
       fin006.codcedente_conta,
       fin006.dvcodcedente_conta,
       fin006.carteira_conta,
       fin006.variacaocarteira_conta,
       fin006.siscobranca_conta,

       fin009.vlrcontrato_compromisso,
       fin009.vlroriginal_compromisso,
       fin009.vlrcorrecao_compromisso,
       fin009.vlrcorrposvcto_compromisso,
       fin009.vlrjurocomp_compromisso,
       fin009.vlrjcomppvcto_compromisso,
       fin009.vlrdesconto_compromisso,
       fin009.vlrinss_compromisso,
       fin009.vlrcsll_compromisso,
       fin009.vlrirpf_compromisso,
       fin009.vlriss_compromisso,
       fin009.vlrpis_compromisso,
       fin009.vlrcofins_compromisso,
       fin009.vlriof_compromisso,
       fin009.vlroutros_compromisso,
       fin009.tarifaboleto_compromisso,
       fin009.vlrfinal_compromisso,

       fin009.dtbaixa_compromisso,
       fin009.usrbaixa_compromisso,
       sis005_3.nome_usuario nome_usuariobaixa,
       fin009.dtdiabaixa_compromisso,
       fin009.jurobaixa_compromisso,
       fin009.multabaixa_compromisso,
       fin009.descbaixa_compromisso,
       fin009.vlracrescimo_compromisso,
       fin009.vlrbaixa_compromisso,
       fin009.vlrresiduo_compromisso,
       fin009.zeraresiduo_compromisso,

       fin010a.tipo_cobranca,
       fin010a.numero_cobranca,
       fin010a.numero_boleto,
       fin010a.nossonumero_boleto,
       fin010a.seunumero_boleto,
       fin010a.dtvcto_boleto,
       fin010a.tarifa_boleto,
       fin010a.situacao_boleto,
       fin010a.valor_boleto,
       fin010a.remessagerada_boleto,
       fin010a.codigo_conta     conta_boleto,
       fin010a.CODAGENCIA_CONTA CODAGENCIA_BOLETO,
       fin010a.DVAGENCIA_CONTA  DVAGENCIA_BOLETO,
       fin010a.NUMCC_CONTA      NUMCC_BOLETO,
       fin010a.DVCC_CONTA       DVCC_BOLETO,
       fin010a.TPORIGEM_COBRANCA,
       fin010a.LINHADIGITAVEL_BOLETO,
       fin010a.CODIGOBARRA_BOLETO,

       vwfin009.dtcriacao_remessa,
       vwfin009.numero_remessa,
       vwfin009.codigo_conta conta_remessa,                                 
       vwfin009.codagencia_contaremessa,
       vwfin009.dvagencia_contaremessa,
       vwfin009.numcc_contaremessa,
       vwfin009.dvcc_contaremessa,
       vwfin009.situacao_remessa,
       vwfin009.USRCONFIRMACAO_REMESSA,
       vwfin009.DTCONFIRMACAO_REMESSA,

       vwfin010.ocorrencia_historico,
       vwfin010.tipoocorrencia_historico,
       vwfin010.descocorrencia_historico,
       vwfin010.data_historico,

       case when fin015.codigo_cheque is null
          then 'Não'
          else 'Sim'
       end temcheque_compromisso,

       fin015.codigo_cheque,
       fin015.dtemissao_cheque,
       fin015.numero_cheque,
       fin015.paradt_cheque,
       fin015.situacao_cheque,

       vde002.emiteboleto_contrato,
       vde002.siscorrecao_contrato,
       vde002.indice1_moeda,
       vde002.indice2_moeda,
       vde002.mesantid1_contrato,
       vde002.mesantid2_contrato,
       vde002.EMITEBOLETO_CONTRATO,
       vde002.PERIODBOLETO_CONTRATO,
       vde002.TPENVIOBOLETO_CONTRATO,
       vde002.BLOQUEADOBOLETO_CONTRATO,
       vde002.USRBLOQBOLETO_CONTRATO,
       vde002.DTBLOQBOLETO_CONTRATO,

       vde002.bloqcobranca_contrato,
       vde002.usrbloqcobranca_contrato,
       vde002.dtbloqcobranca_contrato,

       fin009.usrincluiu_compromisso,
       sis005_1.nome_usuario nome_usuarioincluiu,

       fin009.usralterou_compromisso,
       sis005_2.nome_usuario nome_usuarioalterou,

       fin009.dtliberacao_compromisso,
       fin009.usrliberacao_compromisso,
       sis005_4.nome_usuario nome_usuarioliberacao,
       fin009.stliberacao_compromisso,

       fin009.dtconfirmacao_compromisso,
       fin009.hrconfirmacao_compromisso,
       fin009.usrconfirmacao_compromisso,
       sis005_5.nome_usuario nome_usuarioconfirmacao,
       fin009.stconfirmacao_compromisso,

       fin009.dtconciliacao_compromisso,
       fin009.usrconciliacao_compromisso,
       fin009.conciliado_compromisso,

       fin009.situacao_compromisso,
       fin009.temrateio_compromisso,
       fin009.vinculo_compromisso,
       fin009.rotinacancela_compromisso,
       fin009.observacao_compromisso ,
       fin009.CODIGO_BEM ,

       ger041.nome_bem,
       ger041.numerofrota_bem,
       ger041.placaveiculo_bem,


       fin009.juroctrl_aditivo,
       fin009.correcao_aditivo,
       fin009.NOMEARQUIVO_COMPROMISSO,
       fin009.NOMEARQUIVO2_COMPROMISSO,
       fin009.baixa_rotina,
       FIN010A.mantemvinculo_boleto ,
       fin010a.codigo_contaboleto ,
       vwfin009.stoperacao_boleto ,

       fin009.TXMULTA_COMPROMISSO,
       fin009.INADIMPLENCIA_ADITIVO,
       fin009.HRBAIXA_COMPROMISSO ,
       fin009.USRCANCELOU_COMPROMISSO,
       fin009.DTCANCELAMENTO_COMPROMISSO,
       fin009.TEMBOLETO_COMPROMISSO,
       fin009.HRLIBERACAO_COMPROMISSO,
       fin009.DTCORRECAO_COMPROMISSO,
       fin009.DTCORRECAOVCTO_COMPROMISSO,
       fin009.DTCORRECAOBX_COMPROMISSO ,
       fin009.DTAUTRESIDUO_COMPROMISSO,
       fin009.USRAUTRESIDUO_COMPROMISSO,
       fin009.VLRJINAD_COMPROMISSO ,
       fin009.CODIGO_FASE ,
       fin009.CODIGO_SERVICO,
       FIN009.TXJURO_COMPROMISSO,
       FIN009.TXPRORATA_COMPROMISSO ,
       FIN009.PARCELA_COMPROMISSO ,
       FIN009.MESREFERENCIA_COMPROMISSO ,
       FIN009.bxparcial_compromisso ,
       fin009.OBSERVACAO2_COMPROMISSO


from fin009_compromisso fin009                                        

     inner join ger020_pessoa ger020 on
        ger020.codigo_pessoa = fin009.codigo_pessoa
     left join ger020a_endereco ger020a_1 on
        ger020a_1.codigo_pessoa      = ger020.codigo_pessoa and
        ger020a_1.situacao_endereco  = 'Ativo'              and
        ger020a_1.principal_endereco = 'Sim'
     left join ger003aa_cidade ger003aa_1 on
        ger003aa_1.identificador_cidade = ger020a_1.identificador_cidade

     left join ger020a_endereco ger020a_2 on
        ger020a_2.codigo_pessoa     = ger020.codigo_pessoa and
        ger020a_2.situacao_endereco = 'Ativo'              and
        ger020a_2.cobranca_endereco = 'Sim'
     left join ger003aa_cidade ger003aa_2 on
        ger003aa_2.identificador_cidade = ger020a_2.identificador_cidade

     left join fin001_tipodocumento fin001 on
        fin001.codigo_tipodocumento = fin009.codigo_tipodocumento     

     left join fin003_portador fin003 on                              
        fin003.codigo_portador = fin009.codigo_portador               

     left join fin004_planoconta fin004 on                            
        fin004.codigo_planoconta = fin009.codigo_planoconta   and     
        fin009.codigo_planoconta is not null                          

     left join sis001a_rotina sis001a  on                             
        sis001a.codigo_rotina = fin009.codigo_rotina                  

     left join sis001a_rotina sis001a_1  on
        sis001a_1.codigo_rotina = fin009.rotina_origem

     left join   sis005_usuario  sis005_1  on                         
        sis005_1.codigo_usuario = fin009.usrincluiu_compromisso       

     left join   sis005_usuario  sis005_2  on                          
        sis005_2.codigo_usuario = fin009.usralterou_compromisso        

     left join   sis005_usuario  sis005_3  on                          
        sis005_3.codigo_usuario = fin009.usrbaixa_compromisso          

     left join   sis005_usuario  sis005_4  on                          
        sis005_4.codigo_usuario = fin009.usrliberacao_compromisso      

     left join   sis005_usuario  sis005_5  on                          
        sis005_5.codigo_usuario = fin009.usrconfirmacao_compromisso    

     left join ger021aa_departamento ger021aa on
        ger021aa.codigo_empresa      = fin009.codigo_empresa      and 
        ger021aa.codigo_filial       = fin009.codigo_filial       and 
        ger021aa.codigo_departamento = fin009.codigo_departamento     

     left join eng004_obra eng004 on                                  
         eng004.codigo_empresa = fin009.codigo_empresa and            
         eng004.codigo_filial  = fin009.codigo_filial  and            
         eng004.codigo_obra    = fin009.codigo_obra                   

     left join vwpln002_planejamento vwpln002 on
        vwpln002.codigo_empresa          = fin009.codigo_empresa          and
        vwpln002.codigo_filial           = fin009.codigo_filial           and
        vwpln002.codigo_obra             = fin009.codigo_obra             and
        vwpln002.numeroitem_planejamento = fin009.numeroitem_planejamento and
        fin009.numeroitem_planejamento is not null

     left join eng005_unidade eng005 on                               
         eng005.codigo_empresa = fin009.codigo_empresa and            
         eng005.codigo_filial  = fin009.codigo_filial  and            
         eng005.codigo_unidade = fin009.codigo_unidade                

     left join eng001_tipounidade eng001 on
         eng001.sequencia_tipounidade = eng005.sequencia_tipounidade 

     left join fin006_conta fin006 on                                 
         fin006.codigo_empresa = fin009.codigo_empresa and            
         fin006.codigo_conta   = fin009.codigo_conta                  

     left join fin005_banco fin005 on                                 
         fin005.numero_banco = fin006.numbanco_conta                  

     left join vwfin008_boletocompromisso fin010a on
        fin010a.codigo_empresa     = fin009.codigo_empresa     and    
        fin010a.codigo_filial      = fin009.codigo_filial      and    
        fin010a.numero_compromisso = fin009.numero_compromisso and
        fin010a.tipo_cobranca      = fin009.tipo_cobranca      and
        fin010a.numero_cobranca    = fin009.numero_cobranca    and
        fin010a.numero_boleto      = fin009.numero_boleto

     left join vwfin009_remessaboleto vwfin009 on                     
        vwfin009.codigo_empresa  = fin010a.codigo_empresa  and        
        vwfin009.codigo_filial   = fin010a.codigo_filial   and        
        vwfin009.codigo_conta    = ( case when fin009.tipo_compromisso =  'Pagar'
                                       then fin009.codigo_conta       
                                       else fin010a.codigo_conta      
                                     end) and                         
        vwfin009.tipo_cobranca   = fin010a.tipo_cobranca   and        
        vwfin009.numero_cobranca = fin010a.numero_cobranca and        
        vwfin009.numero_boleto   = fin010a.numero_boleto              

     left join vwfin010_historicoboleto vwfin010 on                   
        vwfin010.codigo_empresa     = fin010a.codigo_empresa     and  
        vwfin010.codigo_filial      = fin010a.codigo_filial      and  
        vwfin010.tipo_cobranca      = fin010a.tipo_cobranca      and  
        vwfin010.numero_cobranca    = fin010a.numero_cobranca    and  
        vwfin010.numero_boleto      = fin010a.numero_boleto      and  
        vwfin010.numero_compromisso = fin010a.numero_compromisso

     left join fin015_cheque fin015 on                                
        fin015.codigo_empresa = fin009.codigo_empresa and             
        fin015.codigo_filial  = fin009.codigo_filial  and             
        fin015.codigo_cheque  = fin009.codigo_cheque                  

     left join vde002_contrato vde002 on                              
        vde002.codigo_empresa  = fin009.codigo_empresa     and        
        vde002.codigo_filial   = fin009.codigo_filial      and        
        vde002.numero_contrato = fin009.origem_compromisso and        
        fin009.rotina_origem   in (  'vde002',
                                     'vdeesp013',
                                     'vdeesp014',
                                     'vdeesp034')

     Left join fin005_banco fin005_1  on
        fin005_1.numero_banco = ger020.numero_banco

     left join vde002l_credito vde002l_1 on                                    
        vde002l_1.codigo_empresa    = fin009.codigo_empresa      and
        vde002l_1.codigo_filial     = fin009.codigo_filial       and
        vde002l_1.numero_contrato   = fin009.NUMERO_ADTVPARCELA  and
        vde002l_1.sequencia_credito = fin009.parcela_credito     and
        vde002l_1.tipo_credito      in ( 'Financiamento',
                                         'FGTS')
     left join vde002l_credito vde002l_2 on
        vde002l_2.codigo_empresa    = fin009.codigo_empresa      and
        vde002l_2.codigo_filial     = fin009.codigo_filial       and
        vde002l_2.numero_contrato   = fin009.origem_compromisso   and
        vde002l_2.sequencia_credito = fin009.parcela_credito     and
        vde002l_2.tipo_credito      in ( 'Financiamento',
                                         'FGTS')
      left join ger041_bem ger041 on
        ger041.codigo_bem     = fin009.codigo_bem


union all

Select fin017.codigo_empresa,
       fin017.codigo_filial,
       fin017.numero_lctobancario numero_compromisso,
       'fin017' codigo_rotina,
       sis001a.nome_rotina,

       Case when fin017.tipo_lctobancario = 'Débito'
          then 'Pagar'
          else 'Receber'
       end tipo_compromisso,

       fin017.numero_origem origem_compromisso,
       null numero_adtvparcela,
       null numero_aditivo,
       null parcela_contrato,
       null parcela_adtvparcela,
       null parcela_credito,
       null tipo_credito,
       null parcelacartao_compromisso,

       fin017.rotina_origem,
       sis001a_1.nome_rotina nomerotina_origem,

       null codigo_pessoa,
       fin005.nome_banco RAZAOSOCIAL_PESSOA,
       fin005.nome_banco nomefantasia_pessoa,
       null documento_pessoa,
       null telefone_pessoa,
       null email_pessoa,
       null endereco_pessoa,
       null numero_pessoa,
       null bairro_pessoa,
       null cep_pessoa,
       null nomecidade_pessoa,
       null siglaestado_pessoa,
       null nometitularcc_pessoa,
       null doctitularcc_pessoa,
       null NUMERO_BANCO,
       null nome_bancopessoa,
       null codigoagencia_pessoa,
       null numerocc_pessoa,
       null dvagencia_pessoa,
       null dvnumerocc_pessoa,

       fin017.documento_lctobancario documento_compromisso,
       fin017.nivelalocado_lctobancario nivelalocado_compromisso,
       null alocado_compromisso,
       fin017.data_lctobancario dtlcto_compromisso,
       fin017.data_lctobancario dtemissao_compromisso,
       fin017.data_lctobancario dtvencimento_compromisso,
       fin017.data_lctobancario dtprevisao_compromisso,

       fin017.codigo_obra,
       eng004.nome_obra,
       eng004.sigla_obra,

       fin017.numeroitem_planejamento,
       vwpln002.refmascara_planejamento,                                    
       vwpln002.referencia_planejamento,
       vwpln002.nome_planejamento,

       null codigo_tipodocumento,
       null nome_tipodocumento,
       null tiposervico_tipodocumento,
       null formalancamento_tipodocumento,

       null codigo_portador,
       null nome_portador,

       fin017.codigo_departamento,
       ger021aa.nome_departamento,                

       fin017.codigo_planoconta,
       fin004.descricao_planoconta,
       fin004.REFMASCARA_PLANOCONTA,

       null codigo_divisao,
       null sigla_divisao,

       null codigo_unidade,
       null sigla_tipounidade,
       null descricao_unidade,
       null numero_unidade,
       null areaconstruida_unidade,
       null areacomum_unidade,
       null areaterreno_unidade,
       null fracaoideal_unidade,
       null situacao_unidade,

       fin017.historico_lctobancario historico_compromisso,
       fin017.documento_lctobancario referencia_compromisso,
       null calculadoate_compromisso,

       fin017.codigo_conta,
       fin006.descricao_conta,                                              
       fin006.NUMCC_CONTA,
       fin006.DVCC_CONTA,
       fin006.CODAGENCIA_CONTA,
       fin006.DVAGENCIA_CONTA,
       fin005.nome_banco,
       fin006.emiteboleto_conta,
       fin006.numbanco_conta,
       fin006.convenio_conta,
       fin006.dvconvenio_conta,
       fin006.conveniopgto_conta,
       fin006.dvconveniopgto_conta,
       fin006.codcedente_conta,
       fin006.dvcodcedente_conta,
       fin006.carteira_conta,
       fin006.variacaocarteira_conta,
       fin006.siscobranca_conta,

       null vlrcontrato_compromisso,
       fin017.valor_lctobancario vlroriginal_compromisso,
       null vlrcorrecao_compromisso,
       null vlrcorrposvcto_compromisso,
       null vlrjurocomp_compromisso,
       null vlrjcomppvcto_compromisso,
       null vlrdesconto_compromisso,
       null vlrinss_compromisso,
       null vlrcsll_compromisso,
       null vlrirpf_compromisso,
       null vlriss_compromisso,
       null vlrpis_compromisso,
       null vlrcofins_compromisso,
       null vlriof_compromisso,
       null vlroutros_compromisso,
       null tarifaboleto_compromisso,
       fin017.valor_lctobancario vlrfinal_compromisso,

       Case when fin017.situacao_lctobancario = 'Ativo'
          then fin017.dtativacao_lctobancario
          else null
       end dtbaixa_compromisso,

       Case when fin017.situacao_lctobancario = 'Ativo'
          then fin017.usrativacao_lctobancario
          else null
       end usrbaixa_compromisso,
       sis005_3.nome_usuario nome_usuariobaixa,
       fin017.dtativacao_lctobancario dtdiabaixa_compromisso,
       null jurobaixa_compromisso,
       null multabaixa_compromisso,
       null descbaixa_compromisso,
       null vlracrescimo_compromisso,

       Case when fin017.situacao_lctobancario = 'Ativo'
          then fin017.valor_lctobancario
          else null
       end vlrbaixa_compromisso,
       null vlrresiduo_compromisso,
       'Não' zeraresiduo_compromisso,

       null tipo_cobranca,
       null numero_cobranca,
       null numero_boleto,
       null nossonumero_boleto,
       null seunumero_boleto,
       null dtvcto_boleto,
       null tarifa_boleto,
       null situacao_boleto,
       null valor_boleto,
       null remessagerada_boleto,
       null conta_boleto,
       null CODAGENCIA_BOLETO,
       null DVAGENCIA_BOLETO,
       null NUMCC_BOLETO,
       null DVCC_BOLETO,
       null TPORIGEM_COBRANCA,
       null LINHADIGITAVEL_BOLETO,
       null CODIGOBARRA_BOLETO,

       null dtcriacao_remessa,
       null numero_remessa,
       null conta_remessa,
       null codagencia_contaremessa,
       null dvagencia_contaremessa,
       null numcc_contaremessa,
       null dvcc_contaremessa,
       null situacao_remessa,
       null USRCONFIRMACAO_REMESSA,
       null DTCONFIRMACAO_REMESSA,

       null ocorrencia_historico,
       null tipoocorrencia_historico,

       null descocorrencia_historico,
       null data_historico,

       'Não' temcheque_compromisso,
       null codigo_cheque,
       null dtemissao_cheque,
       null numero_cheque,
       null paradt_cheque,
       null situacao_cheque,

       null emiteboleto_contrato,
       null siscorrecao_contrato,
       null indice1_moeda,
       null indice2_moeda,
       null mesantid1_contrato,
       null mesantid2_contrato,
       null EMITEBOLETO_CONTRATO,
       null PERIODBOLETO_CONTRATO,
       null TPENVIOBOLETO_CONTRATO,
       null BLOQUEADOBOLETO_CONTRATO,
       null USRBLOQBOLETO_CONTRATO,
       null DTBLOQBOLETO_CONTRATO,

       null bloqcobranca_contrato,
       null usrbloqcobranca_contrato,
       null dtbloqcobranca_contrato,

       null usrincluiu_compromisso,
       null nome_usuarioincluiu,

       null usralterou_compromisso,
       null nome_usuarioalterou,

       null dtliberacao_compromisso,
       null usrliberacao_compromisso,
       null nome_usuarioliberacao,
       null stliberacao_compromisso,

       null dtconfirmacao_compromisso,
       null hrconfirmacao_compromisso,
       null usrconfirmacao_compromisso,
       null nome_usuarioconfirmacao,
       null stconfirmacao_compromisso,

       null dtconciliacao_compromisso,
       null usrconciliacao_compromisso,
       null conciliado_compromisso,

       case when fin017.situacao_lctobancario = 'Inativo'
          then 'Aberto'
          else 'Baixado'
       end situacao_compromisso ,
       fin017.temrateio_lctobancario,
       null VINCULO_COMPROMISSO,
       null rotinacancela_compromisso,
       null observacao_compromisso,
       null codigo_bem ,
       null nome_bem ,

       null numerofrota_bem,
       null placaveiculo_bem,

       null juroctrl_aditivo,
       null correcao_aditivo,
       null NOMEARQUIVO_COMPROMISSO,
       null NOMEARQUIVO2_COMPROMISSO,
       null baixa_rotina,
       NULL MANTEMVINCULO_BOLETO,
       NULL codigo_contaboleto,
       null stoperacao_boleto,

       null  TXMULTA_COMPROMISSO,
       null INADIMPLENCIA_ADITIVO,
       null HRBAIXA_COMPROMISSO ,
       null USRCANCELOU_COMPROMISSO,
       null DTCANCELAMENTO_COMPROMISSO,
       null TEMBOLETO_COMPROMISSO,
       null HRLIBERACAO_COMPROMISSO,
       null DTCORRECAO_COMPROMISSO,
       null DTCORRECAOVCTO_COMPROMISSO,
       null DTCORRECAOBX_COMPROMISSO ,
       null DTAUTRESIDUO_COMPROMISSO,
       null USRAUTRESIDUO_COMPROMISSO,
       null VLRJINAD_COMPROMISSO ,
       NULL CODIGO_FASE ,
       NULL CODIGO_SERVICO ,
       NULL TXJURO_COMPROMISSO ,
       NULL TXPRORATA_COMPROMISSO,
       NULL PARCELA_COMPROMISSO ,
       NULL MESREFERENCIA_COMPROMISSO ,
       NULL bxparcial_compromisso    ,
       null OBSERVACAO2_COMPROMISSO

from fin017_lctobancario fin017

     left join fin004_planoconta fin004 on
        fin004.codigo_planoconta = fin017.codigo_planoconta   and
        fin017.codigo_planoconta is not null

     left join ger021aa_departamento ger021aa on
        ger021aa.codigo_empresa      = fin017.codigo_empresa      and
        ger021aa.codigo_filial       = fin017.codigo_filial       and
        ger021aa.codigo_departamento = fin017.codigo_departamento

     left join eng004_obra eng004 on                                  
         eng004.codigo_empresa = fin017.codigo_empresa and
         eng004.codigo_filial  = fin017.codigo_filial  and
         eng004.codigo_obra    = fin017.codigo_obra

     left join vwpln002_planejamento vwpln002 on                               
        vwpln002.codigo_empresa          = fin017.codigo_empresa          and
        vwpln002.codigo_filial           = fin017.codigo_filial           and
        vwpln002.codigo_obra             = fin017.codigo_obra             and
        vwpln002.numeroitem_planejamento = fin017.numeroitem_planejamento and
        fin017.numeroitem_planejamento is not null

     left join fin006_conta fin006 on
         fin006.codigo_empresa = fin017.codigo_empresa and
         fin006.codigo_conta   = fin017.codigo_conta

     left join fin005_banco fin005 on                                 
         fin005.numero_banco = fin006.numbanco_conta                  

     left join sis001a_rotina sis001a  on
        sis001a.codigo_rotina = 'fin017'

     left join sis001a_rotina sis001a_1  on
        sis001a_1.codigo_rotina = fin017.rotina_origem

     left join   sis005_usuario  sis005_3  on
        sis005_3.codigo_usuario = fin017.usrativacao_lctobancario



where fin017.situacao_lctobancario <> 'Cancelado'
;




/******************************************************************************/
/****                              Privileges                              ****/
/******************************************************************************/
