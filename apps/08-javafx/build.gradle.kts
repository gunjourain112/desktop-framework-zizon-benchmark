plugins {
    application
    id("org.openjfx.javafxplugin") version "0.1.0"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.github.oshi:oshi-core:6.6.3")
}

javafx {
    version = "21"
    modules = listOf("javafx.controls", "javafx.graphics")
}

application {
    mainModule = "com.monitor"
    mainClass = "com.monitor.Main"
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}
