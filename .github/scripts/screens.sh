#!/usr/bin/env bash
# Roda no emulador: instala o APK, cria contatos, simula ligações e tira capturas.
set -u
PKG=com.velarlabs.velarcall
OUT=shots
mkdir -p "$OUT"
RES="$OUT/resultados.txt"
: > "$RES"

shot() { sleep 2; adb shell screencap -p /sdcard/s.png; adb pull -q /sdcard/s.png "$OUT/$1.png" >/dev/null; }

dismiss_anr() {
  # Fecha diálogos "não está respondendo" do próprio emulador.
  adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1
  adb pull -q /sdcard/ui.xml /tmp/ui.xml >/dev/null 2>&1
  if grep -q "isn't responding" /tmp/ui.xml 2>/dev/null; then
    echo "emulador: fechando diálogo ANR" >> "$RES"
    tap_once "Wait"
  fi
}

tap_text() {
  local i
  for i in 1 2 3; do
    dismiss_anr
    tap_once "$1" && return 0
    sleep 3
  done
  echo "texto nao encontrado: $1" >> "$RES"
}

tap_once() {
  adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1
  adb pull -q /sdcard/ui.xml /tmp/ui.xml >/dev/null 2>&1
  local xy
  xy=$(python3 - "$1" <<'PY'
import re, sys
t = sys.argv[1]
xml = open('/tmp/ui.xml', encoding='utf-8').read()
for m in re.finditer(r'<node [^>]*>', xml):
    n = m.group(0)
    tx = re.search(r' text="([^"]*)"', n); cd = re.search(r'content-desc="([^"]*)"', n)
    if (tx and tx.group(1) == t) or (cd and cd.group(1) == t):
        b = re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', n)
        x1, y1, x2, y2 = map(int, b.groups())
        print((x1 + x2) // 2, (y1 + y2) // 2); break
PY
)
  if [ -n "$xy" ]; then adb shell input tap $xy; sleep 2; return 0; fi
  return 1
}

add_contact() {
  adb shell "content insert --uri content://com.android.contacts/raw_contacts --bind account_type:s:local --bind account_name:s:local"
  local id
  id=$(adb shell "content query --uri content://com.android.contacts/raw_contacts --projection _id" | tail -1 | sed -E 's/.*_id=([0-9]+).*/\1/' | tr -d '\r')
  adb shell "content insert --uri content://com.android.contacts/data --bind raw_contact_id:i:$id --bind mimetype:s:vnd.android.cursor.item/name --bind data1:s:'$1'"
  adb shell "content insert --uri content://com.android.contacts/data --bind raw_contact_id:i:$id --bind mimetype:s:vnd.android.cursor.item/phone_v2 --bind data1:s:'$2' --bind data2:i:2"
}

call_state() { adb shell dumpsys telephony.registry | grep -m1 "mCallState" | tr -d '\r' | sed -E 's/.*mCallState=([0-9]).*/\1/'; }

test_call() { # numero esperado(TOCA|BLOQUEIA) descricao
  adb emu gsm call "$1" >/dev/null
  sleep 6
  local st; st=$(call_state)
  local got="BLOQUEIA"; [ "$st" = "1" ] && got="TOCA"
  local ok="OK"; [ "$got" = "$2" ] || ok="FALHOU"
  echo "$ok | $3 | $1 | esperado=$2 obtido=$got (callState=$st)" >> "$RES"
  adb emu gsm cancel "$1" >/dev/null
  sleep 3
}

adb shell settings put global window_animation_scale 0
sleep 20
adb install -r app/build/outputs/apk/debug/app-debug.apk

add_contact "Ana Souza" "11999990000"
add_contact "Bruno Lima" "21988887777"
add_contact "Carla Mendes" "31977776666"
add_contact "Diego Rocha" "41966665555"
add_contact "Elisa Prado" "51955554444"

# 1) primeira abertura sem permissões
adb shell am start -W -n $PKG/.ui.MainActivity >/dev/null
shot 01-boas-vindas

# 2) concede tudo e conclui
adb shell pm grant $PKG android.permission.READ_CONTACTS
adb shell pm grant $PKG android.permission.POST_NOTIFICATIONS
adb shell cmd role add-role-holder android.app.role.CALL_SCREENING $PKG 0
echo "role: $(adb shell cmd role get-role-holders android.app.role.CALL_SCREENING | tr -d '\r')" >> "$RES"
adb shell am force-stop $PKG
adb shell am start -W -n $PKG/.ui.MainActivity >/dev/null
shot 02-boas-vindas-pronto
tap_text "Começar"
shot 03-inicio-vazio

# 3) ligações simuladas
adb shell input keyevent KEYCODE_HOME
test_call 11987654321 BLOQUEIA "desconhecido"
test_call 11999990000 TOCA "contato da agenda"
test_call 03031234567 BLOQUEIA "telemarketing 0303"
test_call 11987654321 TOCA "mesmo desconhecido de novo (insistência)"
test_call 21933332222 BLOQUEIA "outro desconhecido"

# 4) telas com dados
adb shell am start -W -n $PKG/.ui.MainActivity >/dev/null
shot 04-inicio
tap_text "Bloqueadas"; shot 05-bloqueadas
tap_text "Agenda"; shot 06-agenda
tap_text "Bruno Lima"; shot 07-contato
tap_text "Bloquear contato"; adb shell input keyevent KEYCODE_BACK; sleep 1
test_call 21988887777 BLOQUEIA "contato bloqueado pela aba Agenda"
adb shell am start -W -n $PKG/.ui.MainActivity >/dev/null
shot 08-agenda-com-bloqueio
tap_text "Listas"; shot 09-listas
tap_text "Ajustes"; shot 10-ajustes

# 5) tema escuro (inclui boas-vindas)
adb shell cmd uimode night yes
adb shell pm clear $PKG >/dev/null
adb shell pm grant $PKG android.permission.READ_CONTACTS
adb shell pm grant $PKG android.permission.POST_NOTIFICATIONS
adb shell cmd role add-role-holder android.app.role.CALL_SCREENING $PKG 0
adb shell am start -W -n $PKG/.ui.MainActivity >/dev/null
shot 11a-boas-vindas-escuro
tap_text "Começar"
shot 11-inicio-escuro
tap_text "Agenda"; shot 12-agenda-escuro

cat "$RES"
