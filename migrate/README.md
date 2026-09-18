# Migrate

Run the full MPS migration assistant (project migrations, cleanup migrations, and module migrations - including
non-rerunnable and data-requiring ones, plus refactoring scripts) on a project (or multiple projects), completely
headlessly, i.e. without showing the interactive migration wizard.

`migrate` runs everything the interactive "Migration Assistant" would run when opening the project in
the IDE, by driving the same code MPS's own `<migrate>` Ant task uses (`jetbrains.mps.ide.migration.AntTaskExecutionUtil`).

By default, migration proceeds even if the pre-migration consistency check or a dependency check (e.g. a library
that itself needs migrating first) reports a problem - such problems are only logged. Pass
`--halt-on-precheck-failure` and/or `--halt-on-dependency-error` to abort instead when either check finds a problem.

When migrating multiple `--project` entries, the first project that fails stops the whole run by default; pass
`--continue-on-error` to keep processing the remaining projects instead. Either way, the process exits with a
non-zero code if any project failed.

This backend is currently experimental, breaking changes are to be expected until version 1.0.0.

## Prerequisites

`migrate` does not build the project. Languages in the project must already be compiled and have all their dependencies
present, so that they can be loaded. A language that cannot be loaded is silently ignored unless
`--halt-on-precheck-failure` is specified.

## Usage

The tool is JVM-based and needs on its classpath:

- The MPS libraries (`${mps_home}/lib/**/*.jar`).
- The `migrate` backend's own jar, which must also be loaded as a plugin via
  `--plugin=de.itemis.mps.buildbackends.migrate::<path-to-migrate.jar>`.

In addition, the `jetbrains.mps.ide.migration.workbench` plugin (`$mps_home/plugins/mps-migration`) must be loaded,
either via `--plugin` or `--plugin-root`.

The simplest way to run it is by using Gradle's `JavaExec` task. See below for an example.

## Supported arguments

```
usage: migrate [-h] [--plugin PLUGIN]... [--macro MACRO]... [--plugin-location PLUGIN_LOCATION]
               [--plugin-root PLUGIN_ROOT]... [--build-number BUILD_NUMBER] [--test-mode]
               [--environment ENVIRONMENT] [--log-level LOG_LEVEL] [--verbose] [--no-libraries]
               [--force-indexing FORCE_INDEXING] [--project PROJECT]... [--halt-on-precheck-failure]
               [--halt-on-dependency-error] [--continue-on-error]

optional arguments:
  -h, --help                          show this help message and exit

  --plugin PLUGIN                     plugin to load. The format is --plugin=<id>::<path>

  --macro MACRO                       macro to define. The format is --macro=<name>::<value>

  --plugin-location PLUGIN_LOCATION   location to load additional plugins from

  --plugin-root PLUGIN_ROOT           directory to search for plugins in. This detection method is
                                      independent from --plugin and --plugin-location

  --build-number BUILD_NUMBER         build number used to determine if the plugins are compatible

  --test-mode                         run in test mode

  --environment ENVIRONMENT           kind of environment to initialize, supported values are
                                      'idea' (default), 'mps'

  --log-level LOG_LEVEL               console log level. Supported values: all, info, warn, error, off. Default: warn.

  --verbose                           show MPS and IntelliJ Platform log messages on the console

  --no-libraries                      do not load project libraries under MPS environment

  --force-indexing FORCE_INDEXING     whether to force full indexing at startup to work around
                                      MPS-37926. Supported values: always, never, auto. Default:
                                      auto.

  --project PROJECT                   project to migrate. Repeat to migrate multiple projects.

  --halt-on-precheck-failure          halt migration if the pre-migration consistency check (e.g.
                                      unresolved references) reports problems. By default such
                                      problems are logged and migration proceeds anyway.

  --halt-on-dependency-error          halt migration if a dependency (e.g. a library module) hasn't
                                      itself been migrated yet. By default such problems are logged
                                      and migration proceeds anyway.

  --continue-on-error                 when migrating multiple --project entries, continue with the
                                      remaining ones even if migration fails for one of them. By
                                      default the first failure stops the whole run. The process
                                      still exits with a non-zero code if any project failed.
```

At least one `--project` must be given.

## Gradle example (Kotlin syntax)

```kotlin
val mps by configurations.creating
val migrate by configurations.creating

dependencies {
    mps("com.jetbrains:mps:$mpsVersion@zip")
    migrate("de.itemis.mps.build-backends:migrate:$buildBackendsVersion")
}

val mpsHome = File(buildDir, "mps")

val unpackMps by tasks.registering(Sync::class) {
    dependsOn(mps)
    from({ mps.resolve().map(project::zipTree) })
    into(mpsHome)
}

val runMigrate by tasks.registering(JavaExec::class) {
    dependsOn(unpackMps)
    classpath(migrate)
    classpath(fileTree(mpsHome) {
        include("lib/**/*.jar")
    })

    mainClass.set("de.itemis.mps.gradle.migrate.MainKt")

    args("--project", it.projectDir)

    // migrate needs to be loaded as a plugin to find its own worker class at runtime
    // mps-migration (jetbrains.mps.ide.migration.workbench) plugin needs to be loaded because it is a dependency of
    // migrate.
    doFirst {
        args("--plugin=de.itemis.mps.buildbackends.migrate::${migrate.singleFile}")
        args("--plugin=jetbrains.mps.ide.migration.workbench::${mpsHome}/plugins/mps-migration")
    }
}
```
