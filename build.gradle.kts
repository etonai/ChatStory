plugins {
    java
    application
}

group = "com.chatstory"
version = "0.1.0-SPIKE"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // jcefmaven wraps java-cef and handles native binary download/extraction automatically.
    // On first run, ~100MB of Chromium natives are downloaded to ./jcef-bundle.
    // Distribution confirmed: Windows x64, Java 21, jpackage-compatible via bundled-natives approach.
    // See: https://github.com/jcefmaven/jcefmaven
    implementation("me.friwi:jcefmaven:146.0.10")
}

application {
    mainClass.set("com.chatstory.spike.Main")
}

tasks.named<JavaExec>("run") {
    // Suppress restricted-method warnings from JCEF native loader on Java 21+
    jvmArgs(
        "--add-opens", "java.desktop/sun.awt=ALL-UNNAMED",
        "--add-opens", "java.desktop/java.awt.peer=ALL-UNNAMED",
        "--enable-native-access=ALL-UNNAMED"
    )
}
