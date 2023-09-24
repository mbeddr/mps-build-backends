This is a simple Gradle library/plugin for configuring the right Java toolchain and command line flags for a specific
MPS version.

# Usage

```kotlin
plugins {
    id("de.itemis.mps.gradle.launcher")
}

val mpsHome: Provider<File> = ...
val mpsHome: Provider<File> = provider { ... }

tasks.register("myBackendTask", JavaExec::class.java) {
    // Autodetect MPS version from mpsHome
    mpsBackendLauncher.configureJavaForMpsVersion(this, mpsHome)

    // Specify MPS version explicitly
    mpsBackendLauncher.configureJavaForMpsVersion(this, mpsHome, mpsVersion)

    // Further configuration goes here
}
```

# Effect

The plugin will configure the following:

1. The correct JVM for the MPS version (using Gradle's
   [JVM toolchain support](https://docs.gradle.org/current/userguide/toolchains.html)).
2. Additional JVM arguments such as `--add-opens` or `-Djna.boot.library.path`, as necessary for a particular MPS
   version.

For all versions of MPS:
1. Working directory is set to the temporary directory of the task (`build/tmp/TASK-NAME`).
2. `idea.config.path` and `idea.system.path` JVM properties are set to subdirectories within the temporary directory.

These changes help isolate individual tasks from each other, potentially enabling parallel execution of tasks.
