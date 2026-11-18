# JFP Finance

**Intelligent Financial Management — Seu dinheiro. Sua estratégia.**
Por Jonata Ferreira Pessanha.

Aplicativo Android de gestão financeira (Kotlin + Jetpack Compose, local-first).
O documento central do projeto é o Prompt-Mestre v1.0; a arquitetura aprovada está na Fase 0.

## Estado atual

Versão 0.7.0. **O aplicativo ainda não foi compilado nem testado em um celular.**

- Testado: as regras financeiras, o assistente, a importação, os relatórios e o backup — 110 testes aprovados.
- Não testado: todas as telas, o banco de dados, as notificações, o PIN e a biometria, os arquivos (PDF, backup, restauração).

O APK é gerado pelo GitHub. O passo a passo, o que esperar na primeira vez e o roteiro de teste no
celular estão em [docs/COMO-GERAR-O-APK.md](docs/COMO-GERAR-O-APK.md).

O documento central do projeto está em [docs/PROMPT-MESTRE.md](docs/PROMPT-MESTRE.md).

## Módulos

| Módulo | O que contém |
|---|---|
| `core` | Kotlin puro: `Money` (centavos, arredondamento único), modelos e contratos |
| `finance-engine` | Kotlin puro: livro financeiro, saldos, resumo, reconciliação, faturas, parcelas, compromissos, orçamentos, metas, fluxo de caixa, amortização, investimentos, patrimônio |
| `ai` | Kotlin puro: assistente local por regras e preparação do envio mínimo para IA externa |
| `integrations` | Kotlin puro: leitura de extratos OFX e CSV e prévia de importação |
| `reports` | Kotlin puro: relatório do período e exportação em texto, CSV e JSON |
| `backup` | Kotlin puro: backup criptografado com senha e conferência de integridade |
| `data` | Banco local Room/SQLite |
| `security` | PIN (PBKDF2 + Android Keystore) e biometria |
| `ui-dashboard` | Tela inicial |
| `ui-comum` | Componentes de tela compartilhados |
| `ui-movimentacoes` | Lançamentos, contas a pagar e a receber, cartões |
| `ui-planejamento` | Orçamentos, metas, fluxo de caixa, dívidas |
| `ui-patrimonio` | Patrimônio líquido, bens, investimentos |
| `ui-assistente` | Alertas, perguntas ao assistente, projeções |
| `app` | Tema, navegação, montagem das dependências, tela Dados, notificações |

Módulo previsto e ainda não criado: `sync` (sincronização entre aparelhos).

## Andamento

| Fase | Conteúdo | Situação |
|---|---|---|
| 0 | Arquitetura | Concluída |
| 1 | Fundação | Código escrito — falta o primeiro build |
| 2 | Motor financeiro | Concluída e testada |
| 3 | Gestão financeira | Regras testadas; telas e banco escritos — falta o primeiro build |
| 4 | Planejamento e patrimônio | Regras testadas; telas e banco escritos — falta o primeiro build |
| 5 | Inteligência | Regras testadas; telas escritas — falta o primeiro build. IA externa sem provedor escolhido |
| 6 | Recursos avançados | Regras testadas; telas escritas — falta o primeiro build. Sem sincronização nem XLSX |
| 7 | Fechamento | Revisão e documentação feitas. APK e teste em celular dependem do primeiro build |

## O que falta em relação ao Prompt-Mestre

Lista consolidada. Os detalhes de cada fase estão no histórico de versões abaixo.

**Depende de decisão ou de infraestrutura que o projeto não tem**
- IA externa (seção 19): falta escolher o provedor. Só a preparação do envio mínimo está pronta.
- Sincronização entre aparelhos, conta de usuário e grupo familiar conectado com permissões (seções 13, 22, 23): exigem servidor.
- Open Finance (seção 24): nada preparado.
- Backup remoto (seção 31).

**Previsto e não feito**
- XLSX na importação e na exportação (seções 24 e 25).
- Criar e editar categorias e subcategorias; editar ou arquivar contas e cartões; editar metas, dívidas, bens e investimentos.
- Anexar comprovantes e documentos; campo de estabelecimento.
- Calendário financeiro e lembretes por conta.
- Central de notificações completa: histórico, lido/não lido, categorias, horários, prioridades (seção 26).
- Dashboard reorganizável, períodos diário/semanal/anual, alertas e indicadores na tela Início (seção 6).
- Tema claro e automático selecionáveis, tamanho de texto, tela inicial, atalhos (seção 27). O tema claro existe no código, sem tela para escolher.
- Onboarding guiado e ambiente de demonstração (seção 29).
- Orçamento por conta, pessoa e período; amortização personalizada e pagamento extraordinário de dívida.
- Histórico de avaliações de bens; cotações de investimentos.
- Relatórios de orçamentos, metas, tendências, anomalias, projeções e comparações mensais e anuais.
- Projeção de patrimônio e de investimentos; sazonalidade.
- Backup automático e histórico de cópias; controle e revogação de dispositivos; auditoria de operações.
- Banco de dados criptografado no aparelho. Hoje a proteção é o PIN na abertura do aplicativo e a área privada do Android; o backup, esse sim, é criptografado.
- Preferências em DataStore (usam SharedPreferences).
- Testes de interface, de integração com o banco e de segurança (seção 30). Só existem testes de regras.

## Histórico de versões

### 0.7.0
- Revisão do código Android em busca de nomes usados sem importação e de dependências entre módulos.
  Corrigido: o módulo `data` passa a expor o Room, porque os outros módulos usam `JfpDatabase`.
- Guia para gerar o APK e roteiro de teste no celular em `docs/COMO-GERAR-O-APK.md`.
- Prompt-Mestre incluído em `docs/`.
- Lista consolidada do que falta em relação ao Prompt-Mestre.

### 0.6.0-fase6
- Importação de extratos OFX (formato antigo e XML) e CSV (separador, cabeçalho, datas e valores em
  formato brasileiro ou americano, colunas de crédito e débito). Prévia com conta, categoria sugerida
  pela descrição já usada, avisos de linhas não lidas e detecção de duplicidade.
- Reimportar o mesmo arquivo não duplica: cada linha recebe um identificador calculado do conteúdo.
- Relatório do período, com realizado, previsto e posição em seções separadas e rotuladas; por categoria
  e por pessoa; lista de lançamentos. Salva em PDF, CSV e JSON, sempre com período, moeda, filtros e data.
- Backup manual criptografado com senha (AES-256-GCM, chave por PBKDF2). Depois de gravar, o aplicativo
  relê o arquivo e confere se abre e se é idêntico ao banco. Restauração guarda o banco anterior no aparelho.
- Apagar todos os dados, com explicação do que sai do aparelho e do que não é apagado.
- Pessoas da casa: cada lançamento pode ser de uma pessoa ou da casa toda; relatório por pessoa.
- Banco: tabela de pessoas e duas colunas novas em operações (pessoa e arquivo de origem).
- Testes: 110 no total (27 novos), todos aprovados. Um deles cobre um erro que eu teria deixado passar:
  valor "1.500" em CSV era lido como R$ 1,50; agora ponto seguido de três dígitos, sem vírgula, é milhar.

Decisões registradas:
- O backup é uma cópia do arquivo inteiro do banco, não uma exportação tabela a tabela: nada fica de fora
  e não há conversão para manter. Não inclui PIN nem preferências.
- Restaurar e apagar tudo reiniciam o aplicativo na hora, porque o banco em uso é fechado.
- No CSV exportado, texto que começa com = + - @ ganha um apóstrofo, para o Excel não executar como fórmula.
- Lançamento importado igual a um manual (mesma conta, data, valor e tipo) vem desmarcado como possível duplicado.

Ficou fora desta fase (previsto no Prompt-Mestre):
- **Sincronização entre aparelhos e grupo familiar conectado com permissões.** Exigem um servidor e contas
  de usuário, que não existem. Hoje a "família" é só a marcação de pessoas dentro de um aparelho.
- **XLSX**, na importação e na exportação. O CSV abre no Excel.
- Backup automático, histórico de cópias e backup remoto.
- Open Finance: nenhuma preparação foi escrita além de a importação manual não depender dele.
- Relatórios de orçamentos, metas, tendências, anomalias e projeções, e comparações mensais e anuais.
- Importar transferência ou pagamento de fatura como tal (hoje entram como receita ou despesa; a tela avisa).
- Cadastro de usuário e conta conectada (seção 23): o aplicativo funciona só sem conta.

### 0.5.0-fase5
- Diagnóstico local: receita, despesa e sobra médias, percentual poupado, meses de reserva,
  peso das parcelas de dívidas na receita.
- Alertas com explicação de onde veio cada um: risco de conta negativa em 30 dias, previstos vencidos
  e próximos, fatura fechada perto do vencimento ou vencida, orçamento, endividamento, reserva baixa,
  gasto acima do habitual, categoria acima da média, concentração de despesas, meta com prazo vencido.
- Projeções de 1 a 60 meses nos cenários conservador, intermediário, otimista e personalizado,
  sempre com a lista de premissas. Nada é gravado.
- Assistente local: responde sobre saldo, gastos, receitas, categorias, pendências, cartões, dívidas,
  metas, orçamentos, poupança, patrimônio, projeção e saúde financeira. Sempre informa as premissas.
  Só lê: recebe um leitor de dados que não tem método de gravação.
- Aviso diário opcional (desligado por padrão): uma notificação com a quantidade de alertas importantes,
  sem valores nem nomes.
- Preparação do envio para IA externa: monta só totais e percentuais, com a lista do que seria compartilhado.
- Testes: 83 no total (24 novos), todos aprovados. Os módulos puros foram compilados um a um,
  na ordem de dependência, para conferir a visibilidade entre módulos.

Decisões registradas:
- O "assistente local" é um conjunto de regras por palavras-chave, não um modelo de linguagem.
  Ele não interpreta texto livre; pergunta fora dos assuntos conhecidos recebe "não reconheci", nunca um palpite.
- As médias usam até 3 meses completos anteriores e não contam meses anteriores ao primeiro lançamento.
  Sem nenhum mês completo, usam o mês atual e avisam que a base é incompleta.
- A projeção repete as médias mês a mês; não soma lançamentos previstos um a um, para não contar duas vezes.
- Limiares dos alertas (30%/50% de endividamento, 3 meses de reserva, 80% de orçamento, 2x a média para
  gasto incomum) são convenções fixas no código, descritas no próprio alerta.

Ficou fora desta fase:
- **IA externa não conecta a nada.** Falta escolher o provedor. O aplicativo continua sem permissão de internet.
  A interface `IaExterna`, a tela de autorização por uso e o armazenamento da chave ainda precisam ser feitos.
- Sazonalidade (precisa de pelo menos 12 meses de dados).
- Central de notificações completa: histórico, estado de lido, categorias, horários e prioridades configuráveis.
- Alertas e indicadores na tela Início (hoje ficam na aba Assistente).
- Projeção de patrimônio, de investimentos e de mudança de juros; só o saldo das contas é projetado.

### 0.4.0-fase4
- Orçamento mensal por categoria: gasto, previsto ainda não pago, disponível, aviso aos 80% e ao estourar.
- Metas: valor guardado, percentual, prazo e quanto guardar por mês para chegar lá.
- Fluxo de caixa de 6 meses: realizado separado de previsto, com saldo projetado no fim de cada mês.
- Dívidas e financiamentos: tabelas Price e SAC, saldo devedor, juros, custo total, baixa de parcela
  (com lançamento opcional da despesa) e simulação antes de salvar. Calculadoras de juros simples e compostos no motor.
- Bens: valor de aquisição, valor estimado atual, valorização ou depreciação.
- Investimentos: aportes, resgates e rendimentos; custo, preço médio, ganho realizado e não realizado.
- Patrimônio líquido: contas + investimentos + bens − dívidas − cartões − contas negativas.
- Dois tipos novos no livro: aplicação e resgate de investimento (movem dinheiro sem virar despesa ou receita).
- Banco: 7 tabelas novas (orçamentos, metas, aportes de meta, dívidas, bens, investimentos, movimentos).
- Testes: 59 no total (20 novos), todos aprovados. Um teste novo encontrou e corrigiu um erro:
  o aviso de 80% do orçamento disparava em 79,9% por causa do arredondamento do percentual.

Decisões registradas:
- O valor guardado em uma meta é uma marcação: não movimenta contas nem entra no livro.
- Aplicar em investimento não é despesa e resgatar não é receita; o patrimônio não muda com a aplicação.
- No resgate sem quantidade, o custo baixado é proporcional ao valor resgatado sobre o valor atual informado.
- Parcela de dívida paga é lançada como despesa pelo valor cheio (juros + amortização).
- No patrimônio, o cartão conta pela dívida total, incluindo parcelas a vencer.
- Conta com saldo negativo conta como passivo.

Limitações conhecidas desta fase:
- Baixa de parcela de dívida e movimento de investimento gravam em dois lugares (livro e cadastro) em sequência;
  se o aplicativo for interrompido exatamente entre as duas gravações, fica o lançamento na conta sem a baixa.
- A economia de quitação antecipada é uma estimativa pela tabela, sem cálculo pro rata por dia.
- Orçamento só por categoria e por mês (o Prompt-Mestre prevê também por conta, pessoa e período).
- Bens sem histórico de avaliações nem documentos anexados; investimentos só com avaliação manual.
- Não é possível editar metas, dívidas, bens e investimentos depois de criados (só arquivar e, onde cabe, atualizar o valor).
- Amortização personalizada e pagamento extraordinário de dívida ainda não existem.

### 0.3.0-fase3
- Categorias (com lista padrão criada no primeiro uso) e cartões de crédito.
- Banco: tabelas de operações, categorias e cartões.
- Toda gravação passa pelo motor (`GestaoFinanceira`): valida as regras e grava tudo ou nada.
- Cartões: ciclo de fatura (fechamento e vencimento), parcelamento, limite em uso e disponível,
  pagamento de fatura abatendo da mais antiga.
- Contas a pagar e a receber: lançamentos previstos, com repetição mensal, confirmação e cancelamento;
  aviso de vencidos e de risco de saldo negativo nos próximos 30 dias.
- Tela Movimentações: lançamentos do mês com resumo, pendentes e cartões; novo lançamento
  (despesa, receita, transferência, compra no cartão); estorno.
- Dashboard ligado ao motor: saldos calculados do histórico, receitas/despesas/resultado do mês, pendências.
- Testes: 39 no total (13 novos), todos aprovados. A lógica de apresentação das telas
  (`MovimentacoesEstado.kt`, `DashboardEstado.kt`) foi compilada contra o motor.

Decisões registradas:
- Compra feita até a véspera do fechamento entra na fatura do mês; no dia do fechamento ou depois, na seguinte.
- Parcela conta como despesa no mês de cada parcela; o limite do cartão é consumido pelo total desde a compra.
- Contas a pagar e a receber são operações previstas do próprio livro, não uma tabela separada.
- O banco continua na versão 1 porque nenhum APK foi distribuído; depois do primeiro, toda mudança exige migração.

Ficou fora desta fase (previsto no Prompt-Mestre):
- Tela para criar e editar categorias e subcategorias (hoje só a lista padrão).
- Editar ou arquivar contas e cartões pela interface.
- Anexar comprovantes; estabelecimento como campo próprio.
- Calendário financeiro e lembretes (lembretes dependem do módulo de notificações).
- Recorrência sem data de fim (hoje: repetir por N meses).
- Seletor de data em calendário (hoje a data é digitada).

### 0.2.0-fase2
- Modelo de operação do livro financeiro: receita, despesa, transferência, compra no cartão,
  pagamento de fatura, reembolso e ajustes; situações prevista, confirmada e cancelada.
- Livro financeiro: registro com validação, proteção contra duplicidade, estorno e correção
  sem apagar histórico, confirmação e cancelamento de previstas.
- Saldos: atual (calculado do histórico), comprometido, disponível e projetado; saldo total; dívida do cartão.
- Resumo do período: receitas, despesas e resultado, por categoria, sem dupla contagem
  (transferência, pagamento de fatura e ajuste não entram).
- Reconciliação: compara com o saldo informado e propõe o ajuste, sem aplicar sozinho.
- Testes: 21 testes do motor + 5 de `Money`, todos aprovados.

Decisões registradas:
- O valor da operação é sempre positivo; o sentido vem do tipo. As regras de efeito ficam em um único arquivo (`Efeitos.kt`).
- Compra no cartão é despesa na data da compra, não na data do pagamento da fatura.
- Reembolso reduz a despesa da categoria; não é receita.
- Operações confirmadas só mudam por estorno; previstas podem ser confirmadas ou canceladas.

### 0.1.0-fase1
- Estrutura do projeto em módulos e build do APK pelo GitHub Actions.
- Identidade visual: azul-marinho `#101B2B`, dourado `#D5B16D`, marfim `#F5F0E6`; tema escuro como padrão.
- Navegação com as cinco áreas: Início, Movimentações, Planejamento, Patrimônio e Assistente.
- Banco local com a tabela de contas (versão 1 do esquema).
- Segurança inicial: PIN, biometria opcional, bloqueio automático após 30 segundos fora do
  aplicativo, tela protegida nos aplicativos recentes, backup automático do Google desligado.
- Dashboard básico: saldo total, saldo por conta, ocultar saldos, cadastro de conta.

### Testes realizados
- `core`: 5 testes de `Money` (soma exata, parcelas, arredondamento, leitura de texto, formatação) — aprovados.
- `integrations`: 13 testes; `reports`: 7 testes; `backup`: 7 testes — aprovados.
- `ai`: 9 testes (assistente local e conteúdo do envio externo) — aprovados.
- `finance-engine`: 69 testes (livro, saldos, resumo, reconciliação, faturas, parcelas, compromissos, gravação, orçamentos, metas, fluxo, amortização, investimentos, patrimônio, análises, projeções) — aprovados.
- Lógica de apresentação das telas (arquivos `*Estado.kt`): compilada contra o motor.
- Módulos Android: ainda não compilados (dependem do primeiro build no GitHub Actions).

### Pendências conhecidas
- Preferências usam SharedPreferences, não DataStore.
- O banco ainda não é criptografado (previsto junto com o backup criptografado).
- Ícone do aplicativo provisório.
- Não há recuperação de PIN.
