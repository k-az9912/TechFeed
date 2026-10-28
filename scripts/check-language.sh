#!/bin/sh
set -eu
cd "$(dirname "$0")/../app"
[ -f gradlew ] || { echo 'Android Studio で app を作成してください' >&2; exit 1; }
sh ./gradlew lint testDebugUnitTest assembleDebug
