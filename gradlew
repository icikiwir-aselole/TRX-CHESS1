#!/usr/bin/env sh
set -eu
if command -v gradle >/dev/null 2>&1; then exec gradle "$@"; fi
printf '%s\n' 'Gradle 9.5 is required. Use Android Studio or install Gradle 9.5+.' >&2
exit 1
