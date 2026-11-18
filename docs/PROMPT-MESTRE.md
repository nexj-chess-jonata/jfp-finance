# PROMPT-MESTRE — JFP FINANCE
## Intelligent Financial Management
**Seu dinheiro. Sua estratégia.**  
**Por Jonata Ferreira Pessanha**

**Versão:** 1.0 — Documento consolidado  
**Plataforma inicial:** Android (APK)  
**Idioma inicial:** Português brasileiro  
**Moeda inicial:** Real brasileiro (BRL)  
**Tecnologia principal:** Kotlin + Jetpack Compose  
**Arquitetura:** Avançada, modular, híbrida e local-first.

---

# 1. VISÃO GERAL DO PROJETO

Desenvolver o **JFP Finance**, um aplicativo Android completo de gestão financeira inteligente, com inteligência artificial avançada, motor financeiro próprio, gerenciamento patrimonial, planejamento, simulações e acompanhamento financeiro individual e familiar.

O aplicativo deverá funcionar como um verdadeiro centro de controle financeiro pessoal, permitindo que o usuário registre, organize, acompanhe, analise, projete e planeje sua vida financeira.

Não deverá ser apenas um aplicativo de controle de despesas. Deverá funcionar como um sistema integrado de inteligência financeira, capaz de compreender a situação financeira do usuário, identificar problemas, encontrar padrões, acompanhar objetivos e auxiliar na tomada de decisões.

O sistema deverá contemplar:

- Finanças pessoais.
- Finanças familiares.
- Receitas e despesas.
- Contas bancárias e carteiras.
- Cartões de crédito.
- Orçamentos.
- Dívidas e empréstimos.
- Patrimônio e bens.
- Investimentos.
- Planejamento financeiro.
- Metas e objetivos.
- Projeções financeiras.
- Simulações econômicas.
- Relatórios avançados.
- Inteligência artificial financeira.
- Alertas e notificações inteligentes.
- Importação de dados financeiros.
- Arquitetura preparada para Open Finance.
- Sincronização opcional entre dispositivos.

O aplicativo deverá possuir identidade própria, independente de outros projetos, embora possa integrar futuramente o ecossistema Nex-J.

# 2. PRINCÍPIOS FUNDAMENTAIS

O desenvolvimento deverá respeitar os seguintes princípios:

1. Precisão nos cálculos financeiros.
2. Segurança e privacidade dos dados.
3. Funcionamento offline das funcionalidades essenciais.
4. Independência do motor financeiro em relação à IA.
5. Separação entre dados reais, estimativas, projeções e simulações.
6. Transparência nas recomendações.
7. Controle do usuário sobre suas informações.
8. Ausência de dependência obrigatória de contas externas.
9. Arquitetura modular e expansível.
10. Facilidade de utilização sem sacrificar funcionalidades avançadas.

Nenhuma funcionalidade deverá comprometer a integridade dos registros financeiros.

# 3. IDENTIDADE VISUAL E MARCA

Criar uma identidade visual premium, moderna, elegante e profissional.

**Marca:** JFP FINANCE  
**Descrição:** INTELLIGENT FINANCIAL MANAGEMENT  
**Slogan:** Seu dinheiro. Sua estratégia.  
**Assinatura:** Por Jonata Ferreira Pessanha.

### Paleta oficial

- Azul-marinho: `#101B2B`.
- Dourado envelhecido: `#D5B16D`.
- Marfim: `#F5F0E6`.

O tema escuro será a apresentação principal. O tema claro será opcional.

A interface deverá utilizar:
- Tipografia profissional.
- Gráficos modernos.
- Ícones consistentes.
- Animações discretas.
- Componentes organizados.
- Hierarquia visual clara.
- Excelente legibilidade.
- Adaptação a diferentes tamanhos de tela.

Não utilizar uma aparência genérica de aplicativo financeiro. O JFP Finance deverá possuir identidade visual reconhecível.

# 4. ARQUITETURA TECNOLÓGICA

Desenvolver inicialmente como aplicativo Android nativo.

### Tecnologias principais

- Kotlin.
- Jetpack Compose.
- Room/SQLite.
- Kotlin Coroutines.
- WorkManager.
- Android Keystore.
- BiometricPrompt.
- DataStore.
- Navigation Compose.

### Arquitetura

Adotar arquitetura modular, organizada por responsabilidades e camadas.

Separar:
- Interface do usuário.
- Regras de negócio.
- Motor financeiro.
- Persistência de dados.
- Segurança.
- Inteligência artificial.
- Integrações externas.
- Relatórios.
- Notificações.
- Sincronização.

O motor financeiro deverá funcionar independentemente da interface e da IA.

Utilizar princípios de arquitetura limpa, injeção de dependências, baixo acoplamento, testes automatizados e componentes reutilizáveis.

O projeto deverá permitir adicionar novos módulos sem reconstruir toda a aplicação.

# 5. ESTRUTURA PRINCIPAL DO APLICATIVO

Criar navegação principal com cinco áreas:

1. Início.
2. Movimentações.
3. Planejamento.
4. Patrimônio.
5. Assistente IA.

O assistente deverá permanecer acessível em diferentes áreas do aplicativo.

Criar também uma central de configurações, segurança, notificações, importações, relatórios e administração dos dados.

# 6. DASHBOARD FINANCEIRO

A tela inicial deverá funcionar como painel de controle financeiro.

Apresentar:

- Saldo total.
- Saldo por conta.
- Receitas do período.
- Despesas do período.
- Resultado financeiro.
- Contas próximas do vencimento.
- Cartões e faturas.
- Orçamentos.
- Metas.
- Patrimônio líquido.
- Dívidas.
- Investimentos.
- Fluxo de caixa.
- Alertas importantes.
- Indicadores de saúde financeira.

Permitir alternar períodos: diário, semanal, mensal, anual e personalizado.

Os componentes deverão ser reorganizáveis e personalizáveis.

Permitir ocultar saldos e informações sensíveis.

# 7. RECEITAS

Implementar gerenciamento completo de receitas.

Permitir:
- Cadastro manual.
- Receitas recorrentes.
- Salários.
- Rendimentos.
- Receitas extraordinárias.
- Receitas familiares.
- Receitas previstas.
- Receitas recebidas.
- Receitas atrasadas.

Cada registro deverá conter valor, data, origem, categoria, conta de destino, recorrência e situação.

Distinguir receitas confirmadas de receitas previstas.

# 8. DESPESAS

Implementar gerenciamento completo de despesas.

Permitir:
- Cadastro manual.
- Despesas fixas.
- Despesas variáveis.
- Despesas recorrentes.
- Despesas parceladas.
- Despesas familiares.
- Despesas extraordinárias.
- Despesas previstas.
- Despesas pagas e pendentes.

Criar categorias e subcategorias personalizáveis.

Registrar data, valor, descrição, estabelecimento, conta, categoria, forma de pagamento e situação.

Permitir anexar comprovantes e documentos localmente.

# 9. CONTAS E FLUXO DE CAIXA

Permitir cadastrar:

- Contas bancárias.
- Carteiras digitais.
- Dinheiro em espécie.
- Contas de pagamento.
- Outras contas personalizadas.

Controlar saldo inicial, entradas, saídas, transferências e saldo atualizado.

Transferências entre contas próprias não poderão ser contabilizadas como receita ou despesa.

Criar fluxo de caixa real e projetado, com acompanhamento diário, semanal, mensal e anual.

# 10. CONTAS A PAGAR E A RECEBER

Criar módulo específico para compromissos financeiros.

Permitir:
- Cadastro de vencimentos.
- Pagamentos.
- Recebimentos.
- Recorrências.
- Parcelamentos.
- Atrasos.
- Histórico.
- Lembretes.
- Calendário financeiro.

Identificar compromissos próximos do vencimento e riscos de insuficiência de saldo.

# 11. CARTÕES DE CRÉDITO

Criar gerenciamento completo de cartões.

Permitir:
- Cadastro de cartões.
- Limites.
- Limites utilizados e disponíveis.
- Datas de fechamento.
- Datas de vencimento.
- Faturas.
- Compras.
- Parcelamentos.
- Pagamentos.
- Estornos.
- Reembolsos.

Implementar regras rigorosas contra dupla contabilização.

Uma compra deverá ser registrada como despesa uma única vez. O pagamento posterior da fatura deverá liquidar a obrigação, sem registrar novamente a mesma despesa como consumo.

# 12. ORÇAMENTOS

Permitir criar orçamentos por:

- Categoria.
- Subcategoria.
- Período.
- Conta.
- Pessoa.
- Grupo familiar.

Apresentar valor planejado, valor realizado, compromissos, saldo disponível e percentual utilizado.

Criar alertas configuráveis quando os limites forem atingidos ou estiverem próximos.

# 13. GESTÃO FAMILIAR

O sistema deverá permitir utilização individual e familiar.

O usuário poderá começar sozinho e posteriormente criar um grupo familiar.

Separar:
- Dados pessoais.
- Contas privadas.
- Despesas compartilhadas.
- Receitas compartilhadas.
- Patrimônio individual.
- Patrimônio familiar.

Implementar permissões granulares.

A entrada em um grupo familiar nunca deverá liberar automaticamente todos os dados privados do usuário.

Permitir definir quem pode visualizar, cadastrar, editar e administrar cada informação compartilhada.

# 14. PATRIMÔNIO E BENS

Criar módulo de patrimônio.

Permitir cadastrar:
- Imóveis.
- Veículos.
- Terrenos.
- Equipamentos.
- Bens pessoais.
- Participações.
- Outros ativos.

Registrar valor de aquisição, data, valor estimado atual, documentos e histórico de valorização ou depreciação.

Criar cálculo de patrimônio bruto, passivos e patrimônio líquido.

Evitar dupla contabilização de ativos e obrigações.

# 15. DÍVIDAS E EMPRÉSTIMOS

Implementar controle de dívidas e financiamentos.

Permitir:
- Cadastro de credores.
- Valor original.
- Saldo devedor.
- Juros.
- Parcelas.
- Prazos.
- Vencimentos.
- Amortizações.
- Pagamentos extraordinários.

Implementar simulações utilizando:
- Sistema Price.
- Sistema SAC.
- Juros simples.
- Juros compostos.
- Amortização personalizada.

Apresentar custo total, juros pagos, saldo devedor e cenários de quitação antecipada.

# 16. INVESTIMENTOS

Criar módulo para acompanhamento de investimentos.

Permitir registrar:
- Renda fixa.
- Tesouro Direto.
- CDB.
- LCI e LCA.
- Fundos.
- Ações.
- ETFs.
- FIIs.
- Outros ativos.

Controlar aportes, resgates, rendimentos, quantidade, preço médio, custo de aquisição e valorização.

Distinguir ganhos realizados e não realizados.

Inicialmente, permitir avaliações manuais. Preparar arquitetura para futuras integrações com fontes de cotações.

Não prometer rentabilidade nem apresentar projeções como resultados garantidos.

# 17. METAS FINANCEIRAS

Permitir estabelecer objetivos:

- Reserva de emergência.
- Compra de imóvel.
- Compra de veículo.
- Viagens.
- Investimentos.
- Quitação de dívidas.
- Independência financeira.
- Outros objetivos personalizados.

Acompanhar progresso, prazo, valor acumulado, valor restante e necessidade de aportes.

Permitir simular diferentes estratégias para alcançar as metas.

# 18. ASSISTENTE DE INTELIGÊNCIA ARTIFICIAL

Criar um assistente financeiro integrado ao aplicativo.

A IA deverá possuir capacidade avançada para:

- Analisar receitas e despesas.
- Identificar padrões.
- Avaliar orçamento.
- Identificar gastos incomuns.
- Analisar endividamento.
- Sugerir estratégias de quitação.
- Acompanhar metas.
- Analisar patrimônio.
- Interpretar investimentos.
- Produzir relatórios.
- Simular cenários.
- Identificar riscos.
- Explicar indicadores.
- Responder perguntas financeiras com base nos dados autorizados.

A IA deverá compreender o contexto financeiro do usuário e apresentar explicações claras, fundamentadas e transparentes.

Toda recomendação deverá informar as premissas e limitações relevantes.

A IA não poderá alterar, excluir, criar ou executar operações financeiras sem confirmação explícita.

# 19. ARQUITETURA DE IA HÍBRIDA

Implementar arquitetura híbrida:

### IA local
Responsável por:
- Consultas básicas.
- Análises locais.
- Monitoramento.
- Regras financeiras.
- Alertas.
- Processamento de dados autorizados.
- Operações que não exigem conexão externa.

### IA externa
Poderá ser utilizada para:
- Análises complexas.
- Conversas avançadas.
- Interpretações mais elaboradas.
- Produção de relatórios.
- Planejamento e explicações aprofundadas.

**Regra obrigatória de privacidade:** toda utilização de IA externa deverá depender de autorização explícita do usuário para cada utilização.

Antes de enviar informações, o sistema deverá explicar quais dados serão compartilhados e com qual finalidade.

Enviar apenas os dados estritamente necessários.

Não transmitir automaticamente o banco de dados financeiro completo.

O aplicativo deverá continuar funcionando mesmo sem acesso à IA externa.

# 20. PROJEÇÕES E SIMULAÇÕES

Criar laboratório de planejamento financeiro.

Permitir projeções para:
- 30 dias.
- 90 dias.
- 6 meses.
- 12 meses.
- Períodos personalizados.

Criar cenários:
- Conservador.
- Intermediário.
- Otimista.
- Personalizado.

Simular:
- Evolução do patrimônio.
- Capacidade de poupança.
- Quitação de dívidas.
- Crescimento de investimentos.
- Alterações de despesas.
- Perda ou aumento de renda.
- Mudanças nas taxas de juros.
- Impacto de decisões financeiras.

Separar completamente simulações e projeções dos registros reais.

# 21. SEGURANÇA E PRIVACIDADE

Implementar segurança em múltiplas camadas.

Recursos obrigatórios:
- PIN.
- Senha.
- Biometria.
- Bloqueio automático.
- Criptografia de dados.
- Android Keystore.
- Proteção de sessões.
- Controle de dispositivos autorizados.
- Revogação de dispositivos.
- Auditoria de operações.
- Proteção de notificações.
- Privacidade na tela de aplicativos recentes.

Permitir consultar, exportar e excluir os próprios dados.

A exclusão deverá explicar claramente o que será removido do dispositivo, dos servidores opcionais e das cópias de segurança.

# 22. ARMAZENAMENTO LOCAL E SINCRONIZAÇÃO

Adotar estratégia local-first.

O dispositivo será o principal ambiente operacional dos dados financeiros.

Utilizar Room/SQLite como banco local.

As funcionalidades essenciais deverão funcionar offline.

Preparar sincronização opcional e criptografada entre dispositivos autorizados.

Implementar:
- Controle de conflitos.
- Prevenção de duplicidades.
- Histórico de alterações.
- Identificação de dispositivos.
- Recuperação de dados.
- Integridade dos registros.

A nuvem não será obrigatória para utilizar o aplicativo.

# 23. CADASTRO E CONTA DO USUÁRIO

O cadastro deverá ser opcional.

O usuário poderá começar a utilizar o aplicativo sem criar conta.

Uma conta conectada poderá ser necessária para funcionalidades como:
- Sincronização entre dispositivos.
- Administração familiar conectada.
- Backup remoto.
- Gerenciamento remoto de dispositivos.

Separar identidade de acesso e registros financeiros.

O cadastro posterior deverá permitir migrar o perfil local sem perder os dados existentes.

Não enviar automaticamente o banco financeiro local ao servidor durante o cadastro.

# 24. INTEGRAÇÕES BANCÁRIAS E IMPORTAÇÃO

Implementar estratégia completa e evolutiva.

### Cadastro manual
Sempre disponível e independente de integrações externas.

### Importação de arquivos
Suportar:
- OFX.
- CSV.
- XLSX.

Implementar:
- Visualização prévia.
- Identificação de contas.
- Associação de categorias.
- Detecção de duplicidades.
- Conferência dos registros.
- Confirmação antes da importação.
- Histórico da origem dos dados.

### Open Finance Brasil

Preparar arquitetura para futuras integrações com Open Finance Brasil ou provedores autorizados, respeitando as condições legais e técnicas aplicáveis.

Exigir consentimento informado e revogável.

Não armazenar senhas bancárias.

Não realizar operações bancárias sem autorização específica e mecanismo oficialmente suportado.

O aplicativo não poderá depender de integrações bancárias para funcionar.

# 25. RELATÓRIOS E EXPORTAÇÕES

Criar sistema avançado de relatórios.

Disponibilizar relatórios de:
- Receitas.
- Despesas.
- Fluxo de caixa.
- Categorias.
- Comparações mensais e anuais.
- Orçamentos.
- Patrimônio.
- Dívidas.
- Investimentos.
- Metas.
- Diagnósticos financeiros.
- Tendências.
- Anomalias.
- Projeções.

Formatos de exportação:
- PDF.
- CSV.
- XLSX.
- JSON.

Cada relatório deverá informar período, moeda, filtros e data de geração.

Distinguir claramente dados realizados, estimativas, projeções e simulações.

# 26. NOTIFICAÇÕES E CENTRAL DE ALERTAS

Implementar central completa de notificações.

Categorias:
- Contas e vencimentos.
- Cartões e faturas.
- Orçamentos.
- Despesas incomuns.
- Receitas previstas.
- Dívidas.
- Investimentos.
- Metas.
- Análises da IA.
- Relatórios periódicos.

Permitir configurar:
- Categorias.
- Horários.
- Frequência.
- Prioridades.
- Antecedência.
- Notificações silenciosas.
- Pausas temporárias.

Criar histórico interno, estados de leitura e acesso ao registro relacionado.

Utilizar regras locais e tarefas programadas para monitoramento.

Evitar notificações duplicadas e excessivas.

Proteger informações sensíveis nas notificações exibidas na tela bloqueada.

# 27. CONFIGURAÇÕES E PERSONALIZAÇÃO

Permitir personalizar:

- Tema escuro.
- Tema claro.
- Tema automático.
- Organização do dashboard.
- Exibição de saldos.
- Indicadores.
- Gráficos.
- Categorias.
- Contas favoritas.
- Moeda.
- Idioma.
- Formato de datas e números.
- Tamanho dos textos.
- Acessibilidade.
- Tela inicial.
- Atalhos.

Manter a identidade visual JFP Finance.

O sistema poderá sugerir melhorias na organização da interface, mas não deverá alterar preferências importantes sem autorização.

# 28. MOTOR FINANCEIRO AVANÇADO

O motor financeiro será o núcleo central do JFP Finance.

Deverá ser desenvolvido como componente independente da interface e da IA.

### 28.1 Livro financeiro central

Criar estrutura centralizada de registros financeiros, inspirada em princípios de contabilidade por partidas dobradas.

Cada operação deverá possuir:
- Identificador único.
- Data.
- Valor.
- Moeda.
- Categoria.
- Conta.
- Contraparte, quando aplicável.
- Origem.
- Situação.
- Referências relacionadas.
- Histórico de alterações.

Suportar receitas, despesas, transferências, compras, pagamentos, estornos, reembolsos, ajustes, parcelamentos, dívidas e investimentos.

Preservar histórico confirmado.

Corrigir operações por meio de reversões e novos registros, evitando apagar silenciosamente o histórico.

### 28.2 Precisão monetária

Não utilizar números de ponto flutuante binário para cálculos monetários.

Armazenar valores em unidades monetárias menores, como centavos, ou utilizar representação decimal apropriada.

Definir regras explícitas de arredondamento.

Começar com BRL e preparar suporte futuro a múltiplas moedas.

### 28.3 Saldos e reconciliação

Controlar:
- Saldo inicial.
- Entradas.
- Saídas.
- Transferências.
- Ajustes.
- Saldo atual.
- Saldo disponível.
- Valores comprometidos.
- Saldo projetado.

Permitir reconciliação de contas.

### 28.4 Fluxo de caixa

Separar:
- Fluxo realizado.
- Fluxo previsto.
- Receitas comprometidas.
- Despesas comprometidas.
- Valores vencidos.

Permitir visualização por diferentes períodos e critérios de reconhecimento.

### 28.5 Cartões e faturas

Controlar compras, parcelas, limites, faturas e pagamentos.

Impedir dupla contabilização de despesas.

### 28.6 Orçamentos

Comparar valores planejados, realizados e comprometidos.

Calcular percentuais, limites e saldo restante.

### 28.7 Patrimônio

Calcular ativos, passivos e patrimônio líquido.

Impedir dupla contabilização entre módulos.

### 28.8 Investimentos

Controlar quantidades, aportes, resgates, rendimentos, custos e valorização.

Separar ganhos realizados e não realizados.

### 28.9 Dívidas

Calcular juros, amortização, parcelas, saldo devedor e custo total.

Implementar Price, SAC e modelos personalizados.

### 28.10 Projeções

Produzir projeções com premissas explícitas e cenários configuráveis.

### 28.11 Laboratório de simulação

Manter simulações isoladas dos dados financeiros reais.

### 28.12 Inteligência analítica

Identificar:
- Gastos incomuns.
- Tendências.
- Sazonalidade.
- Concentração de despesas.
- Capacidade de poupança.
- Risco de saldo negativo.
- Endividamento.
- Evolução patrimonial.
- Progresso das metas.

### 28.13 Integridade

Implementar transações atômicas, restrições de integridade, idempotência, controle de concorrência, recuperação e testes.

O motor deverá ser a fonte oficial dos resultados financeiros. A IA poderá interpretar os resultados, mas não substituir os cálculos oficiais.

# 29. PRIMEIRO ACESSO E CONFIGURAÇÃO GUIADA

Criar experiência inicial de onboarding.

Etapas:

1. Boas-vindas.
2. Escolha entre perfil individual e familiar.
3. Definição de moeda.
4. Cadastro de contas.
5. Configuração de categorias.
6. Registro de saldos iniciais.
7. Cadastro opcional de cartões.
8. Cadastro opcional de dívidas.
9. Definição de metas.
10. Configuração de segurança.
11. Configuração de notificações.
12. Apresentação do assistente de IA.

As etapas não essenciais poderão ser ignoradas e concluídas posteriormente.

Criar ambiente demonstrativo com dados fictícios para apresentar funcionalidades sem exigir dados financeiros reais.

Identificar claramente os dados de demonstração e impedir que sejam confundidos com registros reais.

# 30. QUALIDADE E TESTES

Desenvolver testes automatizados em diferentes níveis.

### Testes unitários
Para cálculos financeiros, juros, amortizações, saldos, projeções e regras de negócio.

### Testes de integração
Para banco de dados, importações, sincronização e módulos.

### Testes de interface
Para navegação, formulários, acessibilidade e estados visuais.

### Testes de segurança
Para autenticação, armazenamento, permissões, criptografia e exposição de dados.

### Testes de consistência
Para duplicidades, transferências, pagamentos, estornos e reconciliação.

Testar funcionamento offline, recuperação de falhas e diferentes versões e tamanhos de dispositivos Android.

Não considerar uma funcionalidade concluída sem testes adequados.

# 31. BACKUP, RESTAURAÇÃO E RECUPERAÇÃO

Implementar:

- Backup local.
- Backup criptografado.
- Exportação manual.
- Backup automático configurável.
- Restauração.
- Verificação de integridade.
- Histórico de cópias.

Preparar integração futura com armazenamento remoto opcional.

Informar claramente o que está incluído em cada cópia de segurança.

# 32. DESENVOLVIMENTO MODULAR

Organizar o projeto para permitir evolução independente dos módulos.

Sugestão de módulos:

- Core.
- Finance Engine.
- Accounts.
- Transactions.
- Cards.
- Budget.
- Planning.
- Assets.
- Investments.
- Debts.
- Family.
- AI Assistant.
- Reports.
- Notifications.
- Security.
- Backup.
- Integrations.
- Synchronization.
- Settings.

A estrutura definitiva poderá ser ajustada conforme a complexidade real do projeto.

# 33. EVOLUÇÃO FUTURA

Preparar o aplicativo para receber novas funcionalidades, incluindo:

- Planejamento de aposentadoria.
- Análise patrimonial avançada.
- Planejamento tributário informativo.
- Gestão financeira empresarial.
- Múltiplas moedas.
- Integrações financeiras adicionais.
- Inteligência econômica.
- Análises comparativas.
- Planejamento familiar avançado.
- Novos modelos de simulação.

Essas funcionalidades não deverão comprometer a estabilidade da versão inicial.

# 34. PROCESSO DE CONSTRUÇÃO E ATUALIZAÇÕES

O desenvolvimento deverá ocorrer por etapas.

Antes de implementar cada módulo:
1. Definir objetivo.
2. Planejar arquitetura.
3. Identificar dependências.
4. Definir modelos de dados.
5. Implementar.
6. Testar.
7. Documentar.
8. Integrar ao aplicativo.
9. Validar funcionamento.

**REGRA FUNDAMENTAL DE EVOLUÇÃO:**

Atualizações, alterações, correções e melhorias poderão ser realizadas durante toda a construção do JFP Finance.

O projeto não deverá ser considerado rígido ou imutável.

Novas ideias poderão ser incorporadas quando forem tecnicamente viáveis e compatíveis com os princípios fundamentais.

Toda alteração relevante deverá ser documentada.

Manter:
- Histórico de versões.
- Registro de decisões.
- Funcionalidades aprovadas.
- Funcionalidades implementadas.
- Pendências.
- Testes realizados.
- Problemas encontrados.
- Correções aplicadas.
- Mudanças arquiteturais.

Não remover funcionalidades anteriormente aprovadas sem análise e autorização.

Manter o Prompt-Mestre atualizado como documento central do projeto.

# 35. ETAPAS DE IMPLEMENTAÇÃO

Organizar o desenvolvimento em fases.

**FASE 1 — Fundação**
- Estrutura Android.
- Identidade visual.
- Navegação.
- Banco de dados.
- Segurança inicial.
- Dashboard básico.

**FASE 2 — Gestão financeira**
- Contas.
- Receitas.
- Despesas.
- Transferências.
- Cartões.
- Contas a pagar e receber.

**FASE 3 — Planejamento**
- Orçamentos.
- Metas.
- Fluxo de caixa.
- Dívidas.
- Simulações.

**FASE 4 — Patrimônio**
- Ativos.
- Passivos.
- Patrimônio líquido.
- Investimentos.

**FASE 5 — Inteligência**
- Motor financeiro avançado.
- Análises.
- Alertas.
- Assistente IA.
- Projeções.

**FASE 6 — Recursos avançados**
- Família.
- Importações.
- Relatórios.
- Backup.
- Sincronização opcional.

**FASE 7 — Finalização**
- Testes completos.
- Segurança.
- Otimização.
- Correções.
- Documentação.
- Geração do APK.

As fases poderão ocorrer parcialmente em paralelo quando isso não prejudicar as dependências técnicas.

# 36. ENTREGÁVEIS

O desenvolvimento deverá entregar:

1. Código-fonte completo.
2. Projeto Android organizado.
3. Banco de dados estruturado.
4. Motor financeiro documentado.
5. Interface completa.
6. Módulos funcionais.
7. Testes automatizados.
8. Documentação técnica.
9. Histórico de versões.
10. APK instalável.

O APK deverá ser testado em dispositivo Android real antes de ser considerado pronto.

Não entregar apenas protótipos visuais ou telas sem funcionamento.

# 37. RESULTADO FINAL ESPERADO

O resultado deverá ser um aplicativo Android profissional, completo e funcional, chamado **JFP Finance**, capaz de centralizar a vida financeira individual e familiar.

Deverá combinar:

- Gestão financeira.
- Inteligência artificial.
- Motor financeiro avançado.
- Planejamento.
- Gestão patrimonial.
- Investimentos.
- Simulações.
- Segurança.
- Privacidade.
- Automação.
- Relatórios.

O aplicativo deverá possuir qualidade técnica, visual e funcional suficiente para ser utilizado diariamente e evoluir continuamente.

**JFP FINANCE — Seu dinheiro. Sua estratégia.**

**Por Jonata Ferreira Pessanha.**