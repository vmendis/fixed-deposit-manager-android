#!/usr/bin/env bash
# FD Manager — shell environment bootstrap.
#
# WHY THIS EXISTS
# Android Studio / Roo launches the shell as a bare `bash.exe`. A bare bash is NOT a login
# shell, so /etc/profile never runs and /usr/bin is never added to PATH. Result: `grep`, `ls`,
# `cat`, `sed` all vanish, and `adb`/`java` were never on PATH to begin with. Commands then fail
# with "not found" and it looks like the repo is missing files.
#
# USAGE — source it, in the SAME window you will run the tests in:
#
#     . ./scripts/env.sh
#
# NOTE THE LEADING DOT. `./scripts/env.sh` (without the dot) runs it in a child shell and every
# export is thrown away when it exits. `.` is a bash builtin, so it works even when PATH is broken.

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]:-$0}")/.." 2>/dev/null && pwd)"

# ---- JDK 17 ---------------------------------------------------------------------------------
export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-17.0.16.8-hotspot"
if [ ! -d "$JAVA_HOME" ]; then
    for d in "/c/Program Files/Eclipse Adoptium"/jdk-17*; do
        [ -d "$d" ] && export JAVA_HOME="$d" && break
    done
fi

# ---- Android SDK ----------------------------------------------------------------------------
# adb worked from a normal login shell before, so its directory is probably already on PATH.
# Do NOT assume where the SDK lives: take it from adb itself if adb is reachable, then fall back
# to the usual locations only if it is not.
adb_dir_of() {
    _a=$(command -v adb 2>/dev/null) || return 1
    cd "$(dirname "$_a")" 2>/dev/null && pwd -P || return 1
}

if _d=$(adb_dir_of); then
    :   # adb is already reachable — leave PATH alone, just derive ANDROID_HOME from it
else
    for d in \
        "/c/Users/thiss/AppData/Local/Android/Sdk/platform-tools" \
        "/c/Users/$USER/AppData/Local/Android/Sdk/platform-tools" \
        "/c/Android/Sdk/platform-tools" \
        "/c/Program Files/Android/Android Studio/platform-tools"
    do
        if [ -x "$d/adb.exe" ] || [ -x "$d/adb" ]; then
            PATH="$d:$PATH"
            _d="$d"
            break
        fi
    done
fi

if [ -n "$_d" ]; then
    candidate="$(dirname "$_d")"
    if [ -d "$candidate" ]; then
        export ANDROID_HOME="${ANDROID_HOME:-$candidate}"
    fi
fi
[ -z "$ANDROID_HOME" ] && export ANDROID_HOME="/c/Users/thiss/AppData/Local/Android/Sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
unset _d candidate

# ---- PATH -----------------------------------------------------------------------------------
# /usr/bin and /mingw64/bin are added unconditionally: if the shell did not source
# /etc/profile, they are missing and nothing else in this project will work.
# Prepend only. Everything that already worked (including whatever provides adb) stays in $PATH.
export PATH="/usr/bin:/bin:/mingw64/bin:$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$PATH"

unset MSYSTEM 2>/dev/null || true

# ---- Self check -----------------------------------------------------------------------------
echo "----------------------------------------------------------------"
printf '%-16s %s\n' "JAVA_HOME"    "${JAVA_HOME:-<unset>}"
printf '%-16s %s\n' "ANDROID_HOME:" "${ANDROID_HOME:-<unset>}"
printf '%-16s %s\n' "bash"         "${BASH_VERSION:-<not bash>}"
printf '%-16s %s\n' "pwd"          "$(pwd)"
echo "----------------------------------------------------------------"

fail=0
chk() { # chk <label> <command...>
    label="$1"; shift
    if out=$("$@" 2>&1) && [ -n "$out" ]; then
        printf '  [OK]   %-10s %s\n' "$label" "$(printf '%s' "$out" | head -1)"
    else
        printf '  [FAIL] %-10s %s\n' "$label" "${out:-not found}"
        fail=1
    fi
}
chk java   java -version
chk adb    adb version
printf '  %-6s %-10s %s\n' "info" "adb"  "$(command -v adb 2>/dev/null || echo 'not on PATH')"
printf '  %-6s %-10s %s\n' "info" "java" "$(command -v java 2>/dev/null || echo 'not on PATH')"
chk grep   grep --version
chk git    git --version

# /mnt/ means a WSL path leaked in — wrong shell entirely.
case "$JAVA_HOME" in
    /mnt/*) printf '  [FAIL] JAVA_HOME starts with /mnt/ — that is WSL, you are not in Git Bash\n'; fail=1 ;;
esac

if [ -d "$REPO_ROOT" ] && [ -f "$REPO_ROOT/gradlew" ]; then
    printf '  [OK]   %-10s %s\n' "repo" "$REPO_ROOT"
else
    printf '  [FAIL] %-10s could not locate the repo root from %s\n' "repo" "$0"
    fail=1
fi

echo "----------------------------------------------------------------"
if [ "$fail" -eq 0 ]; then
    echo "  ALL CHECKS PASSED — you may run the tests."
else
    echo "  CHECKS FAILED — STOP. Report the [FAIL] lines. Do not fix them (rule C3/C4)."
fi
echo "----------------------------------------------------------------"

unset -f chk 2>/dev/null || true
