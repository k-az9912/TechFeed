#!/bin/sh
set -eu
cd "$(dirname "$0")/../app"
# Connect a device or start an emulator before running.
sh ./gradlew connectedDebugAndroidTest
