# Agenda Acadêmica: acesso

## 1. Identidade e referência
Implementação Android nativa em Jetpack Compose. A referência fornecida em 19/11/2026
define a composição: fundo azul quase preto com luz azul e violeta, capelo em um
quadrado azul, título em duas linhas, ilustração 3D à direita, três indicadores,
painel translúcido com contorno, campos largos e ação em gradiente.
Usar a ilustração original fornecida pelo usuário. Não incorporar a captura de
tela inteira como interface. Barras de sistema são as reais do Android.

## 2. Cores
Tokens em `LoginStyle`: Background #080F1B; SurfaceTop #1D2738; SurfaceBottom
#0D1625; Border #293650; FieldBorder #586478; Text #F5F7FC; Muted #BBC2D0;
Accent #82BEFF; BlueStart #2699FF; BlueEnd #194ADD; Violet #40306C;
ChipBackground #0C1C2E; Error #FFC1BE. A tela de acesso mantém esta paleta em
ambos os temas. O restante do app mantém os temas Material existentes.

## 3. Tipografia
Roboto/sans-serif nativa. Título 28sp, negrito, entrelinha 31sp; corpo/campos
14sp, entrelinha 19sp; labels/ações 14sp seminegrito; indicadores 12sp;
nota de privacidade 11sp com entrelinha 16sp. Escala de fonte do sistema respeitada.

## 4. Layout e espaçamento
Base 4dp, com meios passos de 2dp para contornos e ajustes ópticos. Margem da
tela 18dp; conteúdo com largura máxima de 440dp; painel com raio 26dp e padding
20dp; campos com raio 12dp e altura mínima 48dp; botão com raio 24dp e altura
48dp; espaço entre seções 24dp. Cabeçalho com ilustração de 196dp, ícone 56dp,
título e descrição alinhados à esquerda. Em telas estreitas o texto flui e o
conteúdo rola; com fonte ampliada ou área útil abaixo de 340dp a ilustração fica abaixo do texto.

## 5. Componentes e estados
- `LoginBackdrop`: fundo e curvas/luzes decorativas, sem semântica interativa.
- `LoginHero`: marca, título, descrição e ilustração; `FeatureChip` apresenta
  Prazos, Tarefas, Progresso, sem ação de navegação.
- `AccessHeading`: título Entrar com ícone de usuário. Não há aba nem campo de cadastro manual.
- `LoginField`: label externo, ícone, placeholder e contorno; foco azul,
  desabilitado durante envio; senha com alternância acessível de visibilidade.
- `AccessButton`: gradiente azul, texto branco e seta; desabilitado sem dados
  ou configuração; progresso branco durante envio; alvo de toque mínimo 48dp.
- Mensagens: texto de erro/retorno com live region; recuperação continua
  disponível e usa o e-mail informado. Nota de privacidade com escudo.

## 6. Comportamento e acessibilidade
E-mail com trim, teclado apropriado, ação Próximo; senha com ação Concluir.
Senha não persiste em estado salvo. Operação consulta Firebase Authentication,
que é a fonte de contas: tenta entrar, tenta criar somente quando a credencial
não corresponde a uma conta, e rejeita colisões (senha errada de conta existente).
Não consulta a coleção privada users antes de autenticar nem abre suas regras.
Campos, abas e botões possuem semântica acessível. Conteúdo rola com teclado e
respeita barras do sistema. Sem acesso Google, conforme solicitação do usuário.

## 7. Validação
Compilar APK, executar testes de decisão de acesso, testes Compose de interação
e Android Lint. Inspecionar a tela nativa em emulador e capturas de telefone,
tela estreita e fonte ampliada. Diferenciar testes locais do acesso real ao
Firebase; não usar credenciais reais nem afirmar validação remota sem execução.

Correções solicitadas: acesso único sem cadastro manual; fundo com luz azul/violeta
e curva orgânica contínua (coroa arredondada, cintura côncava, cauda inferior direita);
pílulas com gradiente translúcido, borda iluminada, sombra azul e halo nos ícones.
Tokens adicionais: GlowBlue #073E87, GlowViolet #39247D, OrganicTop #080F20,
OrganicMiddle #101B42, OrganicBottom #07316A, GlassTop #102C43, GlassBottom #0A1524.

Estados desabilitados preservam placeholders, ícones e link de recuperação em
Muted com 80% de opacidade, garantindo legibilidade sobre o painel escuro.

Aviso abaixo do botão Entrar: "Se você ainda não tem conta, ela será criada
automaticamente ao entrar." Corpo 14sp/19sp, cor Muted, centralizado, margem
superior de 12dp; permanece visível nos estados do formulário.

## Configurações: referência de 19/11/2026
O modelo define a hierarquia: cabeçalho com engrenagem à direita, cartão Perfil,
seção PREFERÊNCIAS com Tema escuro/Widgets/Lembretes e divisórias internas,
seção DADOS com Dados acadêmicos, botão Limpar tudo e ação Sair da conta.
O navbar existente não faz parte desta alteração. As cores vêm exclusivamente
de MaterialTheme.colorScheme em ambos os temas: background, surface,
surfaceVariant, outline, primary/onPrimary, primaryContainer/onPrimaryContainer, secondaryContainer,
onSurface/onSurfaceVariant e error. Não copiar a paleta azul da imagem.

Geometria Compose: margem 16dp, conteúdo centralizado até 600dp, cabeçalho com
padding vertical de 24dp, engrenagem de 56dp e halo tonal decorativo de 156dp;
título 26sp/30sp bold, descrição 14sp/20sp. Cartões com raio 16dp, contorno 1dp,
linhas com padding horizontal 18dp/vertical 14dp, ícones 26dp, intervalo 16dp,
perfil com avatar circular de 46dp. Títulos das linhas 16sp semibold e descrições
13sp/18sp; labels de seção 12sp semibold com tracking 2sp e margem superior 26dp.
Botões de ação com raio 16dp e altura mínima 48dp; limpeza com contorno e fundo
error a 10% de opacidade; sair com contorno outline. Base de espaçamento 4dp,
com meios passos de 2dp. Conteúdo rola e textos crescem com a fonte do sistema.

Primitivas: SettingsHeader, SettingsCard, SettingsRow e SettingsSectionLabel.
Linhas navegáveis têm ripple, seta e alvo mínimo 48dp. O switch possui semântica
própria e atualiza o tema persistente existente. Perfil abre modal editável com
e-mail e nome persistido; cancelar descarta a edição. Dados acadêmicos abre resumo
com contagens atuais de disciplinas, aulas, atividades e documentos. Widgets e
lembretes preservam suas telas funcionais atuais. Limpar tudo abre modal que
explica remoção permanente e cancelamento dos lembretes, mantendo conta, perfil
e preferências. Só confirmar executa a limpeza; durante importação/limpeza a
ação fica desabilitada, com motivo e retorno visíveis. Sair aciona o logout real.

Validação: testes Compose dos modais, cancelamento/confirmação da limpeza,
persistência do perfil, callbacks, modo claro/escuro, largura 320dp e fonte 1.6x;
capturas Roborazzi e execução Android. Sem alegar acesso remoto quando simulado.
O símbolo de dados usa vetor cilíndrico de 24dp com traço 1.7dp, conforme a
referência, tingido por onSurfaceVariant; compartilhado entre linha e modal.
