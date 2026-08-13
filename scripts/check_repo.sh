#!/usr/bin/env bash
set -euo pipefail
./gradlew test
./gradlew :app:assembleDebug
./gradlew :app:assembleRelease
