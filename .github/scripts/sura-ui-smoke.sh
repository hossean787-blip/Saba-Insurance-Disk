#!/usr/bin/env bash
set -euo pipefail
mkdir -p ui-artifacts
PACKAGE="ir.saba.insurancedisk"
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -W -n "$PACKAGE/.MainActivity"
sleep 4
adb shell screencap -p /sdcard/sura-home.png
adb pull /sdcard/sura-home.png ui-artifacts/01-home.png >/dev/null

tap_tab() {
  local text="$1"
  local output="$2"
  adb shell uiautomator dump /sdcard/window.xml >/dev/null
  adb pull /sdcard/window.xml /tmp/sura-window.xml >/dev/null
  local coordinates
  coordinates=$(python3 - "$text" <<'PY'
import re,sys,xml.etree.ElementTree as ET
name=sys.argv[1]
tree=ET.parse("/tmp/sura-window.xml")
nodes=[n for n in tree.iter("node") if n.attrib.get("content-desc")==name]
if not nodes:
    print("")
else:
    bounds=nodes[0].attrib["bounds"]
    x1,y1,x2,y2=map(int,re.findall(r"\d+",bounds))
    print((x1+x2)//2,(y1+y2)//2)
PY
)
  if [ -z "$coordinates" ]; then
    echo "ERROR: Navigation tab $text was not found" >&2
    exit 1
  fi
  adb shell input tap $coordinates
  sleep 2
  adb shell screencap -p "/sdcard/sura-$output.png"
  adb pull "/sdcard/sura-$output.png" "ui-artifacts/$output.png" >/dev/null
}

tap_tab "کارگاه" "02-workshops"
tap_tab "کارکنان" "03-employees"
tap_tab "لیست‌ها" "04-lists"
tap_tab "گزارش" "05-report"
tap_tab "خانه" "06-home-return"

adb logcat -d -s AndroidRuntime:E > ui-artifacts/android-runtime-errors.txt
if grep -q "FATAL EXCEPTION" ui-artifacts/android-runtime-errors.txt; then
  echo "Android runtime exception detected"
  cat ui-artifacts/android-runtime-errors.txt
  exit 1
fi
echo "UI tabs and screenshots: PASS" > ui-artifacts/smoke-result.txt
