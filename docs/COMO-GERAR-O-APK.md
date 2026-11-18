# Como gerar o APK pelo GitHub

O APK é gerado pelo próprio GitHub, sem instalar nada no computador. O projeto já traz o arquivo
que faz isso: `.github/workflows/build-apk.yml`.

## Passo a passo

1. Descompacte o arquivo `JFP-Finance-0.7.0.zip` no computador.
2. No GitHub, crie um repositório novo e vazio (sem README, sem .gitignore), por exemplo `jfp-finance`.
   Pode ser privado: o código não tem senhas nem chaves, mas é o seu projeto.
3. Envie para o repositório **todo o conteúdo da pasta descompactada**, mantendo as pastas como estão.
   A pasta `.github` precisa ir junto: é ela que manda o GitHub gerar o APK.
   - Se o GitHub recusar o envio por causa da quantidade de arquivos (são cerca de 115), envie em duas
     partes: primeiro a pasta `app`, depois todo o resto.
4. O envio precisa cair no ramo chamado `main`.
5. Abra a aba **Actions** do repositório. Deve aparecer a execução "Gerar APK".
6. Espere terminar.
   - **✓ verde:** entre na execução, desça até *Artifacts* e baixe `jfp-finance-debug-apk`.
     Dentro dele está o `app-debug.apk`, que é o arquivo para instalar no celular.
   - **✗ vermelho:** entre na execução, clique na etapa que falhou e copie o trecho do erro.
     Com esse texto dá para corrigir.

## O que esperar na primeira vez

Este projeto nunca foi compilado. As regras (cálculos, cartão, importação, backup) foram testadas,
mas as telas e o banco de dados foram escritos sem compilador de Android. É provável que a primeira
execução termine em ✗ vermelho por algum erro de compilação. Isso é esperado: copie o erro,
corrija e envie de novo, até ficar verde.

A execução tem duas etapas que podem falhar por motivos diferentes:

- **"Testes dos módulos de regras":** roda os 110 testes. Se falhar aqui, é um problema de regra
  (pouco provável, porque esses testes já passaram).
- **"Gerar APK de teste":** compila as telas e o banco. É aqui que se espera o primeiro erro.

## Depois que o APK instalar

O Prompt-Mestre exige teste em celular de verdade antes de considerar o aplicativo pronto.
Um roteiro curto, na ordem em que as coisas dependem umas das outras:

1. Abrir o aplicativo e cadastrar uma conta na tela Início.
2. Em Configurações, definir um PIN; fechar o aplicativo por mais de 30 segundos e abrir de novo.
3. Em Movimentações, lançar uma receita, uma despesa e uma transferência; conferir o saldo no Início.
4. Cadastrar um cartão, lançar uma compra em 3 parcelas e pagar a fatura.
5. Lançar uma despesa prevista (desmarcar "Já aconteceu") e depois confirmá-la.
6. Em Planejamento, criar um orçamento, uma meta e uma dívida.
7. Em Patrimônio, cadastrar um bem e um investimento com aporte.
8. Em Assistente, ver os alertas, fazer uma pergunta e abrir as projeções.
9. Em Configurações > Dados, gerar um relatório em PDF e criar um backup.
10. **Por último**, testar restaurar o backup. É a parte de maior risco: faça só depois de ter
    o arquivo de backup salvo fora do aplicativo.

Anote o que não funcionar com o nome da tela e o que você fez antes do erro.
