buildscript {
    configurations.classpath {
        resolutionStrategy.activateDependencyLocking()
    }
}

plugins {
    id("kotlin-conventions")
    id("org.jetbrains.kotlinx.binary-compatibility-validator") version "0.13.2"
}

val mpsVersion: String by project
val kotlinArgParserVersion: String by project
val kotlinApiVersion: String by project
val kotlinVersion: String by project

val nexusUsername: String? by project
val nexusPassword: String? by project
val fastXmlJacksonVersion: String by project

// MPS runtime JARs should be available for compiling code and tests but not propagated to consumers.
val mpsRuntime by configurations.creating
val mpsZip by configurations.creating

configurations {

    compileOnly.get().extendsFrom(mpsRuntime)
    testCompileOnly.get().extendsFrom(mpsRuntime)

}

dependencies {
    compileOnly("log4j:log4j:1.2.17")

    api("com.xenomachina:kotlin-argparser:2.0.7")
    api("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:2.11.+")

    mpsRuntime(zipTree({ mpsZip.singleFile }).matching {
        include("lib/mps-core.jar")
        include("lib/mps-environment.jar")
        include("lib/mps-platform.jar")
        include("lib/mps-openapi.jar")
        include("lib/platform-api.jar")
        include("lib/util.jar")
    })

    mpsZip(libs.mps)

    testImplementation("junit:junit:4.13.1")
    testImplementation("org.xmlunit:xmlunit-core:2.6.+")
}

kotlin {
    explicitApi()
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

tasks {
    val mpsHome = layout.buildDirectory.dir("mps")
    val mpsZip by configurations.getting
    val unpackMps by registering(Sync::class) {
        dependsOn(mpsZip)
        from({ mpsZip.resolve().map(project::zipTree) })
        into(mpsHome)
    }
    test {
        dependsOn(unpackMps)
        classpath += mpsHome.get().dir("lib").asFileTree.matching { include("*.jar") }
    }
}
