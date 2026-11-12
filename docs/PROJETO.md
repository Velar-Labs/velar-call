<p align="center"><img src="logo.png" width="110" alt="VelarCall"></p>

# VelarCall — Documento do Projeto

Aplicativo Android da **Velar Labs** que identifica e bloqueia chamadas indesejadas: quem está fora da agenda, números ocultos, telemarketing e números de uma lista compartilhada na internet. Inspirado em apps como o Whoscall, mas com todo o processamento feito **no próprio aparelho**.

- **Repositório:** `Velar-Labs/velar-call`
- **Plataforma:** Android 10 (API 29) ou superior
- **Tecnologia:** Kotlin + Jetpack Compose (Material 3)
- **Versão atual:** 0.5.0
- **Licença de uso dos dados:** nenhuma informação das suas chamadas sai do celular

---

## 1. O que o app faz

O VelarCall usa a API oficial de triagem de chamadas do Android (`CallScreeningService`). Depois de ser definido como **app de identificação de chamadas e spam**, o Android o consulta a cada ligação recebida, **antes de o telefone tocar**, e o app decide em frações de segundo o que fazer.

### Regras de decisão (ordem de prioridade)

Um contato da agenda **sempre toca**, a não ser que você mesmo o tenha bloqueado.

| # | Situação | Resultado |
|---|----------|-----------|
| 1 | Proteção desligada | permite |
| 2 | Número oculto | bloqueia só se você ativar (padrão: desligado) |
| 3 | Número na sua lista **liberada** | permite |
| 4 | Número **bloqueado por você** | bloqueia (vale até para contatos) |
| 5 | **Contato da agenda** | permite — antes de qualquer bloqueio automático |
| 6 | Origem falsificada (operadora) | bloqueia só se você ativar (padrão: desligado) |
| 7 | Número/prefixo da **lista online** | bloqueia (se ativado) |
| 8 | Prefixo de telemarketing **0303** | bloqueia (se ativado) |
| 9 | Mesmo desconhecido ligando de novo em 3 min | permite (se ativado) |
| 10 | **Fora da agenda** | bloqueia (se ativado e com acesso à agenda) |

> Sem permissão de agenda, a regra 10 fica desativada de propósito: sem ela o app não sabe quem é contato e barraria conhecidos.

### Casamento de número robusto

O número que chega da operadora (ex.: `+5511988887777`) é normalizado pelo próprio app e comparado ao número salvo na agenda (ex.: `(11) 98888-7777`). Isso resolve o caso clássico em que um contato era bloqueado por diferença de formato.

---

## 2. Telas (abas)

- **Início** — estado da proteção, contadores do dia/total, ações rápidas (consultar número, bloquear, agenda, atualizar lista) e as últimas chamadas bloqueadas.
- **Recados** — o que fazer ao bloquear uma chamada: **Correio de voz** (silencia e deixa cair no correio de voz da operadora, para a pessoa deixar recado) ou **Rejeitar** (recusa na hora). Botão para ouvir o correio de voz, ligar de volta num toque e histórico marcando o que foi ao correio.
- **Agenda** — lista os contatos do telefone, com busca e filtro "Bloqueados". Um toque bloqueia ou libera o contato.
- **Listas** — números liberados e bloqueados manualmente, com busca e "Desfazer" ao remover.
- **Ajustes** — todos os interruptores de bloqueio, a lista online (endereço e sincronização), notificações e a tela **"Última chamada analisada"** para diagnóstico no aparelho.

---

## 3. Correio de voz — o que é possível e o que não é

- **Possível:** em vez de recusar, o app **silencia** a chamada e a deixa cair no **correio de voz da operadora**, onde quem ligou deixa um recado. Você ouve pelo correio de voz e liga de volta pela aba Recados.
- **Não é possível:** gravar a voz de quem liga **dentro do app**. O Android não entrega o áudio da chamada a apps de terceiros (bloqueado desde o Android 10), nem mesmo ao app de telefone padrão de forma confiável. O recado de voz fica na operadora, não no VelarCall.

---

## 4. Lista online (números compartilhados)

O app baixa, a cada 12 horas, um arquivo [`blocklist/numeros.json`](../blocklist/numeros.json) do próprio repositório. O endereço pode ser trocado em Ajustes.

```json
{
  "prefixes": ["0303"],
  "numbers": [
    { "n": "11912345678", "label": "Golpe do falso banco" },
    "21987654321"
  ]
}
```

Também aceita texto simples (um número por linha, `numero;rotulo`, `#` para comentário). **Inclua só números com denúncia confirmada** — um número legítimo na lista pública fica bloqueado para todos.

---

## 5. Privacidade

- Toda a decisão de bloqueio acontece **no aparelho**.
- A **única** conexão de rede é o download da lista online.
- O app não pede permissão de registro de chamadas nem envia dados das suas ligações.
- Permissões usadas: **agenda** (para saber quem é contato) e **notificações** (opcional, para avisar).

---

## 6. Arquitetura do código

```
app/src/main/java/com/velarlabs/velarcall/
├── screening/VelarScreeningService.kt   # recebe cada ligação, decide e registra diagnóstico
├── data/
│   ├── CallDecider.kt                    # regras de bloqueio (a lógica central)
│   ├── VelarDb.kt                        # listas e histórico (SQLite, v2)
│   ├── ContactsRepo.kt                   # leitura da agenda
│   ├── PhoneNumbers.kt                   # normalização de números brasileiros
│   ├── Settings.kt                       # preferências (SharedPreferences)
│   └── ...
├── sync/BlocklistSync.kt                 # download da lista online (WorkManager, 12 h)
└── ui/                                   # telas em Jetpack Compose
    ├── VelarApp.kt                       # navegação, barra inferior, boas-vindas
    ├── Theme.kt / Components.kt          # tema claro/escuro e componentes
    ├── HomeScreen / RecadosScreen / ContactsScreen / ListsScreen / SettingsScreen
    └── Sheets.kt                         # folhas de ação e diálogos
```

---

## 7. Compilar, testar e publicar

### Compilar localmente
```bash
./gradlew assembleDebug      # APK de debug em app/build/outputs/apk/debug/
```

### Integração contínua (GitHub Actions)
- **Build APK** — a cada push, compila e roda os testes.
- **Screenshots** — a cada push na branch `ci/screenshots`, roda o app num emulador Android 14, **simula ligações reais** (contato, desconhecido, 0303, +55 vs nacional, etc.), confere o resultado e publica as capturas na branch `screenshots`.
- **Release** — ao mudar o arquivo [`VERSION`](../VERSION) na `main`, compila o **APK assinado** e publica em *Releases*.

### Publicar uma versão
1. Edite o arquivo `VERSION` (ex.: `0.6.0`) na `main`.
2. O workflow **Release** gera e publica o APK assinado sozinho.

Exige os secrets `KEYSTORE_BASE64` e `KEYSTORE_PASSWORD` (chave de assinatura — guarde o `.jks` fora do GitHub; sem ela não há como atualizar o app por cima).

---

## 8. Histórico de versões

| Versão | O que trouxe |
|--------|--------------|
| 0.1.0 | Primeira versão: triagem, listas, bloqueio de desconhecidos/ocultos/0303, lista online, CI |
| 0.2.1 | Correção de texto invisível no tema escuro |
| 0.3.0 | Correção do bloqueio de contatos; contatos sempre tocam antes dos bloqueios automáticos; diagnóstico no aparelho; padrões mais seguros |
| 0.4.0 | Correio de voz: silencia e deixa cair no correio da operadora; aba Recados |
| 0.5.0 | Escolha entre "Correio de voz" e "Rejeitar" ao bloquear |

---

## 9. Limites do Android (o que um app de terceiros não pode fazer)

- Gravar a voz da outra ponta da ligação (correio de voz interno).
- Mostrar "Provável golpe" na própria tela de chamada sem virar o app de telefone padrão.
- Garantir o bloqueio de número oculto sem o risco de barrar contatos em aparelhos que não entregam o número ao app.

---

## 10. Próximos passos (roadmap)

1. **Botão "Denunciar número"** alimentando um banco compartilhado (ex.: Supabase) com moderação — é o que faz a lista online ter valor real.
2. **Backup das listas** (exportar/importar) para trocar de celular sem perder nada.
3. **Bloqueio por padrão** (ex.: todos os 0800, DDDs específicos) e **horário silencioso**.
4. Preparação para a **Play Store** (revisão de permissões).

---

*Velar Labs — VelarCall. Documento gerado em 11/11/2026.*
