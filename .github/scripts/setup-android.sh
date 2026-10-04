#!/usr/bin/env bash
set -euo pipefail

# Use a pinned official SDK tool archive, independent of runner preinstalls.
export ANDROID_HOME="${RUNNER_TEMP:?}/android-sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
android_archive="$RUNNER_TEMP/android-commandline-tools.zip"
android_unpack="$RUNNER_TEMP/android-commandline-unpack"
mkdir -p "$ANDROID_HOME/cmdline-tools" "$android_unpack"
curl --fail --location --retry 3 \
  https://dl.google.com/android/repository/commandlinetools-linux-16111833_latest.zip \
  --output "$android_archive"
# Published SHA-1 from Google's repository2-3.xml, cmdline-tools;23.0.
echo "e025545c62a8e64c7559119566a569fb1dec5f60  $android_archive" | sha1sum --check
sha256sum "$android_archive"
unzip -q "$android_archive" -d "$android_unpack"
mv "$android_unpack/cmdline-tools" "$ANDROID_HOME/cmdline-tools/23.0"
android_bin="$ANDROID_HOME/cmdline-tools/23.0/bin"
export PATH="$android_bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$PATH"
printf 'ANDROID_HOME=%s\nANDROID_SDK_ROOT=%s\n' "$ANDROID_HOME" "$ANDROID_HOME" >> "$GITHUB_ENV"
printf '%s\n' "$android_bin" "$ANDROID_HOME/platform-tools" "$ANDROID_HOME/emulator" >> "$GITHUB_PATH"

# yes may receive SIGPIPE after sdkmanager exits successfully; its status
# must not turn a successful license acceptance into a setup failure.
set +o pipefail
yes | sdkmanager --sdk_root="$ANDROID_HOME" --licenses > "$RUNNER_TEMP/android-sdk-licenses.log"
set -o pipefail
