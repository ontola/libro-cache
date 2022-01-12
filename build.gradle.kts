val bugsnag_version: String by project
val coroutines_version: String by project
val datetime_version: String by project
val graal_version: String by project
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

    kotlin("multiplatform") version "1.6.10"
    kotlin("plugin.serialization") version "1.6.10"
    id("org.jetbrains.dokka") version "1.6.10"
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
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-properties:$serialization_version")

                implementation("io.ktor:ktor-http:$ktor_version")
                implementation("io.ktor:ktor-utils:$ktor_version")
                implementation("io.ktor:ktor-serialization:$ktor_version")

                implementation("io.ktor:ktor-client-core:$ktor_version")
                implementation("io.ktor:ktor-client-cio:$ktor_version")
                implementation("io.ktor:ktor-client-auth:$ktor_version")
                implementation("io.ktor:ktor-client-json:$ktor_version")
                implementation("io.ktor:ktor-client-mock:$ktor_version")
                implementation("io.ktor:ktor-client-serialization:$ktor_version")
                implementation("io.ktor:ktor-client-content-negotiation:$ktor_version")
                implementation("io.ktor:ktor-client-logging:$ktor_version")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
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
                implementation("io.ktor:ktor-server-call-logging:$ktor_version")
                implementation("io.ktor:ktor-server-forwarded-header:$ktor_version")
                implementation("io.ktor:ktor-server-sessions:$ktor_version")
                implementation("io.ktor:ktor-server-metrics-micrometer:$ktor_version")
                implementation("io.micrometer:micrometer-registry-prometheus:$prometheus_version")

                implementation("io.ktor:ktor-client-cio:$ktor_version")
                implementation("io.ktor:ktor-client-gson:$ktor_version")

                implementation("org.jetbrains.kotlin-wrappers:kotlin-css:$kotlin_css_version")

                implementation("org.graalvm.sdk:graal-sdk:$graal_version")
                implementation("org.graalvm.js:js:$graal_version")
                implementation("org.graalvm.truffle:truffle-api:$graal_version")
                implementation("org.graalvm.truffle:truffle-dsl-processor:$graal_version")

                implementation("commons-codec:commons-codec:1.15")
                implementation("org.apache.commons:commons-text:1.9")

                implementation("io.lettuce:lettuce-core:$lettuce_version")

                implementation("com.bugsnag:bugsnag:$bugsnag_version")
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation("io.ktor:ktor-server-tests:$ktor_version")

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

tasks {
    shadowJar {
        manifest {
            attributes(Pair("Main-Class", "io.ktor.server.cio.EngineMain"))
        }
    }
}

group = "io.ontola"
version = "1.0.0"

application {
    mainClass.set("io.ktor.server.cio.EngineMain")

    if (System.getenv("KTOR_ENV") == "development") {
        applicationDefaultJvmArgs = listOf("-Dio.ktor.development=true")
    }
}

repositories {
    mavenLocal()
    maven("https://maven.pkg.jetbrains.space/public/p/ktor/eap")
    mavenCentral()
    maven("https://jitpack.io")
}

tasks.named<JavaExec>("run") {
    dependsOn(tasks.named<Jar>("jvmJar"))
    classpath(tasks.named<Jar>("jvmJar"))
}

task("stage").dependsOn("shadowJar")
