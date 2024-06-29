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
   mpsBackendLauncher.builder()
      .withMpsHome(mpsHome)
      .withMpsVersion(mpsVersion) // Optionally specify the MPS version explicitly
      .withJetBrainsJvm() // Optionally request a JetBrains JBR (and fail if it's not available)
      .withTemporaryDirectory(mpsTempDir) // Optionally override the directory where MPS will place its logs and caches.
      .configure(this)

    // Further configuration goes here
}
```

Besides configuring a `JavaExec` task, it is also possible to configure the  `project.javaexec` operation. In this case,
the MPS temporary directory needs to be set explicitly by calling `withTemporaryDirectory()`.

# Effect

The plugin will configure the following:

1. The correct JVM for the MPS version (using Gradle's
   [JVM toolchain support](https://docs.gradle.org/current/userguide/toolchains.html)).
2. Additional JVM arguments such as `--add-opens` or `-Djna.boot.library.path`, as necessary for a particular MPS
   version.

For all versions of MPS:
1. Working directory is set to the temporary directory of the task (`build/tmp/TASK-NAME`) or the specified directory.
2. `idea.config.path` and `idea.system.path` JVM properties are set to subdirectories within the temporary directory.

These changes help isolate individual tasks from each other, potentially enabling parallel execution of tasks.

# Overriding the configured JVM

If you need to override the JVM after it was configured by the backend builder, you have the following options.

* For `JavaExec` task:
   1. Set `javaLauncher` to a different launcher:
      ```java
      mpsBackendBuilder
        .withMpsHome(...)
        ...
        .configure(task);
      task.getJavaLauncher().set(newLauncher);
      ```
   2. Alternatively, set `javaLauncher` to null, then set `executable`:
      ```java
      mpsBackendBuilder
        .withMpsHome(...)
        ...
        .configure(task);
      task.getJavaLauncher().set((JavaLauncher) null);
      task.executable(newExecutable);
      ```
      If `javaLauncher` is not explicitly reset, it takes precedence over `executable`.

* For `project.javaexec` action, set `executable`:
  ```java
  project.javaexec(it -> {
    mpsBackendBuilder...configure(it);
    executable(newExecutable);
  });
  ```
