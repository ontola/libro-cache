import org.jetbrains.dokka.DokkaDefaults.moduleName

val bugsnag_version: String by project
val coroutines_version: String by project
val datetime_version: String by project
val kotlin_css_version: String by project
val kotlin_logging_version: String by project
val kotlin_version: String by project
val ktor_version: String by project
val lettuce_version: String by project
val logback_version: String by project
val prometheus_version: String by project
val serialization_version: String by project

plugins {
    application

    kotlin("multiplatform") version "1.7.0"
    kotlin("plugin.serialization") version "1.7.0"
    id("org.jetbrains.dokka") version "1.7.0"
    id("org.jlleitschuh.gradle.ktlint") version "10.2.1"
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

kotlin {
    jvm {
        application
        compilations.all {
            kotlinOptions.jvmTarget = "1.8"
        }
        withJava()
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }

    sourceSets {
        all {
            languageSettings.optIn("kotlin.RequiresOptIn")
        }

        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$serialization_version")
                implementation("io.github.pdvrieze.xmlutil:serialization:0.84.1")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-properties:$serialization_version")

                implementation("io.ktor:ktor-http:$ktor_version")
                implementation("io.ktor:ktor-utils:$ktor_version")
                implementation("io.ktor:ktor-serialization:$ktor_version")
                implementation("io.ktor:ktor-serialization-kotlinx-json:$ktor_version")

                implementation("io.ktor:ktor-client-core:$ktor_version")
                implementation("io.ktor:ktor-client-cio:$ktor_version")
                implementation("io.ktor:ktor-client-auth:$ktor_version")
                implementation("io.ktor:ktor-client-json:$ktor_version")
                implementation("io.ktor:ktor-client-serialization:$ktor_version")
                implementation("io.ktor:ktor-client-content-negotiation:$ktor_version")
                implementation("io.ktor:ktor-client-logging:$ktor_version")

                implementation("org.jetbrains.kotlin-wrappers:kotlin-css:$kotlin_css_version")

                implementation("com.benasher44:uuid:0.4.1")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("io.ktor:ktor-client-mock:$ktor_version")
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlin_version")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutines_version")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactive:$coroutines_version")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:$datetime_version")

                implementation("ch.qos.logback:logback-classic:$logback_version")
                implementation("io.github.microutils:kotlin-logging:$kotlin_logging_version")

                implementation("io.ktor:ktor-server-core:$ktor_version")
                implementation("io.ktor:ktor-server-auth-jwt:$ktor_version")
                implementation("io.ktor:ktor-server-caching-headers:$ktor_version")
                implementation("io.ktor:ktor-server-cio:$ktor_version")
                implementation("io.ktor:ktor-server-host-common:$ktor_version")
                implementation("io.ktor:ktor-server-hsts:$ktor_version")
                implementation("io.ktor:ktor-server-html-builder:$ktor_version")
                implementation("io.ktor:ktor-server-locations:$ktor_version")
                implementation("io.ktor:ktor-server-metrics:$ktor_version")
                implementation("io.ktor:ktor-server-websockets:$ktor_version")
                implementation("io.ktor:ktor-server-status-pages:$ktor_version")
                implementation("io.ktor:ktor-server-locations:$ktor_version")
                implementation("io.ktor:ktor-server-compression:$ktor_version")
                implementation("io.ktor:ktor-server-default-headers:$ktor_version")
                implementation("io.ktor:ktor-server-call-id:$ktor_version")
                implementation("io.ktor:ktor-server-call-logging:$ktor_version")
                implementation("io.ktor:ktor-server-forwarded-header:$ktor_version")
                implementation("io.ktor:ktor-server-sessions:$ktor_version")
                implementation("io.ktor:ktor-server-metrics-micrometer:$ktor_version")
                implementation("io.micrometer:micrometer-registry-prometheus:$prometheus_version")

                implementation("io.ktor:ktor-client-cio:$ktor_version")
                implementation("io.ktor:ktor-client-gson:$ktor_version")

                implementation("commons-codec:commons-codec:1.15")
                implementation("org.apache.commons:commons-text:1.10.0")

                implementation("io.lettuce:lettuce-core:$lettuce_version")

                implementation("com.bugsnag:bugsnag:$bugsnag_version")
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation("io.ktor:ktor-server-tests:$ktor_version")
                implementation("io.ktor:ktor-client-mock:$ktor_version")

                implementation("io.mockk:mockk:1.12.1")
                implementation("org.jetbrains.kotlin:kotlin-test")
                implementation("it.skrape:skrapeit:1.1.7")
            }
        }
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

tasks.withType<org.jetbrains.dokka.gradle.DokkaTask>().configureEach {
    dokkaSourceSets {
        all {
            includes.from("Module.md")
        }
    }
}

tasks {
    shadowJar {
        manifest {
            attributes(Pair("Main-Class", "io.ktor.server.cio.EngineMain"))
        }
    }
}

group = "tools.empathy.libro"
version = "2.0.0"

application {
    mainClass.set("io.ktor.server.cio.EngineMain")

    val dotenv = file(".env")
    val devEnabledInDotfile = dotenv.exists() && dotenv.readLines().any { it == "KTOR_ENV=development" }

    if (devEnabledInDotfile || System.getenv("KTOR_ENV") == "development") {
        applicationDefaultJvmArgs += "-Dio.ktor.development=true"
    }
}

repositories {
    mavenLocal()
    maven("https://maven.pkg.jetbrains.space/public/p/ktor/eap")
    mavenCentral()
    maven("https://jitpack.io")
}

fun setEnvironmentFromDotEnv(): List<List<String>> {
    val dotenv = file(".env")

    if (!dotenv.exists()) {
        return emptyList()
    }

    return dotenv
        .readLines()
        .filter { it.isNotBlank() && !it.startsWith('#') }
        .map { it.split('=') }
        .filter { (key, value) -> key.isNotBlank() && value.isNotBlank() }
}

fun getEnvVar(name: String): String? = System.getenv(name)
    ?: setEnvironmentFromDotEnv()
        .find { it.first() == name }
        ?.last()

tasks.withType<JavaExec> {
    setEnvironmentFromDotEnv().map { (key, value) ->
        System.setProperty(key, value)
        environment(key, value)
    }
}

tasks.named<JavaExec>("run") {
    dependsOn(tasks.named<Jar>("jvmJar"))
    classpath(tasks.named<Jar>("jvmJar"))
}

configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
    disabledRules.set(setOf("trailing-comma"))
}

task("stage").dependsOn("shadowJar")

task("dev") {
    description = "Builds the documentation and runs the server."
    group = "application"
    dependsOn("dokkaHtml")
    dependsOn("run")
}

tasks.dokkaHtml.configure {
    moduleName.set("Libro server")
}

task("dokkaServe", type = Exec::class) {
    dependsOn("dokkaHtml")

    group = "documentation"

    logger.warn("Serving documentation on http://localhost:36552")
    commandLine("python3", "-m", "http.server", "36552", "--directory", "./build/dokka/html")
}
