# Segurança do VelarCall

Resumo honesto do que protege o app, do que **não** dá para impedir, e como está o backend.

## O que dá e o que não dá para impedir

- **Não dá para impedir que alguém baixe o APK e descompile o app.** Isso vale para qualquer app Android. Ferramentas públicas leem o código de qualquer APK. O que dá para fazer é **dificultar** e **limitar o estrago**.
- **Ofuscação (R8):** as versões de release são compiladas com R8 (`isMinifyEnabled = true`), que renomeia classes e métodos e remove código morto. Isso torna o código descompilado bem mais difícil de ler. É o teto realista sem serviços de atestado do Google.
- **Anti-clonagem/anti-abuso de verdade** exigiria o **Play Integrity API** (Google), que só funciona com o app na Play Store e com os serviços Google Play. Enquanto o app for distribuído fora da Play Store, isso não está disponível.

## A chave de assinatura é o ativo mais importante

- Só quem tem `velarcall-release.jks` + a senha consegue publicar uma atualização que o Android aceite instalar por cima da versão existente. **Guarde esse arquivo e a senha fora do GitHub** (gerenciador de senhas / backup offline).
- O Android **recusa** instalar uma atualização assinada com outra chave. Ou seja, mesmo que alguém publique um APK falso "VelarCall", ele **não** consegue atualizar o app de quem já tem o original.
- Reforço no app: antes de instalar uma atualização automática, o VelarCall confere se o APK baixado tem a **mesma assinatura** do app instalado; se não tiver, recusa.

## Backend (Supabase)

- O app **não guarda nenhuma chave do Supabase**. Todo acesso passa por duas Edge Functions públicas: `report` (recebe denúncia) e `blocklist` (devolve a lista agregada).
- A tabela `reports` tem **RLS ligada e sem políticas**: ninguém lê nem escreve direto nela. Só as funções, que usam a *service role* no servidor, acessam.
- A view `public_blocklist` usa `security_invoker = true` e teve o acesso de `anon`/`authenticated` revogado: não é consultável direto, só pela função.
- **Moderação automática:** um número só entra na lista pública depois de **3 aparelhos distintos** denunciarem. Denúncia repetida do mesmo aparelho não conta.
- **Limites de abuso:** no máximo 50 denúncias por aparelho por dia. O "token" que o app envia é apenas uma barreira leve (dá para extrair do APK) — a proteção real é o limiar de 3 aparelhos + os limites.

### Limitação conhecida

Um clone do app poderia reaproveitar o token e enviar denúncias forjando vários "aparelhos". O limiar de 3 reduz o impacto, mas não elimina. A solução completa é o Play Integrity (depende da Play Store). Por isso a lista tem **moderação automática por limiar** e pode, no futuro, ganhar **moderação manual** (aprovar/remover) se necessário.

## Dados no aparelho

- Listas, histórico e preferências ficam só no aparelho, em armazenamento privado do app (outros apps não acessam sem root). Nenhum dado das chamadas é enviado para fora.

## Relatar uma vulnerabilidade

Escreva para natanaelnetfibra@gmail.com com os detalhes. Não abra uma issue pública para falhas de segurança.
