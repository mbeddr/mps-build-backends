plugins {
    id("kotlin-conventions")
}

tasks {
    val generateBackendClasspathFile = layout.buildDirectory.file("generate-backend-classpath.txt")
    val writeGenerateBackendClasspathFile by registering {
        dependsOn(generateBackend)
        doLast {
            generateBackendClasspathFile.get().asFile.writeText(
                generateBackend.get().files.joinToString(File.pathSeparator))
        }
    }

    test {
        useJUnitPlatform()

        systemProperty("test.projects.root", file("projects"))
        systemProperty("generate.backend.classpath.file", generateBackendClasspathFile.get().asFile)
        systemProperty("junit.jupiter.execution.parallel.enabled", "true")

        val testkitRoot = temporaryDir.resolve("testkit-root")
        systemProperty("testkit.root", testkitRoot)

        dependsOn(writeGenerateBackendClasspathFile)

        maxParallelForks = Runtime.getRuntime().availableProcessors()
    }
}


val generateBackend by configurations.registering {
    isCanBeConsumed = false
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.12.2")
    testImplementation("org.hamcrest:hamcrest:3.0")
    testImplementation(gradleTestKit())

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    generateBackend(project(":execute-generators"))
}
