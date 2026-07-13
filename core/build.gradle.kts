plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

dependencies {
    api("com.badlogicgames.gdx:gdx:1.13.1")
    api("com.badlogicgames.gdx:gdx-freetype:1.13.1")
    api("com.badlogicgames.gdx-controllers:gdx-controllers-core:2.2.3") // v2.188 ゲームパッド
    api("io.github.quillraven.fleks:Fleks:2.8")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
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

tasks.test {
    useJUnitPlatform()
}
