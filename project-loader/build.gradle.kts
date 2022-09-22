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

configurations {
    // MPS runtime JARs should be available for compiling code and tests but not propagated to consumers.
    val mpsRuntime by configurations.creating

    compileOnly.get().extendsFrom(mpsRuntime)
    testCompileOnly.get().extendsFrom(mpsRuntime)

    val mpsZip by configurations.creating
}

dependencies {
    val mpsRuntime by configurations.getting

    implementation(kotlin("stdlib-jdk8", version = kotlinVersion))
    implementation("com.xenomachina:kotlin-argparser:$kotlinArgParserVersion")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:$fastXmlJacksonVersion")

    mpsRuntime("com.jetbrains:mps-core:$mpsVersion")
    mpsRuntime("com.jetbrains:mps-environment:$mpsVersion")
    mpsRuntime("com.jetbrains:mps-platform:$mpsVersion")
    mpsRuntime("com.jetbrains:mps-openapi:$mpsVersion")
    mpsRuntime("com.jetbrains:platform-api:$mpsVersion")
    mpsRuntime("com.jetbrains:util:$mpsVersion")
    mpsRuntime("log4j:log4j:1.2.17")

    "mpsZip"("com.jetbrains:mps:$mpsVersion")

    testImplementation("junit:junit:4.12")
    testImplementation("org.xmlunit:xmlunit-core:2.6.+")
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
