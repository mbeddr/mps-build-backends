import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    configurations.classpath {
        resolutionStrategy.activateDependencyLocking()
    }
}

plugins {
    kotlin("jvm")
    `maven-publish`
}

val mpsVersion: String by project
val kotlinArgParserVersion: String by project
val kotlinApiVersion: String by project
val kotlinVersion: String by project

val nexusUsername: String? by project
val nexusPassword: String? by project
val fastXmlJacksonVersion: String by project

repositories {
    mavenCentral()
    maven {
        url = uri("https://projects.itemis.de/nexus/content/repositories/mbeddr")
    }
}

dependencies {
    implementation(kotlin("stdlib-jdk8", version = kotlinVersion))
    implementation("com.xenomachina:kotlin-argparser:$kotlinArgParserVersion")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:$fastXmlJacksonVersion")
    compileOnly("com.jetbrains:mps-core:$mpsVersion")
    compileOnly("com.jetbrains:mps-environment:$mpsVersion")
    compileOnly("com.jetbrains:mps-platform:$mpsVersion")
    compileOnly("com.jetbrains:mps-openapi:$mpsVersion")
    compileOnly("com.jetbrains:platform-api:$mpsVersion")
    compileOnly("com.jetbrains:util:$mpsVersion")
    compileOnly("log4j:log4j:1.2.17")
    testImplementation("junit:junit:4.12")
    testImplementation("org.xmlunit:xmlunit-core:2.6.+")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
    kotlinOptions.apiVersion = kotlinApiVersion
    kotlinOptions.allWarningsAsErrors = true
}

publishing {
    publications {
        create<MavenPublication>("projectLoader") {
            from(components["java"])
            versionMapping {
                allVariants {
                    fromResolutionResult()
                }
            }
        }
    }
}
