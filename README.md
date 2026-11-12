<p align="center"><img src="docs/logo.png" width="120" alt="VelarCall"></p>

# VelarCall

App Android da **Velar Labs** que identifica e bloqueia ligações indesejadas: quem está fora da sua agenda, números ocultos, telemarketing e números suspeitos de uma lista online.

## Como funciona

O VelarCall usa a API oficial de triagem de chamadas do Android (`CallScreeningService`). Depois que você o define como **app de identificação de chamadas e spam**, o Android consulta o app a cada ligação recebida, antes de o telefone tocar.

Regras, em ordem de prioridade:

| # | Regra | Resultado |
|---|-------|-----------|
| 1 | Número oculto | bloqueia (opcional) |
| 2 | Origem falsificada, quando a operadora informa (Android 11+) | bloqueia (opcional) |
| 3 | Lista **liberada** do app | toca |
| 4 | Lista **bloqueada** do app (vale até para contatos) | bloqueia |
| 5 | Contato da agenda | toca |
| 6 | Lista online / prefixos suspeitos | bloqueia (opcional) |
| 7 | Prefixo 0303 (telemarketing) | bloqueia (opcional) |
| 8 | Mesmo desconhecido ligando de novo em até 3 min | toca (opcional, ligado por padrão) |
| 9 | Fora da agenda | bloqueia (opcional, ligado por padrão) |

Na aba **Agenda** dá para bloquear ou liberar qualquer contato com um toque.

> Sem a permissão de agenda, a regra 9 fica desativada de propósito. Sem essa permissão o app não sabe quem é contato e acabaria bloqueando conhecidos.

Toda a decisão acontece no aparelho. Nenhum dado das suas chamadas sai do celular. A única conexão de rede é o download da lista online.

## Correio de voz (aba Recados)

Em vez de recusar na hora, o VelarCall pode **silenciar** a chamada bloqueada e deixá-la cair no **correio de voz da operadora**, onde quem ligou deixa um recado. Você ouve pelo correio de voz e liga de volta pela aba **Recados**. A opção vem ligada e pode ser desligada na própria aba. Nenhum áudio é gravado pelo app: o Android não permite que apps de terceiros gravem a chamada; o recado fica no correio de voz da operadora.

## Lista online

O arquivo [`blocklist/numeros.json`](blocklist/numeros.json) deste repositório é a lista padrão. O app baixa essa lista a cada 12 h. O endereço pode ser trocado em **Ajustes**.

Formatos aceitos:

```json
{
  "prefixes": ["0303"],
  "numbers": [
    { "n": "11912345678", "label": "Golpe do falso banco" },
    "21987654321"
  ]
}
```

Também aceita um arquivo de texto simples, com um número por linha (`numero;rotulo`). Linhas que começam com `#` são comentários.

Inclua só números com denúncia confirmada. Um número legítimo na lista pública fica bloqueado para todos os usuários.

## Requisitos

- Android 10 (API 29) ou superior
- Permissões: agenda (obrigatória para o bloqueio de desconhecidos) e notificações (opcional)

## Publicar uma versão

Altere o número no arquivo [`VERSION`](VERSION) na `main`. O workflow **Release** compila, assina e publica o APK em Releases. Exige os secrets `KEYSTORE_BASE64` e `KEYSTORE_PASSWORD`.

## Compilar

```bash
./gradlew assembleDebug
# APK em app/build/outputs/apk/debug/
```

Cada push na `main` gera o APK de debug no GitHub Actions. Ele fica em **Actions → Build APK → Artifacts**.

## Estrutura

```
app/src/main/java/com/velarlabs/velarcall/
├── screening/VelarScreeningService.kt   # recebe cada ligação e decide
├── data/CallDecider.kt                  # regras de bloqueio
├── data/VelarDb.kt                      # listas e histórico (SQLite)
├── data/PhoneNumbers.kt                 # normalização de números BR
├── sync/BlocklistSync.kt                # download da lista online
└── ui/                                  # telas em Jetpack Compose
```
