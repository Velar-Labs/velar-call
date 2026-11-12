# VelarCall — Ficha para a Play Store

Material pronto para preencher a página do app no Google Play Console.

## Título
VelarCall — Bloqueio de chamadas

## Descrição curta (até 80 caracteres)
Bloqueie golpes, telemarketing e quem não está na sua agenda. Tudo no aparelho.

## Descrição completa
O VelarCall identifica e bloqueia chamadas indesejadas antes de o telefone tocar.

Recursos:
• Bloqueia quem não está na sua agenda nem na sua lista de liberados
• Bloqueia telemarketing (prefixo 0303), números ocultos e origem falsificada
• Bloqueio por prefixo: 0800, 0300 ou um DDD inteiro
• Horário silencioso: só contatos tocam no período que você escolher
• Correio de voz: deixe a chamada bloqueada cair no correio da operadora, ou rejeite na hora
• Lista online de números suspeitos, atualizada sozinha
• Backup das suas listas para trocar de celular sem perder nada

Privacidade em primeiro lugar: toda a análise acontece no seu aparelho. O VelarCall não grava chamadas, não acessa o registro de chamadas e não envia seus contatos ou números para nenhum servidor.

Requer Android 10 ou superior.

## Categoria
Ferramentas / Comunicação

## Permissões e justificativas (para a revisão do Google)

| Permissão | Justificativa |
|-----------|---------------|
| `READ_CONTACTS` | Identificar se quem liga é um contato, para nunca bloquear chamadas conhecidas. Os contatos são lidos só na memória; não saem do aparelho. |
| `ROLE_CALL_SCREENING` (papel, não permissão perigosa) | Função principal do app: triagem de chamadas recebidas. |
| `INTERNET` | Baixar a lista pública de números suspeitos. |
| `POST_NOTIFICATIONS` | Avisar quando uma chamada é bloqueada (opcional). |

O app **não** solicita `READ_CALL_LOG` nem `READ_PHONE_STATE`, o que simplifica a revisão.

## Data safety (Segurança dos dados)
- Coleta de dados: **Nenhuma**.
- Compartilhamento de dados: **Nenhum**.
- Dados processados no aparelho: contatos e chamadas recebidas, usados apenas localmente e não enviados.
- Os dados ficam no aparelho e o usuário pode apagá-los desinstalando o app.

## Link da política de privacidade
Publique o arquivo [`docs/PRIVACY.md`](PRIVACY.md) numa URL pública (ex.: GitHub Pages do repositório) e informe esse endereço no Play Console.

## Arquivo para envio
Use o **AAB** anexado em cada *Release* (`VelarCall-vX.Y.Z.aab`), já assinado com a chave do projeto.
