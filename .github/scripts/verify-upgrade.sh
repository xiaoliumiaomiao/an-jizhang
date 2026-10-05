#!/usr/bin/env bash
set -euo pipefail

# Only synthetic data on this job's temporary AVD. Never use a physical device.
artifacts=upgrade-artifacts
mkdir -p "$artifacts"
finish() {
  result=$?
  printf 'Upgrade validation exit: %s\n' "$result" | tee "$artifacts/result.txt"
  trap - EXIT
  exit "$result"
}
trap finish EXIT
fail() { printf '%s\n' "$*" | tee "$artifacts/error.txt" >&2; exit 1; }
[[ "$#" -eq 3 ]] || fail 'Usage: verify-upgrade.sh baseline-app.apk current-app.apk current-test.apk'
[[ "${CI:-}" == true ]] || fail 'Upgrade validation is restricted to a temporary CI emulator'
baseline_apk=$1
current_apk=$2
test_apk=$3
app=com.xiaoliumiaomiao.anjizhang
test_package="$app.test"
build_tools="${ANDROID_HOME:?}/build-tools/35.0.0"
for tool in adb timeout; do command -v "$tool" >/dev/null || fail "Missing tool: $tool"; done
for tool in aapt apksigner; do [[ -x "$build_tools/$tool" ]] || fail "Missing tool: $tool"; done

check_apk() {
  local apk=$1 label=$2 package=$3 code=$4 name=$5
  [[ -f "$apk" ]] || fail "Missing APK: $apk"
  timeout 30s "$build_tools/aapt" dump badging "$apk" > "$artifacts/$label-badging.txt"
  local line
  line=$(grep '^package: ' "$artifacts/$label-badging.txt")
  [[ "$line" == "package: name='$package' "* ]] || fail "$label package mismatch"
  if [[ -n "$code" ]]; then
    [[ "$line" == *" versionCode='$code' "* && "$line" == *" versionName='$name'"* ]] || fail "$label version mismatch"
  fi
  timeout 30s "$build_tools/apksigner" verify --print-certs "$apk" > "$artifacts/$label-certificates.txt" 2>&1
  sha256sum "$apk" > "$artifacts/$label-apk-sha256.txt"
}
check_apk "$baseline_apk" baseline "$app" 3 0.1.2
check_apk "$current_apk" current "$app" 6 0.1.4
check_apk "$test_apk" test "$test_package" '' ''
certificate() {
  local file=$1 digest
  digest=$(sed -n 's/^Signer #[0-9]* certificate SHA-256 digest: //p' "$file")
  [[ "$digest" =~ ^[[:xdigit:]]{64}$ ]] || fail 'Expected exactly one verified public SHA-256 signer certificate'
  printf '%s' "${digest,,}"
}
baseline_cert=$(certificate "$artifacts/baseline-certificates.txt")
current_cert=$(certificate "$artifacts/current-certificates.txt")
test_cert=$(certificate "$artifacts/test-certificates.txt")
[[ "$baseline_cert" == "$current_cert" && "$baseline_cert" == "$test_cert" ]] || fail 'APK signing certificates differ'
printf 'Same public signer SHA-256: %s\n' "$baseline_cert" | tee "$artifacts/certificate-comparison.txt"

# Device discovery cannot select a serial; every subsequent device command does.
timeout 10s adb devices -l > "$artifacts/devices.txt"
mapfile -t devices < <(tr -d '\r' < "$artifacts/devices.txt" | awk 'NR > 1 && NF {print $1 " " $2}')
[[ "${#devices[@]}" -eq 1 && "${devices[0]}" =~ ^(emulator-[0-9]+)\ device$ ]] || fail 'Expected exactly one online emulator and no other devices'
serial=${BASH_REMATCH[1]}
timeout 10s adb -s "$serial" shell getprop ro.kernel.qemu > "$artifacts/qemu.txt"
[[ "$(tr -d '\r\n' < "$artifacts/qemu.txt")" == 1 ]] || fail 'Device is not a QEMU emulator'
timeout 10s adb -s "$serial" emu avd name > "$artifacts/avd-name.txt"
[[ "$(head -n 1 "$artifacts/avd-name.txt" | tr -d '\r')" == ci-ledger ]] || fail 'Device is not the temporary ci-ledger AVD'
printf 'Validated temporary serial: %s\n' "$serial" | tee "$artifacts/serial.txt"

install_apk() {
  local label=$1
  shift
  timeout 60s adb -s "$serial" install "$@" 2>&1 | tee "$artifacts/$label-install-raw.txt"
  tr -d '\r' < "$artifacts/$label-install-raw.txt" > "$artifacts/$label-install.txt"
  grep -qx 'Success' "$artifacts/$label-install.txt" || fail "$label installation did not report Success"
}
run_method() {
  local method=$1 phase=$2 output="$artifacts/$2-instrumentation.txt"
  timeout 120s adb -s "$serial" shell am instrument -w -r \
    -e class "$app.LedgerUpgradeTest#$method" \
    "$test_package/androidx.test.runner.AndroidJUnitRunner" 2>&1 | tee "$artifacts/$phase-instrumentation-raw.txt"
  tr -d '\r' < "$artifacts/$phase-instrumentation-raw.txt" > "$output"
  # adb can exit 0 after JUnit/runner failure; inspect the actual runner protocol.
  if grep -qiE 'FAILURES|INSTRUMENTATION_FAILED|INSTRUMENTATION_ABORTED|Process crashed|shortMsg=|INSTRUMENTATION_STATUS: Error=|INSTRUMENTATION_STATUS_CODE: -[2-9]' "$output"; then
    fail "$phase instrumentation reported failure or a crash"
  fi
  [[ "$(grep -cx 'OK (1 test)' "$output")" -eq 1 ]] || fail "$phase did not execute exactly one passing test"
  [[ "$(grep -c '^INSTRUMENTATION_CODE:' "$output")" -eq 1 ]] || fail "$phase reported multiple or missing runner completion codes"
  [[ "$(grep -cx 'INSTRUMENTATION_CODE: -1' "$output")" -eq 1 ]] || fail "$phase runner did not finish successfully"
}

install_apk baseline "$baseline_apk"
install_apk test -t "$test_apk"
run_method seedOldVersionLedger seed
timeout 15s adb -s "$serial" shell am force-stop "$app" 2>&1 | tee "$artifacts/force-stop.txt"
# No uninstall, clear, downgrade flag, or connected task is allowed between phases.
install_apk current -r "$current_apk"
run_method verifyUpgradedLedger verify
timeout 30s adb -s "$serial" shell am start -W -n "$app/.MainActivity" 2>&1 | tee "$artifacts/launch-raw.txt"
tr -d '\r' < "$artifacts/launch-raw.txt" > "$artifacts/launch.txt"
grep -qx 'Status: ok' "$artifacts/launch.txt" || fail 'Upgraded app launch did not report Status: ok'
if grep -qiE 'Error:|Exception|unable to resolve' "$artifacts/launch.txt"; then fail 'Upgraded app launch failed'; fi
printf 'PASS: synthetic ledger survived 0.1.2/code3 -> 0.1.4/code6 replacement\n' | tee "$artifacts/summary.txt"
