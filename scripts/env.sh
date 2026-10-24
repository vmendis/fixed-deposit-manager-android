#!/usr/bin/env bash
# FD Manager — shell environment bootstrap.
#
# WHY THIS EXISTS
# Roo launches a bare `bash.exe`. That is NOT a login shell, so /etc/profile never runs and the
# Git Bash PATH is never built. The shell inherits only the Windows PATH, and then:
#   - grep / ls / cat / sed  -> not found (they live in /usr/bin and /mingw64/bin)
#   - adb / java             -> not found (their dirs are added by the login PATH)
# In the owner's normal Git Bash window all of these DO work, because that shell logged in.
#
# USAGE — source it, in the SAME window you will run the tests in:
#
#     . ./scripts/env.sh
#
# NOTE THE LEADING DOT. `./scripts/env.sh` runs it in a child shell and every export is thrown
# away when it exits. `.` is a bash builtin, so it works even when PATH is already broken.

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]:-$0}")/.." 2>/dev/null && pwd)"

# ---- 1. Rebuild the login PATH ---------------------------------------------------------------
# If /usr/bin is not on PATH we are in the bare-shell case. Source /etc/profile, which is what a
# real Git Bash window does on startup. This restores grep/ls/cat and everything else.
case ":$PATH:" in
    *:/usr/bin:*) : ;;                       # already a login shell, leave it alone
    *)
        if [ -f /etc/profile ]; then
            . /etc/profile 2>/dev/null || true
        fi
        ;;
esac

# ---- 2. Git Bash coreutils, unconditionally -----------------------------------------------
# Harmless if already present; essential if /etc/profile did not cover it.
export PATH="/mingw64/bin:/usr/bin:/bin:$PATH"

# ---- 3. JDK 17 ------------------------------------------------------------------------------
if [ -z "$JAVA_HOME" ] || [ ! -d "$JAVA_HOME" ]; then
    for d in \
        "/c/Program Files/Eclipse Adoptium/jdk-17.0.16.8-hotspot" \
        "/c/Program Files/Eclipse Adoptium"/jdk-17*
    do
        [ -d "$d" ] && export JAVA_HOME="$d" && break
    done
fi
[ -n "$JAVA_HOME" ] && case ":$PATH:" in
    *":$JAVA_HOME/bin:"*) : ;;
    *) export PATH="$JAVA_HOME/bin:$PATH" ;;
esac

# ---- 4. Android SDK — take it from adb, do not assume it ------------------------------------
# adb works in the owner's login shell, so its directory is the ground truth for the SDK root.
if _adb=$(command -v adb 2>/dev/null) && [ -n "$_adb" ]; then
    _pt="$(cd "$(dirname "$_adb")" && pwd -P)"
else
    _pt=""
    for d in \
        "/c/Users/thiss/AppData/Local/Android/Sdk/platform-tools" \
        "/c/Users/$USER/AppData/Local/Android/Sdk/platform-tools" \
        "/c/Android/Sdk/platform-tools"
    do
        if [ -x "$d/adb.exe" ] || [ -x "$d/adb" ]; then _pt="$d"; break; fi
    done
    [ -n "$_pt" ] && export PATH="$_pt:$PATH"
fi
if [ -n "$_pt" ]; then
    export ANDROID_HOME="${ANDROID_HOME:-$(dirname "$_pt")}"
else
    export ANDROID_HOME="${ANDROID_HOME:-/c/Users/thiss/AppData/Local/Android/Sdk}"
fi
export ANDROID_SDK_ROOT="$ANDROID_HOME"
for d in "$ANDROID_HOME/emulator"; do
    [ -d "$d" ] && case ":$PATH:" in *":$d:"*) : ;; *) export PATH="$d:$PATH" ;; esac
done
unset _adb _pt d

unset MSYSTEM 2>/dev/null || true

# ---- 5. Self check ---------------------------------------------------------------------------
echo "----------------------------------------------------------------"
printf '%-16s %s\n' "JAVA_HOME"      "${JAVA_HOME:-<unset>}"
printf '%-16s %s\n' "ANDROID_HOME"   "${ANDROID_HOME:-<unset>}"
printf '%-16s %s\n' "bash"           "${BASH_VERSION:-<not bash>}"
printf '%-16s %s\n' "pwd"            "$(pwd)"
echo "----------------------------------------------------------------"

fail=0
chk() { # chk <label> <command...>
    label="$1"; shift
    if out=$("$@" 2>&1) && [ -n "$out" ]; then
        printf '  [OK]   %-8s %s\n' "$label" "$(printf '%s' "$out" | head -1)"
        printf '  info   %-8s %s\n' "$label" "$(command -v "$1" 2>/dev/null || echo '?')"
    else
        printf '  [FAIL] %-8s %s\n' "$label" "${out:-not found}"
        fail=1
    fi
}
chk java java -version
chk adb  adb version
chk grep grep --version
chk git  git --version

case "$JAVA_HOME" in
    /mnt/*) printf '  [FAIL] JAVA_HOME starts with /mnt/ — that is WSL, you are not in Git Bash\n'; fail=1 ;;
esac

if [ -n "$REPO_ROOT" ] && [ -f "$REPO_ROOT/gradlew" ]; then
    printf '  [OK]   %-8s %s\n' "repo" "$REPO_ROOT"
else
    printf '  [FAIL] %-8s could not locate the repo root from %s\n' "repo" "$0"
    fail=1
fi

echo "----------------------------------------------------------------"
if [ "$fail" -eq 0 ]; then
    echo "  ALL CHECKS PASSED — you may run the tests."
else
    echo "  CHECKS FAILED — STOP. Report the [FAIL] lines. Do not fix them (rules C3/C4)."
fi
echo "----------------------------------------------------------------"

unset -f chk 2>/dev/null || true
