# Configuração do login do MVP

O aplicativo usa Firebase Authentication para acesso e Cloud Firestore para registrar usuários e telemetria da importação por IA. A análise dos documentos é executada no backend; nenhuma chave de provedor de IA deve ser incluída no aplicativo.

O cliente envia o ID token Firebase para o backend nas análises. A fonte de custo por usuário agora são as coleções `backendUsageOperations`, `backendUsageAttempts` e `backendUsageQuotas`, gravadas exclusivamente pelo backend. A subcoleção `aiUsage` abaixo é apenas telemetria do aplicativo. As regras atuais já negam acesso direto às novas coleções por padrão. A configuração do servidor está em `../Backend/RELEASE_TEST.md`; nunca coloque credenciais Firebase Admin no aplicativo.

## Ativar no Firebase

1. No Firebase Console, crie ou selecione o projeto da Agenda Acadêmica.
2. Cadastre o aplicativo Android com o pacote `com.aistudio.academicagenda.app`.
3. Baixe `google-services.json` e coloque o arquivo em `app/google-services.json`.
4. Em Authentication > Sign-in method, habilite **E-mail/senha**.
5. Crie um banco Cloud Firestore.
6. Na raiz deste projeto, autentique o Firebase CLI e publique as regras com `firebase deploy --only firestore:rules`.

## Dados registrados

- `users/{uid}`: e-mail, criação, último acesso, plataforma e versão instalada.
- `users/{uid}/aiUsage/{id}`: quantidade e tamanho dos arquivos, duração, resultado e categoria segura de erro.

As disciplinas, aulas, notas e faltas continuam armazenadas localmente no aparelho e não são copiadas para o Firestore.

## Conferência antes do teste

- Criar uma conta pelo aplicativo.
- Confirmar o documento em `users` no Firestore.
- Processar um documento e confirmar um registro na subcoleção `aiUsage`.
- Sair e entrar novamente para validar a persistência da sessão.
