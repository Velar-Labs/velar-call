<p align="center"><img src="logo.png" width="110" alt="VelarCall"></p>

# VelarCall — Documento do Projeto

Aplicativo Android da **Velar Labs** que identifica e bloqueia chamadas indesejadas: quem está fora da agenda, números ocultos, telemarketing e números de uma lista compartilhada na internet. Inspirado em apps como o Whoscall, mas com todo o processamento feito **no próprio aparelho**.

- **Repositório:** `Velar-Labs/velar-call`
- **Plataforma:** Android 10 (API 29) ou superior
- **Tecnologia:** Kotlin + Jetpack Compose (Material 3)
- **Versão atual:** 0.8.0
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
| 9 | **Prefixo que você bloqueou** (0800, DDD, etc.) | bloqueia |
| 10 | **Horário silencioso** (dias e faixa que você definir) | bloqueia |
| 11 | Mesmo desconhecido ligando de novo em 3 min | permite (se ativado) |
| 12 | **Fora da agenda** | bloqueia (se ativado e com acesso à agenda) |

> Sem permissão de agenda, a regra 12 fica desativada de propósito: sem ela o app não sabe quem é contato e barraria conhecidos.

### Casamento de número robusto

O número que chega da operadora (ex.: `+5511988887777`) é normalizado pelo próprio app e comparado ao número salvo na agenda (ex.: `(11) 98888-7777`). Isso resolve o caso clássico em que um contato era bloqueado por diferença de formato.

---

## 2. Telas (abas)

- **Início** — estado da proteção, contadores do dia/total, ações rápidas (consultar número, bloquear, agenda, atualizar lista), um **resumo dos últimos 7 dias** (mini gráfico por dia e motivos mais comuns) e as últimas chamadas bloqueadas.
- **Recados** — o que fazer ao bloquear uma chamada: **Correio de voz** (silencia e deixa cair no correio de voz da operadora, para a pessoa deixar recado) ou **Rejeitar** (recusa na hora). Botão para ouvir o correio de voz, ligar de volta num toque e histórico marcando o que foi ao correio. Tocar numa chamada abre o **detalhe daquela ligação** (motivo, diagnóstico e ações).
- **Agenda** — lista os contatos do telefone, com busca e filtro "Bloqueados". Um toque bloqueia ou libera o contato.
- **Listas** — números liberados e bloqueados manualmente, com busca e "Desfazer" ao remover.
- **Ajustes** — interruptores de bloqueio, **bloqueio por prefixo** (0800/0300/0500/0900 ou personalizado), **horário silencioso** (faixa e dias da semana), lista online, notificações, **atualizações** (verificar/instalar nova versão), **backup e restauração** das listas e a tela **"Última chamada analisada"** para diagnóstico.

Há ainda um **atalho nas Configurações Rápidas do Android** para ligar/desligar a proteção sem abrir o app.

---

## 3. Correio de voz — o que é possível e o que não é

- **Possível:** em vez de recusar, o app **silencia** a chamada e a deixa cair no **correio de voz da operadora**, onde quem ligou deixa um recado. Você ouve pelo correio de voz e liga de volta pela aba Recados.
- **Não é possível:** gravar a voz de quem liga **dentro do app**. O Android não entrega o áudio da chamada a apps de terceiros (bloqueado desde o Android 10), nem mesmo ao app de telefone padrão de forma confiável. O recado de voz fica na operadora, não no VelarCall.

---

## 3b. Atualização dentro do app

O app verifica sozinho (uma vez por dia) se há versão nova no **GitHub Releases**, avisa por notificação e, em **Ajustes → Atualizações**, baixa e instala com um toque (via `PackageInstaller`).

- **Possível:** verificar, baixar e iniciar a instalação sem o usuário procurar nada.
- **Não é possível (fora da Play Store):** instalar de forma 100% silenciosa. O Android pede uma confirmação ao atualizar, e na primeira vez pede para permitir instalar apps do VelarCall. O auto-update só funciona entre versões assinadas com a **mesma chave** (as dos Releases).
- **100% automático** só publicando na Play Store (o AAB já sai pronto em cada release).

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
├── screening/
│   ├── VelarScreeningService.kt          # recebe cada ligação, decide e registra diagnóstico
│   ├── VelarTileService.kt               # atalho nas Configurações Rápidas
│   └── InstallResultReceiver.kt          # resultado da instalação da atualização
├── data/
│   ├── CallDecider.kt                    # regras de bloqueio (a lógica central)
│   ├── VelarDb.kt                        # listas e histórico (SQLite, v3)
│   ├── ContactsRepo.kt                   # leitura da agenda
│   ├── PhoneNumbers.kt                   # normalização de números brasileiros
│   ├── Settings.kt                       # preferências (SharedPreferences)
│   ├── QuietHours.kt                     # lógica do horário silencioso (testável)
│   ├── Backup.kt                         # exportar/importar listas em JSON
│   └── ...
├── sync/
│   ├── BlocklistSync.kt                  # download da lista online (WorkManager, 12 h)
│   ├── Updater.kt                        # verifica/baixa/instala nova versão (GitHub Releases)
│   └── UpdateNotifier.kt                 # notificação de versão nova
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
- **Release** — ao mudar o arquivo [`VERSION`](../VERSION) na `main`, compila o **APK e o AAB assinados** (AAB para a Play Store) e publica em *Releases*.

### Publicar uma versão
1. Edite o arquivo `VERSION` (ex.: `0.6.0`) na `main`.
2. O workflow **Release** gera e publica o APK e o AAB assinados sozinho.

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
| 0.6.0 | Backup das listas; bloqueio por prefixo; horário silencioso; AAB para a Play Store; política de privacidade e ficha da loja |
| 0.7.0 | Detalhe por chamada; horário silencioso por dia da semana; atalho nas Configurações Rápidas; resumo dos últimos 7 dias |
| 0.8.0 | Atualização dentro do app: verifica, avisa, baixa e instala nova versão do GitHub Releases |

---

## 9. Limites do Android (o que um app de terceiros não pode fazer)

- Gravar a voz da outra ponta da ligação (correio de voz interno).
- Mostrar "Provável golpe" na própria tela de chamada sem virar o app de telefone padrão.
- Garantir o bloqueio de número oculto sem o risco de barrar contatos em aparelhos que não entregam o número ao app.
- Atualizar-se de forma 100% silenciosa fora da Play Store (o sistema sempre pede uma confirmação).

---

## 10. Próximos passos (roadmap)

Já entregues: backup das listas, bloqueio por prefixo, horário silencioso (com dias), AAB e documentos para a Play Store, detalhe por chamada, atalho rápido e estatísticas.

Pendente:

1. **Botão "Denunciar número"** alimentando um banco compartilhado (ex.: Supabase) com moderação — é o que faz a lista online ter valor real. (Adiado a pedido.)
2. **Publicação na Play Store** — depende de uma conta no Google Play Console e de hospedar a política de privacidade numa URL pública; o AAB já sai pronto em cada *Release*.

---

*Velar Labs — VelarCall. Documento atualizado em 11/11/2026 (v0.8.0).*
