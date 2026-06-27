# Phase 0 — Gradle / libGDX Native Scaffold Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the legacy Maven/Java/Capacitor web project with a clean Kotlin + libGDX Gradle multi-module project (`core` / `desktop` / `android`) whose empty `GameScreen` launches on desktop and builds a debug APK, with a CI skeleton — satisfying Phase 0 of [the approved spec](../specs/2026-06-28-android-native-redesign-design.md).

**Architecture:** Three Gradle modules with Kotlin DSL. `core` holds all platform-independent game code (depends on libGDX, KTX, Fleks, VisUI, gdx-freetype). `desktop` is a dev-only lwjgl3 launcher. `android` is the distributable APK launcher with a native-`.so` extraction task. Shared assets live in a root `assets/` dir. The old web/Java/Capacitor tree is removed after tagging it `legacy-web-v1.1.1` (recoverable from the tag).

**Tech Stack:** Gradle 8.11.1 · AGP 8.9.3 · Kotlin 2.1.10 (JVM target 17) · libGDX 1.13.1 · KTX 1.13.1-rc1 · Fleks 2.11 · VisUI 1.5.5 · JUnit Jupiter 5.11.4 · compileSdk/targetSdk 35 / minSdk 24 · JDK 21 (installed).

**Working directory:** `V:\src\demo0902` (already on branch `redesign/android-native-kotlin`).

**Version-matrix rationale (why these exact pins):**
- KTX `1.13.1-rc1` is the binding constraint — it is built against and pins libGDX `1.13.1`, Kotlin `2.1.10`, Coroutines `1.10.1`, VisUI `1.5.5`.
- Fleks `2.13` (latest) is compiled with Kotlin `2.3.20`; a Kotlin `2.1.10` project compiler would fail to read its metadata ("compiled with a newer Kotlin"). **Fleks `2.11`** carries Kotlin `2.1.x` metadata → consumable. Bump Fleks only if/when the project Kotlin version is raised.
- AGP `8.9.3` ↔ Gradle `8.11.1` is the documented pairing (AGP 8.9 minimum Gradle = 8.11.1); both run on JDK 21; AGP 8.9 supports `compileSdk 35`. Staying on Gradle 8.x (not the demo's bleeding-edge 9.5.1) keeps the Kotlin 2.1.10 Gradle plugin inside its tested range.

---

## File Structure (what each new file is responsible for)

```
demo0902/
├─ settings.gradle.kts          # module graph + central repositories
├─ build.gradle.kts             # root: plugin versions (apply false)
├─ gradle.properties            # shared version pins + Gradle/AndroidX flags
├─ local.properties             # (gitignored) Android SDK path
├─ gradlew / gradlew.bat        # wrapper scripts (generated)
├─ gradle/wrapper/*             # wrapper jar + properties (generated)
├─ .gitignore                   # Gradle/Android/IDE ignores (revised)
├─ assets/.gitkeep              # shared runtime assets root
├─ core/
│  ├─ build.gradle.kts          # libGDX+KTX+Fleks+VisUI+freetype deps, JUnit
│  └─ src/
│     ├─ main/kotlin/io/github/panda17tk/arpg/
│     │  ├─ App.kt              # KtxGame entry, sets GameScreen
│     │  ├─ GameScreen.kt       # empty screen: clear + ASCII label
│     │  └─ core/Constants.kt   # FIXED_DT / MAX_STEPS / TITLE
│     └─ test/kotlin/io/github/panda17tk/arpg/core/
│        └─ ConstantsTest.kt    # pure-JVM JUnit5 smoke test
├─ desktop/
│  ├─ build.gradle.kts          # lwjgl3 backend + desktop natives, application
│  └─ src/main/kotlin/io/github/panda17tk/arpg/desktop/
│     └─ Lwjgl3Launcher.kt      # dev-only desktop launcher
└─ android/
   ├─ build.gradle.kts          # AGP config, android backend, natives copy task
   └─ src/main/
      ├─ AndroidManifest.xml     # sensorLandscape, VIBRATE, launcher activity
      ├─ kotlin/io/github/panda17tk/arpg/android/AndroidLauncher.kt
      └─ res/drawable/ic_launcher.xml  # single vector launcher icon (minSdk24-safe)

# Removed (preserved in tag legacy-web-v1.1.1):
#   pom.xml, src/, mobile/, .classpath, .project, .settings/, CODE_REVIEW*.md
# Replaced:
#   .github/workflows/{build.yml,android.yml,release.yml} -> {ci.yml,release.yml}
#   README.md, BUILD.md, DESIGN.md
```

---

## Task 1: Safety net — tag the legacy snapshot

**Files:** none (git tag only)

- [ ] **Step 1: Confirm clean tree on the redesign branch**

Run:
```bash
git -C V:/src/demo0902 status -s
git -C V:/src/demo0902 branch --show-current
```
Expected: empty status output; branch prints `redesign/android-native-kotlin`. If the tree is dirty, stop and resolve before continuing.

- [ ] **Step 2: Create the legacy tag at current HEAD**

The current HEAD still contains the entire legacy tree, so tagging it now preserves the old implementation.

Run:
```bash
git -C V:/src/demo0902 tag -a legacy-web-v1.1.1 -m "Legacy web/Java/Capacitor implementation snapshot before Kotlin/libGDX native rewrite"
git -C V:/src/demo0902 tag --list | grep legacy
```
Expected: `legacy-web-v1.1.1` appears in the list.

- [ ] **Step 3: (Optional) push the tag to origin**

Only run if the user wants the tag preserved on the remote now:
```bash
git -C V:/src/demo0902 push origin legacy-web-v1.1.1
```
Expected: `* [new tag] legacy-web-v1.1.1 -> legacy-web-v1.1.1`. (Skip if deferring all pushes.)

---

## Task 2: Remove the legacy tree (destructive)

**Files:**
- Delete: `pom.xml`, `src/`, `mobile/`, `.classpath`, `.project`, `.settings/`, `CODE_REVIEW.md`, `CODE_REVIEW_2.md`

- [ ] **Step 1: Remove legacy files via git**

Run (from repo root):
```bash
cd V:/src/demo0902
git rm -r --quiet pom.xml src mobile .classpath .project .settings CODE_REVIEW.md CODE_REVIEW_2.md
```
Expected: git reports the removals with no errors. (`.settings` is a directory; `-r` handles it.)

- [ ] **Step 2: Verify only intended files remain**

Run:
```bash
git -C V:/src/demo0902 status -s
git -C V:/src/demo0902 ls-files | head -40
```
Expected: staged deletions for all legacy paths. Remaining tracked files should be limited to `.github/`, `.gitignore`, `BUILD.md`, `DESIGN.md`, `README.md`, and `docs/` (the spec). No `.java`, `.js`, `.jsp`, or `pom.xml` should remain in `ls-files`.

- [ ] **Step 3: Commit the removal**

```bash
git -C V:/src/demo0902 commit -m "chore(phase0): remove legacy web/Java/Capacitor/Eclipse tree

Preserved in tag legacy-web-v1.1.1. Repository is being rebuilt as a
Kotlin/libGDX Android-native project per docs/superpowers/specs/2026-06-28-android-native-redesign-design.md."
```
Expected: commit succeeds.

---

## Task 3: Gradle wrapper (8.11.1)

**Files:**
- Create: `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`, `gradle/wrapper/gradle-wrapper.properties`

No standalone Gradle is installed, so download the exact distribution once and use it to generate a correct wrapper. Subsequent steps use `./gradlew`.

- [ ] **Step 1: Download and unpack Gradle 8.11.1 to a tools dir**

Run (PowerShell):
```powershell
$gz = "C:\Users\banti\AppData\Local\Temp\claude\C--\f2420d3a-04c2-41b4-98fb-4401fadb7da9\scratchpad\gradle-8.11.1-bin.zip"
Invoke-WebRequest -Uri "https://services.gradle.org/distributions/gradle-8.11.1-bin.zip" -OutFile $gz
$dest = "C:\Gradle"
New-Item -ItemType Directory -Force -Path $dest | Out-Null
Expand-Archive -Path $gz -DestinationPath $dest -Force
& "$dest\gradle-8.11.1\bin\gradle.bat" --version
```
Expected: prints `Gradle 8.11.1` and `JVM: 21.0.11`.

- [ ] **Step 2: Generate the wrapper in the project**

Run (PowerShell):
```powershell
cd V:\src\demo0902
& "C:\Gradle\gradle-8.11.1\bin\gradle.bat" wrapper --gradle-version 8.11.1 --distribution-type bin
```
Expected: creates `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`, `gradle/wrapper/gradle-wrapper.properties`. Build result `BUILD SUCCESSFUL`.

- [ ] **Step 3: Verify the wrapper works**

Run (PowerShell):
```powershell
cd V:\src\demo0902
.\gradlew.bat --version
```
Expected: `Gradle 8.11.1`, `JVM: 21.0.11`. (First run downloads the distribution to the Gradle user home; allow time.)

- [ ] **Step 4: Confirm `distributionUrl`**

Read `gradle/wrapper/gradle-wrapper.properties` and confirm it contains:
```properties
distributionUrl=https\://services.gradle.org/distributions/gradle-8.11.1-bin.zip
```

- [ ] **Step 5: Commit the wrapper**

```bash
git -C V:/src/demo0902 add gradlew gradlew.bat gradle/wrapper/gradle-wrapper.jar gradle/wrapper/gradle-wrapper.properties
git -C V:/src/demo0902 commit -m "build(phase0): add Gradle 8.11.1 wrapper"
```
Expected: commit succeeds (the `gradle-wrapper.jar` is intentionally tracked).

---

## Task 4: Root Gradle configuration

**Files:**
- Create: `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `local.properties`
- Modify: `.gitignore`

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

rootProject.name = "demo0902"

include("core", "desktop", "android")
```

- [ ] **Step 2: Create root `build.gradle.kts`**

```kotlin
plugins {
    id("com.android.application") version "8.9.3" apply false
    kotlin("jvm") version "2.1.10" apply false
    kotlin("android") version "2.1.10" apply false
}
```

- [ ] **Step 3: Create `gradle.properties`**

```properties
# --- Gradle ---
org.gradle.jvmargs=-Xmx2g -Dfile.encoding=UTF-8
org.gradle.daemon=true
org.gradle.parallel=true
org.gradle.caching=true

# --- Android ---
android.useAndroidX=true

# --- Kotlin ---
kotlin.code.style=official

# --- Shared dependency versions (read in module build files) ---
gdxVersion=1.13.1
ktxVersion=1.13.1-rc1
fleksVersion=2.11
visuiVersion=1.5.5
junitVersion=5.11.4
```

- [ ] **Step 4: Create `local.properties` (gitignored — points AGP at the SDK)**

```properties
sdk.dir=C\:\\Users\\banti\\AppData\\Local\\Android\\Sdk
```

- [ ] **Step 5: Replace `.gitignore` with Gradle/Android content**

```gitignore
# Gradle
.gradle/
build/
**/build/
!gradle/wrapper/gradle-wrapper.jar

# Android
local.properties
*.apk
*.aab
*.keystore
*.jks
android/src/main/jniLibs/

# IDE / OS
.idea/
*.iml
.DS_Store
*.swp

# Kotlin
.kotlin/
```

- [ ] **Step 6: Verify settings resolve (the three modules are not created yet, so expect a controlled failure)**

Run (PowerShell):
```powershell
cd V:\src\demo0902
.\gradlew.bat help
```
Expected: `BUILD SUCCESSFUL` for `help` is fine even before module build files exist *if* the module directories exist. If it complains that project directory for `:core` does not exist, that is expected and resolved in Tasks 5–7. Proceed regardless; full verification is `gradlew projects` after Task 7.

- [ ] **Step 7: Commit**

```bash
git -C V:/src/demo0902 add settings.gradle.kts build.gradle.kts gradle.properties .gitignore
git -C V:/src/demo0902 commit -m "build(phase0): root Gradle config (settings, plugins, version pins)"
```

---

## Task 5: `core` module (game logic, headless-testable)

**Files:**
- Create: `core/build.gradle.kts`
- Create: `core/src/main/kotlin/io/github/panda17tk/arpg/App.kt`
- Create: `core/src/main/kotlin/io/github/panda17tk/arpg/GameScreen.kt`
- Create: `core/src/main/kotlin/io/github/panda17tk/arpg/core/Constants.kt`
- Test: `core/src/test/kotlin/io/github/panda17tk/arpg/core/ConstantsTest.kt`

- [ ] **Step 1: Write the failing test first**

Create `core/src/test/kotlin/io/github/panda17tk/arpg/core/ConstantsTest.kt`:
```kotlin
package io.github.panda17tk.arpg.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ConstantsTest {
    @Test
    fun `fixed timestep is 60hz`() {
        assertEquals(1f / 60f, Constants.FIXED_DT)
    }

    @Test
    fun `max steps is positive`() {
        assertTrue(Constants.MAX_STEPS > 0)
    }

    @Test
    fun `title is set`() {
        assertEquals("ARPG Survival", Constants.TITLE)
    }
}
```

- [ ] **Step 2: Create `core/build.gradle.kts`**

```kotlin
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm")
}

val gdxVersion: String by project
val ktxVersion: String by project
val fleksVersion: String by project
val visuiVersion: String by project
val junitVersion: String by project

dependencies {
    api("com.badlogicgames.gdx:gdx:$gdxVersion")
    api("com.badlogicgames.gdx:gdx-freetype:$gdxVersion")
    api("io.github.libktx:ktx-app:$ktxVersion")
    api("io.github.libktx:ktx-graphics:$ktxVersion")
    api("io.github.quillraven.fleks:Fleks:$fleksVersion")
    api("com.kotcrab.vis:vis-ui:$visuiVersion")

    testImplementation("org.junit.jupiter:junit-jupiter:$junitVersion")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.11.4")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

tasks.test {
    useJUnitPlatform()
}
```

- [ ] **Step 3: Create `core/src/main/kotlin/io/github/panda17tk/arpg/core/Constants.kt`**

```kotlin
package io.github.panda17tk.arpg.core

/** Global, platform-independent constants. */
object Constants {
    const val TITLE = "ARPG Survival"

    /** Fixed simulation timestep (60 Hz) — see spec §7.1. */
    const val FIXED_DT = 1f / 60f

    /** Max simulation sub-steps per frame to prevent the spiral of death. */
    const val MAX_STEPS = 5
}
```

- [ ] **Step 4: Create `core/src/main/kotlin/io/github/panda17tk/arpg/GameScreen.kt`**

```kotlin
package io.github.panda17tk.arpg

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.ScreenUtils
import io.github.panda17tk.arpg.core.Constants
import ktx.app.KtxScreen

/**
 * Phase 0 placeholder screen: clears to a dark background and draws an ASCII
 * label, proving the SpriteBatch + font + render loop work on every platform.
 * Replaced by the real Boot/MainMenu/Game screens in later phases.
 */
class GameScreen : KtxScreen {
    private val batch = SpriteBatch()
    private val font = BitmapFont()

    override fun render(delta: Float) {
        ScreenUtils.clear(0.06f, 0.07f, 0.10f, 1f)
        batch.begin()
        font.color = Color.WHITE
        font.draw(
            batch,
            "${Constants.TITLE} — Phase 0 scaffold OK",
            24f,
            Gdx.graphics.height - 24f,
        )
        batch.end()
    }

    override fun dispose() {
        batch.dispose()
        font.dispose()
    }
}
```

- [ ] **Step 5: Create `core/src/main/kotlin/io/github/panda17tk/arpg/App.kt`**

```kotlin
package io.github.panda17tk.arpg

import ktx.app.KtxGame
import ktx.app.KtxScreen

/** Application root. Owns screen transitions (spec §4). */
class App : KtxGame<KtxScreen>() {
    override fun create() {
        addScreen(GameScreen())
        setScreen<GameScreen>()
    }
}
```

- [ ] **Step 6: Run the core test**

Run (PowerShell):
```powershell
cd V:\src\demo0902
.\gradlew.bat :core:test
```
Expected: dependencies download on first run, then `BUILD SUCCESSFUL`; 3 tests pass. If Gradle reports a Kotlin-metadata incompatibility from Fleks, the Fleks pin is too new for Kotlin 2.1.10 — keep `fleksVersion=2.11` (do NOT bump to 2.13).

- [ ] **Step 7: Verify core compiles (incl. libGDX-dependent classes)**

Run (PowerShell):
```powershell
cd V:\src\demo0902
.\gradlew.bat :core:compileKotlin
```
Expected: `BUILD SUCCESSFUL` (proves libGDX/KTX/Fleks/VisUI APIs resolve).

- [ ] **Step 8: Commit**

```bash
git -C V:/src/demo0902 add core
git -C V:/src/demo0902 commit -m "feat(phase0): core module with empty GameScreen and JUnit5 smoke test"
```

---

## Task 6: `desktop` module (dev-only lwjgl3 launcher)

**Files:**
- Create: `desktop/build.gradle.kts`
- Create: `desktop/src/main/kotlin/io/github/panda17tk/arpg/desktop/Lwjgl3Launcher.kt`
- Create: `assets/.gitkeep`

- [ ] **Step 1: Create the shared assets root**

Create an empty file `assets/.gitkeep` (the desktop run task uses `assets/` as its working dir; Phase 0 uses the built-in font so no asset files are needed yet).

- [ ] **Step 2: Create `desktop/build.gradle.kts`**

```kotlin
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm")
    application
}

val gdxVersion: String by project

dependencies {
    implementation(project(":core"))
    implementation("com.badlogicgames.gdx:gdx-backend-lwjgl3:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-desktop")
    implementation("com.badlogicgames.gdx:gdx-freetype-platform:$gdxVersion:natives-desktop")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

application {
    // NOTE: top-level main() in Lwjgl3Launcher.kt compiles to class Lwjgl3LauncherKt.
    mainClass.set("io.github.panda17tk.arpg.desktop.Lwjgl3LauncherKt")
}

tasks.named<JavaExec>("run") {
    workingDir = rootProject.file("assets")
    isIgnoreExitValue = true
}
```

- [ ] **Step 3: Create `desktop/src/main/kotlin/io/github/panda17tk/arpg/desktop/Lwjgl3Launcher.kt`**

```kotlin
package io.github.panda17tk.arpg.desktop

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import io.github.panda17tk.arpg.App
import io.github.panda17tk.arpg.core.Constants

/** Development-only desktop launcher (spec §3, §13). The product is Android-only. */
fun main() {
    val config = Lwjgl3ApplicationConfiguration().apply {
        setTitle("${Constants.TITLE} (dev)")
        setWindowedMode(1280, 720)
        useVsync(true)
        setForegroundFPS(60)
    }
    Lwjgl3Application(App(), config)
}
```

- [ ] **Step 4: Verify desktop compiles**

Run (PowerShell):
```powershell
cd V:\src\demo0902
.\gradlew.bat :desktop:compileKotlin
```
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Smoke-launch the desktop app (bounded), confirm GL init, then close**

Run (PowerShell) — starts the app, waits 6 seconds, then terminates so it doesn't block:
```powershell
cd V:\src\demo0902
$p = Start-Process -FilePath ".\gradlew.bat" -ArgumentList ":desktop:run" -PassThru -RedirectStandardError "$env:TEMP\gdx-run-err.txt" -RedirectStandardOutput "$env:TEMP\gdx-run-out.txt"
Start-Sleep -Seconds 6
if (-not $p.HasExited) { Stop-Process -Id $p.Id -Force }
Get-Content "$env:TEMP\gdx-run-err.txt" -Tail 20
```
Expected: a 1280×720 window titled "ARPG Survival (dev)" appears showing the dark screen + label; no fatal exception in the error log (a benign termination message from the forced stop is fine). If a `GLFW`/display error appears, note that this host may be headless for GL — in that case rely on `:desktop:installDist` building successfully as the proxy and verify the window on a machine with a display.

- [ ] **Step 6: Commit**

```bash
git -C V:/src/demo0902 add desktop assets
git -C V:/src/demo0902 commit -m "feat(phase0): desktop lwjgl3 dev launcher + shared assets root"
```

---

## Task 7: `android` module (distributable APK)

**Files:**
- Create: `android/build.gradle.kts`
- Create: `android/src/main/AndroidManifest.xml`
- Create: `android/src/main/kotlin/io/github/panda17tk/arpg/android/AndroidLauncher.kt`
- Create: `android/src/main/res/drawable/ic_launcher.xml`

- [ ] **Step 1: Verify the Android SDK has platform 35 + build-tools and licenses are accepted**

Run (PowerShell):
```powershell
$sdk = "C:\Users\banti\AppData\Local\Android\Sdk"
& "$sdk\cmdline-tools\latest\bin\sdkmanager.bat" --list_installed 2>$null | Select-String "platforms;android-35|build-tools;35|platform-tools"
```
Expected: lines for `platforms;android-35` and a `build-tools;35.x.x` and `platform-tools`. If `platforms;android-35` is missing, install + accept licenses:
```powershell
& "$sdk\cmdline-tools\latest\bin\sdkmanager.bat" "platform-tools" "platforms;android-35" "build-tools;35.0.0"
& "$sdk\cmdline-tools\latest\bin\sdkmanager.bat" --licenses
```
(If `cmdline-tools\latest` is absent, use the versioned folder under `cmdline-tools\`.)

- [ ] **Step 2: Create `android/build.gradle.kts`**

```kotlin
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    kotlin("android")
}

val gdxVersion: String by project

val natives: Configuration by configurations.creating

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

    sourceSets["main"].assets.srcDir(rootProject.file("assets"))

    buildTypes {
        named("release") {
            isMinifyEnabled = false
            // Signing config is wired in CI (Task 8) when keystore secrets exist.
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(project(":core"))
    implementation("com.badlogicgames.gdx:gdx-backend-android:$gdxVersion")

    val abis = listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
    for (abi in abis) {
        natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-$abi")
        natives("com.badlogicgames.gdx:gdx-freetype-platform:$gdxVersion:natives-$abi")
    }
}

// Extract libGDX native .so files into the per-ABI jniLibs folders.
tasks.register("copyAndroidNatives") {
    val nativesConfig = configurations["natives"]
    val outputRoot = layout.projectDirectory.dir("src/main/jniLibs")
    doFirst {
        nativesConfig.files.forEach { jar ->
            val abi = jar.name.substringAfter("natives-").substringBeforeLast(".jar")
            val outDir = outputRoot.dir(abi).asFile
            outDir.mkdirs()
            copy {
                from(zipTree(jar)) { include("*.so") }
                into(outDir)
            }
        }
    }
}

tasks.whenTaskAdded {
    if (name.contains("merge") && name.contains("JniLibFolders")) {
        dependsOn("copyAndroidNatives")
    }
}
```

- [ ] **Step 3: Create `android/src/main/AndroidManifest.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.VIBRATE" />
    <uses-feature android:glEsVersion="0x00020000" android:required="true" />

    <application
        android:allowBackup="true"
        android:icon="@drawable/ic_launcher"
        android:label="ARPG Survival"
        android:theme="@android:style/Theme.DeviceDefault.NoActionBar.Fullscreen">

        <activity
            android:name=".AndroidLauncher"
            android:exported="true"
            android:label="ARPG Survival"
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

- [ ] **Step 4: Create `android/src/main/kotlin/io/github/panda17tk/arpg/android/AndroidLauncher.kt`**

```kotlin
package io.github.panda17tk.arpg.android

import android.os.Bundle
import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import io.github.panda17tk.arpg.App

/** Distributable Android launcher (spec §4). */
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

- [ ] **Step 5: Create `android/src/main/res/drawable/ic_launcher.xml` (single vector icon, valid for minSdk 24)**

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path android:fillColor="#10131A" android:pathData="M0,0h108v108h-108z" />
    <path android:fillColor="#E0457B" android:pathData="M54,28 L74,80 L54,66 L34,80 Z" />
</vector>
```

- [ ] **Step 6: Build the debug APK**

Run (PowerShell):
```powershell
cd V:\src\demo0902
.\gradlew.bat :android:assembleDebug
```
Expected: `BUILD SUCCESSFUL`; APK at `android/build/outputs/apk/debug/android-debug.apk`.

- [ ] **Step 7: Verify the APK exists and contains native libraries**

Run (PowerShell):
```powershell
cd V:\src\demo0902
$apk = "android\build\outputs\apk\debug\android-debug.apk"
Test-Path $apk
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [System.IO.Compression.ZipFile]::OpenRead((Resolve-Path $apk))
$zip.Entries | Where-Object { $_.FullName -like "lib/*/*.so" } | Select-Object -ExpandProperty FullName
$zip.Dispose()
```
Expected: `True`, and entries such as `lib/arm64-v8a/libgdx.so`, `lib/arm64-v8a/libgdx-freetype.so`, etc. If no `.so` entries appear, the `copyAndroidNatives` hook did not run — re-check the `whenTaskAdded` matcher against the actual merge task name via `.\gradlew.bat :android:tasks --all | findstr JniLib`.

- [ ] **Step 8: Commit**

```bash
git -C V:/src/demo0902 add android
git -C V:/src/demo0902 commit -m "feat(phase0): android module (manifest, launcher, natives copy) building debug APK"
```

---

## Task 8: CI skeleton (Maven → Gradle)

**Files:**
- Delete: `.github/workflows/build.yml`, `.github/workflows/android.yml`, `.github/workflows/release.yml`
- Create: `.github/workflows/ci.yml`, `.github/workflows/release.yml`

- [ ] **Step 1: Remove the legacy workflows**

```bash
git -C V:/src/demo0902 rm .github/workflows/build.yml .github/workflows/android.yml .github/workflows/release.yml
```

- [ ] **Step 2: Create `.github/workflows/ci.yml`**

```yaml
name: CI

on:
  push:
    branches: ["**"]
  pull_request:

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: "21"

      - name: Set up Android SDK
        uses: android-actions/setup-android@v3

      - name: Set up Gradle
        uses: gradle/actions/setup-gradle@v4

      - name: Run core tests
        run: ./gradlew :core:test

      - name: Assemble debug APK
        run: ./gradlew :android:assembleDebug

      - name: Upload debug APK
        uses: actions/upload-artifact@v4
        with:
          name: arpg-debug-apk
          path: android/build/outputs/apk/debug/android-debug.apk
          if-no-files-found: error
```

- [ ] **Step 3: Create `.github/workflows/release.yml` (signed release skeleton, gated on secrets)**

```yaml
name: Release

on:
  push:
    tags: ["v*"]

jobs:
  release-apk:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: "21"

      - name: Set up Android SDK
        uses: android-actions/setup-android@v3

      - name: Set up Gradle
        uses: gradle/actions/setup-gradle@v4

      # Signing is enabled once these repo secrets exist:
      #   KEYSTORE_BASE64, KEYSTORE_PASSWORD, KEY_ALIAS, KEY_PASSWORD
      - name: Decode keystore (if present)
        if: ${{ env.KEYSTORE_BASE64 != '' }}
        env:
          KEYSTORE_BASE64: ${{ secrets.KEYSTORE_BASE64 }}
        run: echo "$KEYSTORE_BASE64" | base64 -d > "$RUNNER_TEMP/release.keystore"

      - name: Assemble release APK
        env:
          ARPG_KEYSTORE: ${{ runner.temp }}/release.keystore
          ARPG_KEYSTORE_PASSWORD: ${{ secrets.KEYSTORE_PASSWORD }}
          ARPG_KEY_ALIAS: ${{ secrets.KEY_ALIAS }}
          ARPG_KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}
        run: ./gradlew :android:assembleRelease

      - name: Upload release APK
        uses: actions/upload-artifact@v4
        with:
          name: arpg-release-apk
          path: android/build/outputs/apk/release/*.apk
          if-no-files-found: error
```

- [ ] **Step 4: Validate the workflow YAML locally (syntax only)**

Run (PowerShell):
```powershell
cd V:\src\demo0902
foreach ($f in @(".github/workflows/ci.yml", ".github/workflows/release.yml")) {
  python -c "import sys,yaml; yaml.safe_load(open(sys.argv[1], encoding='utf-8')); print('OK', sys.argv[1])" $f
}
```
Expected: `OK .github/workflows/ci.yml` and `OK .github/workflows/release.yml`. (If Python/PyYAML is unavailable, visually confirm indentation instead.)

- [ ] **Step 5: Commit**

```bash
git -C V:/src/demo0902 add .github/workflows
git -C V:/src/demo0902 commit -m "ci(phase0): replace Maven/Capacitor workflows with Gradle CI + release skeleton"
```

---

## Task 9: Documentation refresh (new stack)

**Files:**
- Modify: `README.md`, `BUILD.md`, `DESIGN.md`

- [ ] **Step 1: Replace `README.md`**

```markdown
# ARPG サバイバル（Android ネイティブ / Kotlin + libGDX）

見下ろし型のウェーブ制ローグライト・サバイバルシューターを、Android 専用ネイティブ
ゲームとして Kotlin + libGDX で再実装するプロジェクト。

> 旧 Web/Java/Capacitor 実装はタグ `legacy-web-v1.1.1` から参照できます。

## 構成（Gradle マルチモジュール）

- `core/`    … プラットフォーム非依存のゲームロジック（libGDX / KTX / Fleks / VisUI / freetype）
- `desktop/` … 開発専用 lwjgl3 ランチャー（PC で高速反復）
- `android/` … 配布対象の Android ランチャー（APK）

## クイックスタート

```bash
# コアのテスト
./gradlew :core:test

# デスクトップ（開発用）で起動
./gradlew :desktop:run

# デバッグ APK をビルド
./gradlew :android:assembleDebug
```

詳細は **[BUILD.md](BUILD.md)**、設計は **[docs/superpowers/specs/2026-06-28-android-native-redesign-design.md](docs/superpowers/specs/2026-06-28-android-native-redesign-design.md)** を参照。

## 技術スタック

Gradle 8.11.1 · AGP 8.9.3 · Kotlin 2.1.10 · libGDX 1.13.1 · KTX 1.13.1-rc1 ·
Fleks 2.11 · VisUI 1.5.5 · minSdk 24 / targetSdk 35。
```

- [ ] **Step 2: Replace `BUILD.md`**

```markdown
# ビルド / 実行ガイド（Kotlin + libGDX）

## 前提

- JDK 21（`JAVA_HOME` 設定済み）
- Android SDK（`platforms;android-35`, `build-tools;35.x`, `platform-tools`、ライセンス承諾済み）
- `local.properties` に `sdk.dir` を設定（gitignore 済み）
- 初回ビルドは依存解決のためインターネット接続が必要

Gradle 本体はリポジトリ同梱の Wrapper（`./gradlew`、Gradle 8.11.1）を使用します。

## よく使うコマンド

```bash
# コア（ロジック）の JUnit5 テスト
./gradlew :core:test

# デスクトップ（開発専用）で起動
./gradlew :desktop:run

# デバッグ APK（→ android/build/outputs/apk/debug/android-debug.apk）
./gradlew :android:assembleDebug

# 署名 release APK（CI でキーストア secret 設定後に有効）
./gradlew :android:assembleRelease

# すべて検証
./gradlew clean check assembleDebug
```

## 端末へのインストール

```bash
adb install -r android/build/outputs/apk/debug/android-debug.apk
```

## CI（GitHub Actions）

- `ci.yml`: push / PR で `:core:test` と `:android:assembleDebug` を実行し、デバッグ APK を成果物化。
- `release.yml`: `v*` タグで `:android:assembleRelease`。署名は `KEYSTORE_BASE64` 他の
  リポジトリ secret 設定後に有効化されます。
```

- [ ] **Step 3: Replace `DESIGN.md` with a pointer to the authoritative spec**

```markdown
# 設計ドキュメント

本プロジェクトの最新かつ正式な設計（アーキテクチャ・移植マップ・フェーズ計画）は
以下の仕様書に集約されています:

- **[docs/superpowers/specs/2026-06-28-android-native-redesign-design.md](docs/superpowers/specs/2026-06-28-android-native-redesign-design.md)**

Phase 0（本環境構築）の実装計画:

- **[docs/superpowers/plans/2026-06-28-phase0-gradle-libgdx-scaffold.md](docs/superpowers/plans/2026-06-28-phase0-gradle-libgdx-scaffold.md)**

> 旧 Web/Java 実装の設計記述は、タグ `legacy-web-v1.1.1` のリビジョンを参照してください。
```

- [ ] **Step 4: Commit**

```bash
git -C V:/src/demo0902 add README.md BUILD.md DESIGN.md
git -C V:/src/demo0902 commit -m "docs(phase0): rewrite README/BUILD/DESIGN for the Kotlin/libGDX stack"
```

---

## Task 10: Phase 0 acceptance

**Files:** none (verification + plan checkbox close-out)

- [ ] **Step 1: Full clean verification**

Run (PowerShell):
```powershell
cd V:\src\demo0902
.\gradlew.bat clean check :android:assembleDebug
```
Expected: `BUILD SUCCESSFUL`; `:core:test` passes; debug APK is produced.

- [ ] **Step 2: Confirm `projects` lists all three modules**

Run (PowerShell):
```powershell
cd V:\src\demo0902
.\gradlew.bat projects
```
Expected: shows `+--- Project ':android'`, `+--- Project ':core'`, `\--- Project ':desktop'`.

- [ ] **Step 3: Confirm acceptance criteria (spec §13 Ph0 done-condition)**

Verify each:
- Empty `GameScreen` launches on desktop (Task 6 Step 5) — window + label visible.
- Debug APK builds and contains native `.so` libs (Task 7 Steps 6–7).
- `:core:test` is green (Task 5 Step 6, Task 10 Step 1).
- Legacy Java/JS/Capacitor/Eclipse/Maven removed; repo is Kotlin/libGDX-only (Task 2), old code referenceable via `legacy-web-v1.1.1` (Task 1).
- CI workflows define `:core:test` + `:android:assembleDebug` on push/PR (Task 8).

- [ ] **Step 4: (Optional) Push the branch**

Only if the user wants it on the remote now:
```bash
git -C V:/src/demo0902 push -u origin redesign/android-native-kotlin
```

- [ ] **Step 5: Install on a connected device (optional manual smoke test)**

If an Android device/emulator is attached:
```powershell
adb devices
adb install -r "V:\src\demo0902\android\build\outputs\apk\debug\android-debug.apk"
```
Expected: `Success`; launching the app shows the dark screen + "ARPG Survival — Phase 0 scaffold OK" in landscape.

---

## Notes & Fallbacks

- **Kotlin ↔ Gradle warning:** Kotlin 2.1.10's Gradle plugin may print an "untested with Gradle 8.11.1" warning. This is benign. If it becomes a hard error, lower the wrapper to `8.10.2` (still ≥ AGP 8.9.3's minimum is **not** met — AGP 8.9 needs ≥ 8.11.1; in that case instead lower AGP to `8.7.3`, whose minimum Gradle is 8.9).
- **Fleks metadata error:** if `:core` fails with "class was compiled with a newer version of Kotlin," keep `fleksVersion=2.11`. Do not raise to 2.12/2.13 unless the whole project Kotlin version is raised accordingly.
- **Android natives missing from APK:** verify the merge-task name match in `whenTaskAdded` via `:android:tasks --all | findstr JniLib`; adjust the `name.contains(...)` predicate if AGP renamed the task.
- **Headless GL host:** if `:desktop:run` cannot create a GL context on this machine, treat `:desktop:installDist` success as the build proxy and verify the window on a machine with a display.
- **No commits are pushed** unless the user explicitly approves (Tasks 1/10 push steps are optional).
```