#!/usr/bin/env bash
# Preflight for the FD Manager test round.
#
# WHY THIS EXISTS
#   Roo does not always get a MINGW64 prompt. When it is handed cmd.exe, every
#   command in the instruction file fails for a different reason and the report
#   fills up with five "blockers" that are really one. This script answers the
#   only question that matters — can this shell build and run the suite? — in
#   one command, and it works when launched from cmd.exe.
#
# HOW TO RUN IT (works from cmd.exe, PowerShell and MINGW64 alike)
#   "C:\Program Files\Git\bin\bash.exe" tools/preflight.sh
#
# Exit code 0 = ready. Anything else = not ready; send the output back.

set -u

ok=0
fail=0
notes=""

pass() { printf "  PASS  %-22s %s\n" "$1" "$2"; ok=$((ok + 1)); }
fail_it() { printf "  FAIL  %-22s %s\n" "$1" "$2"; fail=$((fail + 1)); notes="$notes\n  - $1: $2"; }

echo "=============================================="
echo " FD Manager - test environment preflight"
echo "=============================================="

# --- 1. which shell are we actually in? ----
# A bash is not necessarily THE bash. WSL bash is a Linux shell with no JDK and
# no Android SDK; it fails later in a way that looks like a broken machine.
if [ -z "${BASH_VERSION:-}" ]; then
    fail_it "shell" "not bash - the executor is in cmd.exe or PowerShell"
else
    case "$(uname -s 2>/dev/null)" in
        MINGW*|MSYS*|CYGWIN*)
            pass "shell" "bash $BASH_VERSION - Git Bash on Windows" ;;
        Linux*)
            fail_it "shell" "bash on Linux/WSL - NOT Git Bash; no JDK, no Android SDK here" ;;
        *)
            fail_it "shell" "bash $BASH_VERSION on $(uname -s 2>/dev/null) - unrecognised platform" ;;
    esac
fi

# --- 2. where did we land? ----
here="$(pwd)"
if [ -f "gradlew" ] && [ -f "settings.gradle.kts" -o -f "settings.gradle" ]; then
    pass "repo" "$here"
else
    fail_it "repo" "run this from the repo root (looked in $here)"
fi

# --- 3. gradle wrapper present and usable ----
if [ -f "gradlew" ]; then
    case "$(uname -s)" in
        MINGW*|MSYS*|CYGWIN*)
            # Windows has no execute bit; MSYS reports everything as executable.
            # Checking -x here would produce a false failure on the owner's machine.
            pass "gradlew" "present" ;;
        *)
            if [ -x "gradlew" ]; then
                pass "gradlew" "present + executable"
            else
                fail_it "gradlew" "present but NOT executable - run: chmod +x gradlew"
            fi ;;
    esac
else
    fail_it "gradlew" "missing - wrong directory?"
fi

# --- 4. a JDK is reachable ----
if command -v java >/dev/null 2>&1; then
    jline="$(java -version 2>&1 | head -1)"
    pass "java" "$jline  [$(command -v java)]"
elif [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/java" ]; then
    pass "java" "via JAVA_HOME: $JAVA_HOME/bin/java"
else
    # Look, do not guess. Naming the candidates stops the executor hunting with
    # find /c and stops the owner being sent to a folder that may not exist.
    candidates=""
    for c in "/c/Program Files/Android/Android Studio/jbr" \
             "/c/Program Files/Eclipse Adoptium"/* \
             "/c/Program Files/Java"/* \
             "${LOCALAPPDATA:-/nonexistent}/Programs/Android Studio/jbr"; do
        [ -x "$c/bin/java.exe" ] && candidates="$candidates $c"
    done
    if [ -n "$candidates" ]; then
        fail_it "java" "not on PATH and JAVA_HOME unset - but found:$candidates"
    else
        fail_it "java" "no java on PATH and JAVA_HOME unset (no JDK found in the usual places)"
    fi
fi

# --- 5. the Android SDK ----
sdk=""
if [ -f "local.properties" ]; then
    sdk="$(grep -i '^sdk.dir=' local.properties | head -1 | cut -d= -f2- | sed 's|\\\\|/|g; s|\\:|:|g')"
fi
if [ -n "$sdk" ] && [ -d "$sdk" ]; then
    pass "android sdk" "$sdk (from local.properties)"
elif [ -n "${ANDROID_HOME:-}" ] && [ -d "$ANDROID_HOME" ]; then
    pass "android sdk" "$ANDROID_HOME (from ANDROID_HOME)"
else
    fail_it "android sdk" "no sdk.dir in local.properties and ANDROID_HOME unset"
fi

# --- 6. adb + a device ----
if command -v adb >/dev/null 2>&1; then
    devs="$(adb devices 2>/dev/null | sed -n '2,$p' | grep -c "device$")"
    if [ "$devs" -ge 1 ]; then
        pass "adb device" "$devs connected"
    else
        fail_it "adb device" "adb present, no device - start Pixel_7_API_34"
    fi
else
    fail_it "adb" "not on PATH - use \$ANDROID_HOME/platform-tools/adb"
fi

# --- 7. the branch under test ----
if git rev-parse --git-dir >/dev/null 2>&1; then
    pass "git" "$(git rev-parse --abbrev-ref HEAD) @ $(git rev-parse --short HEAD)"
else
    fail_it "git" "not a git working tree"
fi

echo "----------------------------------------------"
if [ "$fail" -eq 0 ]; then
    echo " RESULT: READY ($ok checks passed)"
    echo " Next: run the steps in docs/test-instructions/TEST-INSTRUCTIONS-006.md"
    exit 0
else
    echo " RESULT: NOT READY - $fail of $((ok + fail)) checks failed"
    printf " Failed:$notes\n"
    echo " Send this whole block back. Do not try a different shell or tool."
    exit 1
fi
