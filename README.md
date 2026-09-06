<div align="center">
<img width="1200" height="475" alt="GHBanner" src="https://ai.google.dev/static/site-assets/images/share-ais-513315318.png" />

# Agenda Acadêmica (Mobile)

Agenda acadêmica inteligente para centralizar e organizar automaticamente compromissos, disciplinas, prazos e documentos via IA.
</div>

---

## 📱 Sobre o Projeto

O **Agenda Acadêmica** é um aplicativo móvel desenvolvido em Android nativo (Kotlin / Jetpack Compose) integrado ao backend da Agenda Acadêmica, que usa o Gemini para interpretar PDFs sem expor a chave da IA no aplicativo.

### ✨ Principais Recursos
- 📅 **Gestão de Compromissos e Prazos:** Acompanhe datas de entregas, provas e eventos acadêmicos.
- 📚 **Controle de Disciplinas:** Organize matérias, horários e anotações.
- 🤖 **Assistente Inteligente (Gemini AI):** Resumos, geração de lembretes e auxílio nos estudos.
- 🔔 **Notificações:** Alertas antecipados para nunca perder prazos importantes.
- 📲 **Widgets:** Acesso rápido às próximas atividades direto na tela inicial.

---

## 🚀 Como Executar Localmente

### 📋 Pré-requisitos
- [Android Studio](https://developer.android.com/studio) (versão recente recomendada)
- JDK 17+
- Dispositivo Android físico ou emulador com Android 8.0 (API 26) ou superior
- Backend da Agenda Acadêmica em execução

### 🛠️ Passo a Passo

1. **Clonar/Abrir o Projeto:**
   - Abra o **Android Studio**.
   - Selecione **Open** e escolha a pasta deste projeto (`Mobile`).
   - Aguarde o término da sincronização do Gradle (permita que o Android Studio resolva eventuais dependências/incompatibilidades se solicitado).

2. **Configurar as Variáveis de Ambiente:**
   - O app usa por padrão o backend de produção configurado no [`.env.example`](.env.example).
   - Para testar um backend local, crie um arquivo `.env` na raiz do projeto (ele é ignorado pelo Git):
     ```env
     BACKEND_BASE_URL=http://10.0.2.2:3000/
     ```
   - `10.0.2.2` acessa o computador host a partir do emulador Android.
   - Em um dispositivo físico, use o IP local do computador. Em produção, use uma URL HTTPS.

3. **Ajuste de Build (se aplicável):**
   - Caso esteja executando localmente sem a configuração de assinatura do AI Studio, verifique no arquivo [`app/build.gradle.kts`](app/build.gradle.kts) se há referências a `signingConfig = signingConfigs.getByName("debugConfig")` e ajuste conforme necessário.

4. **Executar a Aplicação:**
   - Selecione o seu emulador ou dispositivo conectado na barra superior do Android Studio.
   - Clique em **Run** (`Shift + F10` ou o botão de play verde ▶️).

---

## 📦 Deploy e Publicação

- **AI Studio:** Se você já publicou seu app no AI Studio e precisa gerenciar a chave de upload, consulte o [pedido de redefinição de chave de upload no Google Play Console](https://support.google.com/googleplay/android-developer/answer/9842756#zippy=%2Crequest-an-upload-key-reset).
- **Link do App no AI Studio:** [Visualizar App no AI Studio](https://ai.studio/apps/43ed04be-2842-4fe6-b1f0-31e3b5b6531a)
