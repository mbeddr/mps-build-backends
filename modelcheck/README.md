# modelcheck

A command-line tool to execute MPS model checker on a project or a subset thereof and output results as a JUnit XML
report file.

## Usage

The tool is JVM-based and needs on its classpath:

* The MPS libraries (`${mps_home}/lib/**/*.jar`).
* The `mps-httpsupport` plugin (`${mps_home}/plugins/mps-httpsupport/**/*.jar`).
* The `mps-modelchecker` plugin (`${mps_home}/plugins/mps-modelchecker/**/*.jar`).

The simplest way to run it is by using Gradle's `JavaExec` task. See below for an example.

## Supported arguments

```
usage: modelcheck [-h] [--plugin PLUGIN]... [--macro MACRO]... [--plugin-location PLUGIN_LOCATION]
                  [--plugin-root PLUGIN_ROOT]... [--build-number BUILD_NUMBER] [--test-mode]
                  [--environment ENVIRONMENT] [--log-level LOG_LEVEL] [--no-libraries]
                  --project PROJECT [--model MODEL]... [--module MODULE]...
                  [--exclude-model EXCLUDE_MODEL]... [--exclude-module EXCLUDE_MODULE]...
                  [--parallel] [--warning-as-error] [--error-no-fail] [--result-file RESULT_FILE]
                  [--result-format RESULT_FORMAT] [--force-indexing FORCE_INDEXING]

required arguments:
  --project PROJECT                   project to generate from


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

  --log-level LOG_LEVEL               console log level. Supported values: info, warn, error, off.
                                      Default: warn.

  --no-libraries                      do not load project libraries under MPS environment

  --model MODEL                       list of models to check (regexes)

  --module MODULE                     list of modules to check (regexes)

  --exclude-model EXCLUDE_MODEL       list of models to exclude from check (regexes)

  --exclude-module EXCLUDE_MODULE     list of modules to exclude from check (regexes)

  --parallel                          run model checker in parallel

  --warning-as-error                  treat model checker warning as errors

  --error-no-fail                     report errors but don't fail the build

  --result-file RESULT_FILE           stores the result as an JUnit xml file

  --result-format RESULT_FORMAT       reporting format for the JUnit file

  --force-indexing FORCE_INDEXING     whether to force full indexing at startup to work around
                                      MPS-37926. Supported values: always, never, auto. Default:
                                      auto.
```

The `--result-format` option takes one of three values: `model` produces one test per tested model (the default,
deprecated and buggy, see below), `module-and-model` produces one test per module and tested model, `message`
produces one test for each model checker message.

Option `--result-format=model` is deprecated because the resulting XML report does not contain module-level errors.
It is however kept as default for backwards compatibility.

If no models (`--model`) nor modules (`--module`) are given, the entire project will be checked. Models or modules can
be excluded from checking via `--exclude-model` or `--exclude-module` options. All options accept regular expressions
that will be matched against the model or module name (excluding model stereotypes).

All checkers that are registered in the `CheckerRegistry` of MPS will be run.

## Gradle example (Kotlin syntax)

```kotlin
val mps by configurations.creating
val modelcheck by configurations.creating

dependencies {
    mps("com.jetbrains:mps:$mpsVersion@zip")
    modelcheck("de.itemis.mps.build-backends:modelcheck:$buildBackendsVersion")
}

val mpsHome = File(buildDir, "mps")

val unpackMps by tasks.registering(Sync::class) {
    dependsOn(mps)
    from({ mps.resolve().map(project::zipTree) })
    into(mpsHome)
}

val runModelCheck by tasks.registering(JavaExec::class) {
    dependsOn(unpackTask)
    classpath(modelcheck)
    classpath(fileTree(mpsHome) {
        include("lib/**/*.jar")
        // modelcheck uses HttpSupportUtil#getURL()
        include("plugins/mps-httpsupport/**/*.jar")
        include("plugins/mps-modelchecker/**/*.jar")
    })

    mainClass.set("de.itemis.mps.gradle.modelcheck.MainKt")

    args("--project", it.projectDir)
    args("--result-file", file("$buildDir/modelcheck-results.xml"))

    args("--model", "my.model.to.check")
}
```
