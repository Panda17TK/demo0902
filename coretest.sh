#!/usr/bin/env bash
# Isolated :core test build. The full build applies the Android Gradle Plugin, which resolves from
# dl.google.com — blocked by this session's egress policy. core is a pure kotlin-jvm module, so we
# swap in core-only settings/build Gradle files (Maven Central only), run, then always restore.
set -uo pipefail
cd "$(dirname "$0")"

restore() {
  [ -f settings.gradle.kts.bak ] && mv -f settings.gradle.kts.bak settings.gradle.kts
  [ -f build.gradle.kts.bak ] && mv -f build.gradle.kts.bak build.gradle.kts
}
trap restore EXIT

cp settings.gradle.kts settings.gradle.kts.bak
cp build.gradle.kts build.gradle.kts.bak

cat > settings.gradle.kts <<'EOF'
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}
dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}
rootProject.name = "arpg"
include("core")
EOF

cat > build.gradle.kts <<'EOF'
plugins {
    kotlin("jvm") version "2.0.21" apply false
    kotlin("plugin.serialization") version "2.0.21" apply false
}
EOF

gradle --no-daemon --console=plain :core:test "$@"
status=$?
exit $status
