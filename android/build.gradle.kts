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
        versionCode = 60
        versionName = "2.49.0"
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
    natives("com.badlogicgames.gdx:gdx-freetype-platform:1.13.1:natives-armeabi-v7a")
    natives("com.badlogicgames.gdx:gdx-freetype-platform:1.13.1:natives-arm64-v8a")
    natives("com.badlogicgames.gdx:gdx-freetype-platform:1.13.1:natives-x86")
    natives("com.badlogicgames.gdx:gdx-freetype-platform:1.13.1:natives-x86_64")
}

// Extract libGDX .so natives from the platform jars into src/main/jniLibs/<abi>
// so AGP's jniLibs merge packages them. Runs before preBuild.
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
