# execute

A command-line tool to execute methods from generated Java code.

## Usage

The tool is JVM-based and needs MPS libraries (`${mps_home}/lib/**/*.jar`) on its classpath. The simplest way to run it
is by using Gradle's `JavaExec` task. See below for an example.

The tool requires that the class with the method is generated beforehand. If needed the method has to acquire 
read and write locks itself, for example with `jetbrains.mps.lang.access`.

## Supported argumnets

```
usage: execute [-h] [--plugin PLUGIN]... [--macro MACRO]... [--plugin-location PLUGIN_LOCATION]
               [--build-number BUILD_NUMBER] --project PROJECT [--test-mode]
               [--environment ENVIRONMENT] [--log-level LOG_LEVEL] --module MODULE --class CLASS
               --method METHOD [--arg ARG]...

required arguments:
  --project PROJECT                   project to generate from

  --module MODULE                     name of the module that contains the class

  --class CLASS                       fully qualified name of the class that contains the method

  --method METHOD                     name of the method to execute. Must be public and
                                      static.Supported method signatures in order of precedence:
                                      (Project, String[]), (Project)


optional arguments:
  -h, --help                          show this help message and exit

  --plugin PLUGIN                     plugin to to load. The format is --plugin=<id>::<path>

  --macro MACRO                       macro to define. The format is --macro=<name>::<value>

  --plugin-location PLUGIN_LOCATION   location to load additional plugins from

  --build-number BUILD_NUMBER         build number used to determine if the plugins are compatible

  --test-mode                         run in test mode

  --environment ENVIRONMENT           kind of environment to initialize, supported values are
                                      'idea' (default), 'mps'

  --log-level LOG_LEVEL               console log level. Supported values: info, warn, error, off.
                                      Default: warn.

  --arg ARG                           list of strings to pass to the method. Allowed only if the
                                      method signature is (Project, String[])
```

## Gradle example (Kotlin syntax)

```kotlin
val mps by configurations.creating
val execute by configurations.creating

dependencies {
    mps("com.jetbrains:mps:$mpsVersion@zip")
    execute("de.itemis.mps.build-backends:execute:$buildBackendsVersion")
}

val mpsHome = File(buildDir, "mps")

val unpackMps by tasks.registering(Sync::class) {
    dependsOn(mps)
    from({ mps.resolve().map(project::zipTree) })
    into(mpsHome)
}

val executeMethod by tasks.registering(JavaExec::class) {
    dependsOn(unpackTask)
    classpath(execute)
    classpath(fileTree(mpsHome) {
        include("lib/**/*.jar")
    })

    mainClass.set("de.itemis.mps.gradle.execute.MainKt")

    args("--project", projectDir)

    args("--module", "my.module.to.execute")
    args("--class", "my.module.to.execute.java.MyClass")
    args("--method", "myMethod")
    
    args("--arg", "first argument")
    args("--arg", "second argument")
}
```