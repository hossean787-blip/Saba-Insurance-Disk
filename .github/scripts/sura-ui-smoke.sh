#!/usr/bin/env bash
set -euo pipefail
mkdir -p ui-artifacts
PACKAGE="ir.saba.insurancedisk"
APK="app/build/outputs/apk/debug/app-debug.apk"
adb install -r "$APK"

# Assertions test the screen itself, not merely the tap response.
verify_screen() {
  local expected="$1"
  local attempts=0
  while [ "$attempts" -lt 12 ]; do
    adb shell uiautomator dump /sdcard/sura-window.xml >/dev/null 2>&1 || true
    adb pull /sdcard/sura-window.xml /tmp/sura-window.xml >/dev/null 2>&1 || true
    if python3 - "$expected" <<'PY'
import sys,xml.etree.ElementTree as ET
expected=sys.argv[1]
try:
    root=ET.parse("/tmp/sura-window.xml").getroot()
    nodes=[n.attrib.get("text","") for n in root.iter("node")]
    assert expected in nodes, f"Screen title missing: {expected}"
except Exception:
    raise SystemExit(1)
PY
    then
      return 0
    fi
    attempts=$((attempts+1))
    sleep 1
  done
  echo "FAIL: Expected screen not visible: $expected" >&2
  adb shell dumpsys activity activities | grep -E 'mResumedActivity|topResumedActivity' || true
  return 1
}

tap_tab() {
  local name="$1"
  local expected="$2"
  local output="$3"
  adb shell uiautomator dump /sdcard/sura-window.xml >/dev/null
  adb pull /sdcard/sura-window.xml /tmp/sura-window.xml >/dev/null
  local coordinates
  coordinates=$(python3 - "$name" <<'PY'
import sys,re,xml.etree.ElementTree as ET
name=sys.argv[1]
nodes=[n for n in ET.parse("/tmp/sura-window.xml").iter("node")
       if n.attrib.get("content-desc")==name]
if not nodes:
    raise SystemExit(f"Navigation tab not found: {name}")
bounds=nodes[0].attrib["bounds"]
x1,y1,x2,y2=map(int,re.findall(r"\d+",bounds))
if x2-x1<45 or y2-y1<45:
    raise SystemExit(f"Navigation control has inadequate touch bounds: {bounds}")
print((x1+x2)//2,(y1+y2)//2)
PY
)
  adb shell input tap $coordinates
  verify_screen "$expected"
  adb shell screencap -p "/sdcard/$output.png"
  adb pull "/sdcard/$output.png" "ui-artifacts/$output.png" >/dev/null
}

suite() {
  local suffix="$1"
  adb shell am force-stop "$PACKAGE" || true
  adb shell am start -W -n "$PACKAGE/.MainActivity"
  verify_screen "سورا"
  adb shell screencap -p "/sdcard/$suffix-home.png"
  adb pull "/sdcard/$suffix-home.png" "ui-artifacts/$suffix-01-home.png" >/dev/null

  tap_tab "کارگاه" "مدیریت کارگاه‌ها" "$suffix-02-workshops"
  tap_tab "کارکنان" "مدیریت کارکنان" "$suffix-03-employees"
  tap_tab "لیست‌ها" "لیست بیمه ماهانه" "$suffix-04-lists"
  tap_tab "گزارش" "خروجی و گزارش بیمه" "$suffix-05-report"
  tap_tab "خانه" "سورا" "$suffix-06-home-return"
}

# Compact 320dp portrait: form columns must stack.
adb shell wm size 720x1600
adb shell wm density 360
adb shell settings put system font_scale 1.0
suite compact

# Wider portrait with 125% text: verify labels and large text remain navigable.
adb shell wm size 900x1800
adb shell wm density 360
adb shell settings put system font_scale 1.25
suite large-text

adb logcat -d -s AndroidRuntime:E > ui-artifacts/android-runtime-errors.txt
if grep -q "FATAL EXCEPTION" ui-artifacts/android-runtime-errors.txt; then
  cat ui-artifacts/android-runtime-errors.txt
  exit 1
fi
echo "PASS: 5-tab navigation and UI headings on compact and large-text screens" > ui-artifacts/smoke-result.txt
