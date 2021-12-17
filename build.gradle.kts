import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackOutput.Target.UMD

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
val serialization_version: String by project

plugins {
    application

    kotlin("multiplatform") version "1.6.10"
    kotlin("plugin.serialization") version "1.6.10"
}

kotlin {
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "1.8"
        }
        withJava()
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }

    js(IR) {
        binaries.executable()
        browser {
            webpackTask {
                output.libraryTarget = UMD
            }
        }
    }

    sourceSets {
        val commonMain by getting
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
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-properties:$serialization_version")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:$datetime_version")

                implementation("ch.qos.logback:logback-classic:$logback_version")
                implementation("io.github.microutils:kotlin-logging:$kotlin_logging_version")
                implementation("io.ktor:ktor-server-cio:$ktor_version")
                implementation("io.ktor:ktor-auth-jwt:$ktor_version")
                implementation("io.ktor:ktor-client-core:$ktor_version")
                implementation("io.ktor:ktor-client-core-jvm:$ktor_version")
                implementation("io.ktor:ktor-client-cio:$ktor_version")
                implementation("io.ktor:ktor-client-auth-jvm:$ktor_version")
                implementation("io.ktor:ktor-client-json-jvm:$ktor_version")
                implementation("io.ktor:ktor-client-mock:$ktor_version")
                implementation("io.ktor:ktor-client-serialization:$ktor_version")
                implementation("io.ktor:ktor-client-gson:$ktor_version")
                implementation("io.ktor:ktor-client-logging-jvm:$ktor_version")
                implementation("io.ktor:ktor-html-builder:$ktor_version")
                implementation("io.ktor:ktor-server-core:$ktor_version")
                implementation("io.ktor:ktor-locations:$ktor_version")
                implementation("io.ktor:ktor-metrics:$ktor_version")
                implementation("io.ktor:ktor-websockets:$ktor_version")
                implementation("io.ktor:ktor-server-host-common:$ktor_version")
                implementation("io.ktor:ktor-serialization:$ktor_version")

                implementation("org.jetbrains.kotlin-wrappers:kotlin-css:$kotlin_css_version")

                implementation("org.graalvm.sdk:graal-sdk:$graal_version")
                implementation("org.graalvm.js:js:$graal_version")
                implementation("org.graalvm.truffle:truffle-api:$graal_version")
                implementation("org.graalvm.truffle:truffle-dsl-processor:$graal_version")

                implementation("commons-codec:commons-codec:1.15")

                implementation("io.lettuce:lettuce-core:$lettuce_version")

                implementation("com.bugsnag:bugsnag:$bugsnag_version")
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation("io.ktor:ktor-server-tests:$ktor_version")

                implementation("io.mockk:mockk:1.12.1")
                implementation("org.jetbrains.kotlin:kotlin-test")
            }
        }
        val jsMain by getting {
            dependencies {
            }
        }
        val jsTest by getting
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
        vendor.set(JvmVendorSpec.matching("GraalVM Community"))
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
    mavenCentral()
    jcenter()
    maven("https://jitpack.io")
}

//val compileKotlin: KotlinCompile by tasks
//compileKotlin.kotlinOptions {
//    freeCompilerArgs = listOf(
//        "-Xinline-classes",
//        "-Xopt-in=kotlin.RequiresOptIn",
//    )
//}

tasks.named<JavaExec>("run") {
    dependsOn(tasks.named<Jar>("jvmJar"))
    classpath(tasks.named<Jar>("jvmJar"))
}

task("stage").dependsOn("installDist")
