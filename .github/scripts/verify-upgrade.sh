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
for tool in adb timeout python3; do command -v "$tool" >/dev/null || fail "Missing tool: $tool"; done
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
check_apk "$baseline_apk" baseline "$app" 6 0.1.4
check_apk "$current_apk" current "$app" 10 0.1.8
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
  local class=$1 method=$2 phase=$3 output="$artifacts/$3-instrumentation.txt"
  timeout 120s adb -s "$serial" shell am instrument -w -r \
    -e ledgerCi true -e ledgerUpgradeBaseline 0.1.4 \
    -e class "$app.$class#$method" \
    "$test_package/androidx.test.runner.AndroidJUnitRunner" 2>&1 | tee "$artifacts/$phase-instrumentation-raw.txt"
  tr -d '\r' < "$artifacts/$phase-instrumentation-raw.txt" > "$output"
  # adb can exit 0 after JUnit/runner failure; inspect the actual runner protocol.
  if grep -qiE 'FAILURES|INSTRUMENTATION_FAILED|INSTRUMENTATION_ABORTED|Process crashed|shortMsg=|INSTRUMENTATION_STATUS: Error=|INSTRUMENTATION_STATUS_CODE: -[2-9]' "$output"; then
    fail "$phase instrumentation reported failure or a crash"
  fi
  [[ "$(grep -cx 'OK (1 test)' "$output")" -eq 1 ]] || fail "$phase did not execute exactly one passing test"
  [[ "$(grep -c '^INSTRUMENTATION_CODE:' "$output")" -eq 1 ]] || fail "$phase reported multiple or missing runner completion codes"
  [[ "$(grep -cx 'INSTRUMENTATION_CODE: -1' "$output")" -eq 1 ]] || fail "$phase runner did not finish successfully"
  [[ "$(sed -n 's/^INSTRUMENTATION_STATUS: class=//p' "$output" | sort -u)" == "$app.$class" ]] || fail "$phase executed the wrong class"
  [[ "$(sed -n 's/^INSTRUMENTATION_STATUS: test=//p' "$output" | sort -u)" == "$method" ]] || fail "$phase executed the wrong method"
  [[ "$(sed -n 's/^INSTRUMENTATION_STATUS: numtests=//p' "$output" | sort -u)" == 1 ]] || fail "$phase did not identify exactly one test"
}

install_apk baseline "$baseline_apk"
install_apk test -t "$test_apk"
run_method LedgerRawUpgradeSeedTest seedOldVersionLedger seed
timeout 15s adb -s "$serial" shell am force-stop "$app" 2>&1 | tee "$artifacts/force-stop.txt"
# No uninstall, clear, downgrade flag, or connected task is allowed between phases.
install_apk current -r "$current_apk"
# Cold-launch the real installed app to perform its migration before the readback verifier.
timeout 30s adb -s "$serial" shell am start -W -n "$app/.MainActivity" 2>&1 | tee "$artifacts/launch-raw.txt"
tr -d '\r' < "$artifacts/launch-raw.txt" > "$artifacts/launch.txt"
grep -qx 'Status: ok' "$artifacts/launch.txt" || fail 'Upgraded app launch did not report Status: ok'
if grep -qiE 'Error:|Exception|unable to resolve' "$artifacts/launch.txt"; then fail 'Upgraded app launch failed'; fi
run_method LedgerSchema2UpgradeTest verifyUpgradedLedger verify
# Check the raw instrumentation proofs, including exact legacy data and stage order.
# The frozen verifier must also perform every assertion behind the capabilities flags.
python3 - "$artifacts" <<'PY'
import json
import pathlib
import sys

directory = pathlib.Path(sys.argv[1])
def proofs(phase):
    prefix = 'INSTRUMENTATION_STATUS: ledgerUpgradeProof='
    return [json.loads(line[len(prefix):]) for line in (directory / (phase + '-instrumentation.txt')).read_text(encoding='utf-8').splitlines() if line.startswith(prefix)]

seed, verify = proofs('seed'), proofs('verify')
assert len(seed) == 1 and [item['phase'] for item in seed] == ['seed']
assert len(verify) == 3 and [item['phase'] for item in verify] == ['opened', 'capabilities', 'reopened']
expected = [
    dict(id='01100000-0000-0000-0000-000000000001', kind='INCOME', amountCents=10000, category='工资', date='2026-10-01', note='升级验证收入'),
    dict(id='01100000-0000-0000-0000-000000000002', kind='EXPENSE', amountCents=1, category='自定义分类', date='2026-10-02', note='中文、引号"和换行\n备注'),
    dict(id='01100000-0000-0000-0000-000000000003', kind='EXPENSE', amountCents=1234, category='交通', date='2026-09-30', note='跨月支出'),
]
columns = ['id', 'kind', 'amount_cents', 'category', 'date', 'note']
for proof in seed + verify:
    assert proof['baselineName'] == '0.1.4' and type(proof['baselineCode']) is int and proof['baselineCode'] == 6
for proof, schema, scope in [(seed[0], 1, 'entire-ledger'), (verify[0], 2, 'entire-ledger'), (verify[2], 2, 'legacy-ids-only')]:
    assert type(proof['schema']) is int and proof['schema'] == schema
    assert proof['schemaColumns'] == columns + (['account_id'] if schema == 2 else [])
    assert proof['rowScope'] == scope and type(proof['entryCount']) is int
    assert proof['entryCount'] == 3 if scope == 'entire-ledger' else proof['entryCount'] >= 3
    rows = sorted(proof['rows'], key=lambda row: row['id'])
    assert len(rows) == 3
    for actual, original in zip(rows, expected):
        assert type(actual['amountCents']) is int
        assert {key: actual[key] for key in original} == original
        if schema == 2:
            assert 'accountId' in actual and actual['accountId'] is None
    totals = proof['legacyTotals']
    assert totals['scope'] == scope
    for month, wanted in [('october', (10000, 1, 9999)), ('september', (0, 1234, -1234))]:
        actual = totals[month]
        assert all(type(actual[key]) is int for key in ('incomeCents', 'expenseCents', 'balanceCents'))
        assert tuple(actual[key] for key in ('incomeCents', 'expenseCents', 'balanceCents')) == wanted
    assert proof['databaseExisted'] is True and proof['readOnlyReadback'] is True
assert seed[0]['rawReopened'] is True and verify[2]['rawReopened'] is True
opened = verify[0]
for key in ('accountsCount', 'transfersCount', 'adjustmentsCount', 'assetCents', 'debtCents', 'netCents'):
    assert type(opened['metadata'][key]) is int and opened['metadata'][key] == 0
assert opened['metadata']['accountBalances'] == {}
assert opened['canonicalDefaultsMatch'] is True and opened['distinctLegacyPairsMatch'] is True
categories = opened['categoryRows']
assert len(categories) == 13 and len({row['id'] for row in categories}) == 13
for row in categories:
    assert type(row['sortOrder']) is int and type(row['archived']) is bool
for original in expected:
    assert sum(row['kind'] == original['kind'] and row['name'] == original['category'] and row['archived'] is False for row in categories) == 1
checks = ['createAccount', 'initialBalanceZero', 'accountLinkedIncomeExpense', 'transferDoesNotChangeIncomeExpense', 'calibrationDeltaRecalculated', 'format1NewIdMerge', 'format1DuplicatePreservesLocalLink', 'format2ScratchRoundTrip', 'linkedEntrySavedThroughUi']
assert all(verify[1]['assertedChecks'][key] is True for key in checks)
for name, values in [('seed-proofs.json', seed), ('verify-proofs.json', verify)]:
    (directory / name).write_text(json.dumps(values, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
print('PASS: exact raw seed and opened/capabilities/reopened proofs')
PY
printf 'PASS: synthetic ledger survived 0.1.4/code6/schema1 -> 0.1.8/code10/schema2 replacement\n' | tee "$artifacts/summary.txt"
