# Phase 0 — Scaffold & Safe Legacy Removal Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the legacy Java/JS/Capacitor web project with an empty-but-running Kotlin/libGDX multi-module Gradle project that launches on desktop (dev) and Android, builds a debug APK in CI, and has a working JVM unit-test pipeline.

**Architecture:** Gradle multi-module — `core` (platform-independent Kotlin + libGDX API + Fleks), `desktop` (LWJGL3 launcher, dev-only), `android` (APK launcher). A trivial `GameScreen` clears the screen and draws a placeholder label, proving the render loop runs on every target. The original web app is preserved at git tag `legacy-web-v1.1.1` and removed from the working tree so Gradle owns a clean repo root.

**Tech Stack:** Kotlin 2.0.21, libGDX 1.13.1, Fleks 2.8, Gradle 8.10.2, Android Gradle Plugin 8.7.2, JDK 17, JUnit 5. (Versions are pinned and mutually compatible — keep these pins.)

---

## Pinned Versions (single source of truth)

| Tool | Version |
|---|---|
| JDK | 17 (Temurin) |
| Gradle | 8.10.2 |
| Android Gradle Plugin | 8.7.2 |
| Kotlin | 2.0.21 |
| libGDX | 1.13.1 |
| Fleks (ECS) | 2.8 |
| JUnit Jupiter | 5.10.2 |
| compileSdk / targetSdk | 35 |
| minSdk | 24 |

## File Structure (created in this phase)

```
demo0902/
├─ settings.gradle.kts          # module list + repositories
├─ gradle.properties            # JVM/AndroidX flags
├─ build.gradle.kts             # root: plugin versions (apply false)
├─ gradlew / gradlew.bat / gradle/wrapper/*   # Gradle wrapper (generated)
├─ .gitignore                   # Gradle/Android ignores (replaces web .gitignore)
├─ README.md                    # rewritten to the new stack
├─ .github/workflows/build.yml  # Gradle CI (replaces Maven/Capacitor workflows)
├─ core/
│  ├─ build.gradle.kts
│  └─ src/main/kotlin/io/github/panda17tk/arpg/
│  │  ├─ App.kt                 # com.badlogic.gdx.Game; sets GameScreen
│  │  ├─ core/Constants.kt      # FIXED_DT/MAX_DT/titles
│  │  ├─ core/Numbers.kt        # clamp() util (TDD'd; reused for save validation later)
│  │  └─ screens/GameScreen.kt  # clears screen + placeholder label
│  └─ src/test/kotlin/io/github/panda17tk/arpg/core/NumbersTest.kt
├─ desktop/
│  ├─ build.gradle.kts
│  └─ src/main/kotlin/io/github/panda17tk/arpg/lwjgl3/Lwjgl3Launcher.kt
└─ android/
   ├─ build.gradle.kts
   └─ src/main/
      ├─ AndroidManifest.xml
      └─ kotlin/io/github/panda17tk/arpg/AndroidLauncher.kt
```

---

## Task 1: Tag legacy, remove old tree

**Files:**
- Tag: `legacy-web-v1.1.1` → the legacy release commit on `main` (`b17e97f`)
- Remove: `pom.xml`, `src/main/java`, `src/main/webapp`, `src/test`, `mobile/`, `.classpath`, `.project`, `.settings`, `DESIGN.md`, `BUILD.md`, `CODE_REVIEW.md`, `CODE_REVIEW_2.md`, old `.github/workflows/*`
- Keep: `docs/` (spec + this plan), `.git`, `README.md` (rewritten in Task 6)

- [ ] **Step 1: Confirm we are on the redesign branch**

Run: `git -C V:/src/demo0902 branch --show-current`
Expected: `redesign/android-native-kotlin`

- [ ] **Step 2: Tag the legacy web app (preserves it for reference/diffing)**

```bash
git -C V:/src/demo0902 tag -a legacy-web-v1.1.1 b17e97f -m "Final web (Capacitor) build before native Kotlin rewrite"
git -C V:/src/demo0902 tag -l
```
Expected: output lists `legacy-web-v1.1.1`.

- [ ] **Step 3: Remove the legacy tree from the working branch**

```bash
cd V:/src/demo0902
git rm -r --quiet pom.xml src/main/java src/main/webapp src/test mobile .classpath .project .settings DESIGN.md BUILD.md CODE_REVIEW.md CODE_REVIEW_2.md
git rm -r --quiet .github/workflows
```
Expected: files staged for deletion (no error). If a path is already absent, drop it from the command and continue.

- [ ] **Step 4: Verify only docs + git remain**

Run: `git -C V:/src/demo0902 ls-files`
Expected: lists only `README.md` and files under `docs/`. No `.java`, `.js`, `.jsp`, `pom.xml`, or `mobile/` entries.

- [ ] **Step 5: Commit**

```bash
cd V:/src/demo0902
git commit -m "chore: remove legacy web/Java/Capacitor tree (preserved at tag legacy-web-v1.1.1)"
```

---

## Task 2: Gradle root, wrapper, and ignores

**Files:**
- Create: `settings.gradle.kts`, `gradle.properties`, `build.gradle.kts`, `.gitignore`
- Generate: `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`, `gradle/wrapper/gradle-wrapper.properties`

- [ ] **Step 1: Create `settings.gradle.kts`**

```kotlin
pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "arpg"
include("core", "desktop", "android")
```

- [ ] **Step 2: Create `gradle.properties`**

```properties
org.gradle.jvmargs=-Xmx2g -Dfile.encoding=UTF-8
org.gradle.daemon=true
org.gradle.parallel=true
android.useAndroidX=true
kotlin.code.style=official
```

- [ ] **Step 3: Create root `build.gradle.kts`**

```kotlin
plugins {
    id("com.android.application") version "8.7.2" apply false
    kotlin("android") version "2.0.21" apply false
    kotlin("jvm") version "2.0.21" apply false
}
```

- [ ] **Step 4: Create `.gitignore` (replaces the old web one)**

```gitignore
# Gradle / build
.gradle/
build/
*/build/
# Android
local.properties
*/src/main/jniLibs/
*.apk
*.aab
*.keystore
# IDE
.idea/
*.iml
*.iws
.vscode/
# OS
.DS_Store
Thumbs.db
```

- [ ] **Step 5: Generate the Gradle wrapper (pinned to 8.10.2)**

Requires a Gradle install or Android Studio's bundled Gradle on PATH. Run from the repo root:

Run: `cd V:/src/demo0902 && gradle wrapper --gradle-version 8.10.2 --distribution-type bin`
Expected: creates `gradlew`, `gradlew.bat`, and `gradle/wrapper/gradle-wrapper.{jar,properties}`.

If `gradle` is not on PATH, install Gradle 8.10.2 (e.g. `winget install Gradle.Gradle` or SDKMAN) first, then re-run. Do not hand-edit the wrapper jar.

- [ ] **Step 6: Verify the wrapper works**

Run: `cd V:/src/demo0902 && ./gradlew --version`
Expected: prints `Gradle 8.10.2`.

- [ ] **Step 7: Commit**

```bash
cd V:/src/demo0902
git add settings.gradle.kts gradle.properties build.gradle.kts .gitignore gradlew gradlew.bat gradle/
git commit -m "build: add Gradle multi-module root and wrapper (8.10.2)"
```

---

## Task 3: `core` module — clamp util (TDD), Constants, App, GameScreen

**Files:**
- Create: `core/build.gradle.kts`
- Create: `core/src/main/kotlin/io/github/panda17tk/arpg/core/Numbers.kt`
- Create: `core/src/main/kotlin/io/github/panda17tk/arpg/core/Constants.kt`
- Create: `core/src/main/kotlin/io/github/panda17tk/arpg/App.kt`
- Create: `core/src/main/kotlin/io/github/panda17tk/arpg/screens/GameScreen.kt`
- Test: `core/src/test/kotlin/io/github/panda17tk/arpg/core/NumbersTest.kt`

- [ ] **Step 1: Create `core/build.gradle.kts`**

```kotlin
plugins {
    kotlin("jvm")
}

dependencies {
    api("com.badlogicgames.gdx:gdx:1.13.1")
    api("io.github.quillraven.fleks:Fleks:2.8")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
}

kotlin {
    jvmToolchain(17)
}

tasks.test {
    useJUnitPlatform()
}
```

- [ ] **Step 2: Write the failing test `NumbersTest.kt`**

```kotlin
package io.github.panda17tk.arpg.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class NumbersTest {
    @Test
    fun `clamp returns value inside range unchanged`() {
        assertEquals(5.0, clamp(5.0, 0.0, 10.0, -1.0))
    }

    @Test
    fun `clamp pins below the lower bound`() {
        assertEquals(0.0, clamp(-3.0, 0.0, 10.0, -1.0))
    }

    @Test
    fun `clamp pins above the upper bound`() {
        assertEquals(10.0, clamp(42.0, 0.0, 10.0, -1.0))
    }

    @Test
    fun `clamp returns default for non-finite input`() {
        assertEquals(-1.0, clamp(Double.NaN, 0.0, 10.0, -1.0))
    }
}
```

- [ ] **Step 3: Run the test to verify it fails**

Run: `cd V:/src/demo0902 && ./gradlew :core:test --tests "io.github.panda17tk.arpg.core.NumbersTest"`
Expected: FAIL — compilation error, `clamp` is unresolved.

- [ ] **Step 4: Implement `Numbers.kt`**

```kotlin
package io.github.panda17tk.arpg.core

/**
 * Clamp [v] into [lo, hi]. Returns [def] when [v] is NaN/Infinite.
 * Mirrors the legacy save-validation helper clampNum() so corrupt/tampered
 * save & config JSON can be coerced into safe ranges (see spec §9).
 */
fun clamp(v: Double, lo: Double, hi: Double, def: Double): Double {
    if (!v.isFinite()) return def
    return maxOf(lo, minOf(hi, v))
}
```

- [ ] **Step 5: Run the test to verify it passes**

Run: `cd V:/src/demo0902 && ./gradlew :core:test --tests "io.github.panda17tk.arpg.core.NumbersTest"`
Expected: PASS (4 tests).

- [ ] **Step 6: Create `Constants.kt`**

```kotlin
package io.github.panda17tk.arpg.core

/** App-wide constants. Simulation timing mirrors the legacy fixed-step loop (spec §7). */
object Constants {
    const val APP_TITLE = "ARPG Survival"
    const val VERSION_NAME = "2.0.0"

    /** Fixed simulation timestep (seconds). */
    const val FIXED_DT = 1.0f / 60.0f
    /** Maximum frame delta before clamping (prevents tunneling on stalls). */
    const val MAX_DT = 0.05f
    /** Max simulation sub-steps per frame (anti death-spiral). */
    const val MAX_STEPS = 5
}
```

- [ ] **Step 7: Create `App.kt`**

```kotlin
package io.github.panda17tk.arpg

import com.badlogic.gdx.Game
import io.github.panda17tk.arpg.screens.GameScreen

/** Root libGDX application. Owns global services in later phases; for now boots GameScreen. */
class App : Game() {
    override fun create() {
        setScreen(GameScreen())
    }
}
```

- [ ] **Step 8: Create `screens/GameScreen.kt`**

```kotlin
package io.github.panda17tk.arpg.screens

import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.ScreenUtils
import io.github.panda17tk.arpg.core.Constants

/** Placeholder screen: proves the render loop runs on every platform target. */
class GameScreen : ScreenAdapter() {
    private lateinit var batch: SpriteBatch
    private lateinit var font: BitmapFont

    override fun show() {
        batch = SpriteBatch()
        font = BitmapFont() // default ASCII font; Japanese (FreeType) arrives in Phase 8
    }

    override fun render(delta: Float) {
        ScreenUtils.clear(0.06f, 0.07f, 0.10f, 1f)
        batch.begin()
        font.draw(batch, "${Constants.APP_TITLE} - Phase 0 OK", 24f, 48f)
        batch.end()
    }

    override fun dispose() {
        batch.dispose()
        font.dispose()
    }
}
```

- [ ] **Step 9: Verify the whole module compiles**

Run: `cd V:/src/demo0902 && ./gradlew :core:compileKotlin :core:test`
Expected: BUILD SUCCESSFUL, NumbersTest passes.

- [ ] **Step 10: Commit**

```bash
cd V:/src/demo0902
git add core/
git commit -m "feat(core): scaffold libGDX/Fleks core with clamp util, App, GameScreen"
```

---

## Task 4: `desktop` module — LWJGL3 launcher (dev-only)

**Files:**
- Create: `desktop/build.gradle.kts`
- Create: `desktop/src/main/kotlin/io/github/panda17tk/arpg/lwjgl3/Lwjgl3Launcher.kt`

- [ ] **Step 1: Create `desktop/build.gradle.kts`**

```kotlin
plugins {
    kotlin("jvm")
    application
}

dependencies {
    implementation(project(":core"))
    implementation("com.badlogicgames.gdx:gdx-backend-lwjgl3:1.13.1")
    implementation("com.badlogicgames.gdx:gdx-platform:1.13.1:natives-desktop")
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("io.github.panda17tk.arpg.lwjgl3.Lwjgl3LauncherKt")
}
```

- [ ] **Step 2: Create `Lwjgl3Launcher.kt`**

```kotlin
package io.github.panda17tk.arpg.lwjgl3

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import io.github.panda17tk.arpg.App
import io.github.panda17tk.arpg.core.Constants

/** Desktop launcher — DEVELOPMENT ONLY. The shipped product is the Android module. */
fun main() {
    val config = Lwjgl3ApplicationConfiguration().apply {
        setTitle(Constants.APP_TITLE)
        setWindowedMode(1280, 720)
        useVsync(true)
        setForegroundFPS(60)
    }
    Lwjgl3Application(App(), config)
}
```

- [ ] **Step 3: Run the desktop app and confirm a window opens**

Run: `cd V:/src/demo0902 && ./gradlew :desktop:run`
Expected: a 1280x720 window titled "ARPG Survival" opens on a dark background with the text "ARPG Survival - Phase 0 OK". Close the window to end the run (the Gradle task then completes).

- [ ] **Step 4: Commit**

```bash
cd V:/src/demo0902
git add desktop/
git commit -m "feat(desktop): add LWJGL3 dev launcher"
```

---

## Task 5: `android` module — launcher, manifest, debug APK

**Files:**
- Create: `android/build.gradle.kts`
- Create: `android/src/main/AndroidManifest.xml`
- Create: `android/src/main/kotlin/io/github/panda17tk/arpg/AndroidLauncher.kt`
- Create (local, gitignored): `local.properties` with the Android SDK path

- [ ] **Step 1: Point Gradle at the Android SDK via `local.properties`**

Create `V:/src/demo0902/local.properties` (gitignored) with your SDK path, e.g.:

```properties
sdk.dir=C\:\\Users\\banti\\AppData\\Local\\Android\\Sdk
```
Adjust the path to your machine. On CI this file is unnecessary (the SDK path comes from the `ANDROID_HOME` env set by the setup-android action).

- [ ] **Step 2: Create `android/build.gradle.kts`**

```kotlin
plugins {
    id("com.android.application")
    kotlin("android")
}

android {
    namespace = "io.github.panda17tk.arpg"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.github.panda17tk.arpg"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "2.0.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
}

val natives: Configuration by configurations.creating

dependencies {
    implementation(project(":core"))
    implementation("com.badlogicgames.gdx:gdx-backend-android:1.13.1")
    natives("com.badlogicgames.gdx:gdx-platform:1.13.1:natives-armeabi-v7a")
    natives("com.badlogicgames.gdx:gdx-platform:1.13.1:natives-arm64-v8a")
    natives("com.badlogicgames.gdx:gdx-platform:1.13.1:natives-x86")
    natives("com.badlogicgames.gdx:gdx-platform:1.13.1:natives-x86_64")
}

// Extract libGDX .so natives from the platform jars into src/main/jniLibs/<abi>
// so AGP's jniLibs merge picks them up. Runs before preBuild.
tasks.register("copyAndroidNatives") {
    val nativesConfig = natives
    val outRoot = file("src/main/jniLibs")
    doLast {
        nativesConfig.files.forEach { jar ->
            val abi = jar.name.substringAfter("natives-").substringBeforeLast(".jar")
            val outDir = outRoot.resolve(abi)
            outDir.mkdirs()
            copy {
                from(zipTree(jar))
                into(outDir)
                include("*.so")
            }
        }
    }
}

tasks.named("preBuild") {
    dependsOn("copyAndroidNatives")
}
```

- [ ] **Step 3: Create `android/src/main/AndroidManifest.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.VIBRATE" />

    <application
        android:allowBackup="false"
        android:label="ARPG Survival"
        android:theme="@android:style/Theme.NoTitleBar.Fullscreen">
        <activity
            android:name=".AndroidLauncher"
            android:exported="true"
            android:screenOrientation="sensorLandscape"
            android:configChanges="keyboard|keyboardHidden|navigation|orientation|screenSize|screenLayout">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

- [ ] **Step 4: Create `AndroidLauncher.kt`**

```kotlin
package io.github.panda17tk.arpg

import android.os.Bundle
import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration

class AndroidLauncher : AndroidApplication() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val config = AndroidApplicationConfiguration().apply {
            useImmersiveMode = true
        }
        initialize(App(), config)
    }
}
```

- [ ] **Step 5: Assemble the debug APK**

Run: `cd V:/src/demo0902 && ./gradlew :android:assembleDebug`
Expected: BUILD SUCCESSFUL; APK at `android/build/outputs/apk/debug/android-debug.apk`. If the build fails on native libraries, run `./gradlew :android:copyAndroidNatives` once and re-run; confirm `android/src/main/jniLibs/arm64-v8a/libgdx.so` exists.

- [ ] **Step 6: (Optional) Install on a connected device/emulator and confirm it launches**

Run: `cd V:/src/demo0902 && ./gradlew :android:installDebug`
Expected: app "ARPG Survival" installs, opens in landscape, shows the dark screen with "ARPG Survival - Phase 0 OK".

- [ ] **Step 7: Commit**

```bash
cd V:/src/demo0902
git add android/
git commit -m "feat(android): add APK launcher, manifest (landscape/VIBRATE), native copy task"
```

---

## Task 6: CI workflow + README rewrite

**Files:**
- Create: `.github/workflows/build.yml`
- Modify (replace contents): `README.md`

- [ ] **Step 1: Create `.github/workflows/build.yml`**

```yaml
name: build

on:
  push:
    branches: [ main, "redesign/**" ]
    tags: [ "v*" ]
  pull_request:

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: "17"

      - name: Set up Android SDK
        uses: android-actions/setup-android@v3

      - name: Set up Gradle
        uses: gradle/actions/setup-gradle@v4

      - name: Run core tests
        run: ./gradlew :core:test --no-daemon

      - name: Assemble debug APK
        run: ./gradlew :android:assembleDebug --no-daemon

      - name: Upload APK
        uses: actions/upload-artifact@v4
        with:
          name: arpg-debug-apk
          path: android/build/outputs/apk/debug/*.apk
```

- [ ] **Step 2: Rewrite `README.md` to the new stack**

```markdown
# ARPG サバイバル (Android / Kotlin + libGDX)

見下ろし型・ウェーブ制ローグライトのサバイバルシューターを、**Android専用ネイティブゲーム**として
Kotlin + libGDX で再実装するプロジェクト。

> 旧 Web 版（Java/Servlet + Canvas/JS + Capacitor）は git タグ `legacy-web-v1.1.1` に保存されています。
> 設計の経緯は [docs/superpowers/specs/2026-06-28-android-native-redesign-design.md](docs/superpowers/specs/2026-06-28-android-native-redesign-design.md) を参照。

## モジュール構成

- `core/` — ゲームロジック（プラットフォーム非依存・Kotlin + libGDX + Fleks）
- `desktop/` — LWJGL3 ランチャー（**開発専用**：PCで高速反復）
- `android/` — 配布対象の Android アプリ（APK）

## 開発

```bash
# 前提: JDK 17, Android SDK (local.properties に sdk.dir)

# デスクトップで起動（開発用）
./gradlew :desktop:run

# ユニットテスト
./gradlew :core:test

# デバッグ APK をビルド
./gradlew :android:assembleDebug   # → android/build/outputs/apk/debug/
```

## 技術スタック

Kotlin 2.0 / libGDX 1.13 / Fleks (ECS) / Gradle 8.10 / AGP 8.7 / minSdk 24・targetSdk 35
```

- [ ] **Step 3: Verify the full CI command sequence locally**

Run: `cd V:/src/demo0902 && ./gradlew :core:test :android:assembleDebug`
Expected: BUILD SUCCESSFUL; tests pass and the debug APK is produced.

- [ ] **Step 4: Commit**

```bash
cd V:/src/demo0902
git add .github/workflows/build.yml README.md
git commit -m "ci: replace Maven/Capacitor workflows with Gradle build + APK artifact"
```

---

## Self-Review

**1. Spec coverage (Phase 0 row + relevant §):**
- §0/§5 module structure (core/android/desktop) → Tasks 3, 4, 5. ✓
- §13 Ph0 "tag legacy → remove old tree → gdx project → CI skeleton → blank GameScreen runs on PC/device, CI builds APK" → Tasks 1–6. ✓
- §11 build config (applicationId, minSdk 24/targetSdk 35, sensorLandscape, VIBRATE, versionCode/Name) → Task 5 Steps 2–3. ✓
- §11 CI "Maven→Gradle, PR assembleDebug, upload APK" → Task 6. ✓
- §12 test pipeline (JVM unit, JUnit5) → Task 3 (clamp TDD) + Task 6 CI `:core:test`. ✓
- §6 ECS = Fleks dependency resolves → Task 3 `core/build.gradle.kts`. ✓
- Deferred to later phases by design: Fleks world/systems (Ph1), Japanese FreeType fonts (Ph8), VisUI dev editor (Ph8), release signing/AAB (Ph9), app icon (later). Noted in code comments where relevant.

**2. Placeholder scan:** No TBD/TODO/"handle later". The one non-code action (generate Gradle wrapper, Task 2 Step 5) is an exact command, not a placeholder. ✓

**3. Type/name consistency:** Package `io.github.panda17tk.arpg` everywhere; `App`, `GameScreen`, `Constants.APP_TITLE`, `clamp(v, lo, hi, def)` referenced consistently across core/desktop/android. Desktop `mainClass` matches the file's package + `Lwjgl3LauncherKt`. ✓

**Known risk (flagged):** AGP 8 native-lib wiring (Task 5 Step 2 `copyAndroidNatives` + `preBuild` hook) is the most version-sensitive step; Step 5 includes a concrete fallback (run the task manually, verify `libgdx.so`). If the `preBuild` hook misorders on a future AGP, depend on the variant `merge*JniLibFolders` task instead.

---

## Execution Handoff

This plan is for **Phase 0** only. After it lands (APK builds in CI, app runs on PC + device), the next step is to write the **Phase 1 plan** (core simulation foundation: Constants/Time/Rng/Vec2, Fleks world, fixed-step loop + interpolation, camera/viewport, movement/dash, desktop input) via the writing-plans skill.
