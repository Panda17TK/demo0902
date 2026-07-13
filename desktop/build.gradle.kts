plugins {
    kotlin("jvm")
    application
}

dependencies {
    implementation(project(":core"))
    implementation("com.badlogicgames.gdx:gdx-backend-lwjgl3:1.13.1")
    implementation("com.badlogicgames.gdx-controllers:gdx-controllers-desktop:2.2.3") // v2.188 ゲームパッド
    implementation("com.badlogicgames.gdx:gdx-platform:1.13.1:natives-desktop")
    implementation("com.badlogicgames.gdx:gdx-freetype-platform:1.13.1:natives-desktop")
}

// Expose the bundled assets (fonts, etc.) on the desktop runtime classpath.
sourceSets {
    main {
        resources.srcDir(rootProject.file("android/src/main/assets"))
    }
}

// Build on the installed JDK (21) but emit Java 17-compatible bytecode (no toolchain pin).
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

application {
    mainClass.set("io.github.panda17tk.arpg.lwjgl3.Lwjgl3LauncherKt")
}

tasks.test {
    useJUnitPlatform()
}
