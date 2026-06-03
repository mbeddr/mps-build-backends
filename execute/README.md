# execute

A command-line tool to execute methods from generated Java code.

## Usage

The tool is JVM-based and needs MPS libraries (`${mps_home}/lib/**/*.jar`) on its classpath. The simplest way to run it
is by using Gradle's `JavaExec` task. See below for an example.

The tool requires that the class with the method is generated beforehand. If needed the method has to acquire read and
write locks itself, for example, with `jetbrains.mps.lang.access`.

## Process exit code

If the executed method returns an `int` (`java.lang.Integer`) value, it will be used as the exit code of the process.
Otherwise, if the method runs to completion, it will be treated as a success (exit code 0) regardless of the returned
value, and if an exception is thrown, the exit code will be 255.

## Supported arguments

```
usage: execute [-h] [--plugin PLUGIN]... [--macro MACRO]... [--plugin-location PLUGIN_LOCATION]
               [--plugin-root PLUGIN_ROOT]... [--build-number BUILD_NUMBER] [--test-mode]
               [--environment ENVIRONMENT] [--log-level LOG_LEVEL] [--no-libraries]
               --project PROJECT [--force-indexing FORCE_INDEXING] --module MODULE --class CLASS
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

  --force-indexing FORCE_INDEXING     whether to force full indexing at startup to work around
                                      MPS-37926. Supported values: always, never, auto. Default:
                                      auto.

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

## Maven example

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns="http://maven.apache.org/POM/4.0.0" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.example.mps</groupId>
    <artifactId>mps-build-backends-execute-runner</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>pom</packaging>
    <properties>
        <!-- Version of de.itemis.mps.build-backends:execute -->
        <build.backends.version>REPLACE_WITH_BUILD_BACKENDS_EXECUTE_VERSION</build.backends.version>
        <!-- MPS installation                                          -->
        <mps.home>REPLACE_WITH_ABSOLUTE_PATH_TO_MPS_HOME</mps.home>
        <!-- JNA native library path.
             Linux x86_64 example:
             ${mps.home}/lib/jna/amd64
        -->
        <jna.boot.library.path>${mps.home}/lib/jna/amd64</jna.boot.library.path>
        <!-- Temporary IntelliJ/MPS runtime directories for leater cleanup. -->
        <idea.system.path>${project.build.directory}/idea-system</idea.system.path>
        <idea.config.path>${project.build.directory}/idea-config</idea.config.path>
        <!-- MPS project / plugin / execution configuration             -->
        <!-- Path to the MPS project that should be opened by build-backends. -->
        <mps.project.dir>REPLACE_WITH_PATH_TO_MPS_PROJECT_TO_OPEN</mps.project.dir>
        <!-- Plugin identifier expected by build-backends execute.
             Example shape:
             my.dsl.plugin::my.dsl.plugin
        -->
        <mps.plugin.id>REPLACE_WITH_PLUGIN_ID</mps.plugin.id>
        <!-- Root directory containing the standard MPS bundled plugins. -->
        <mps.plugin.root>${mps.home}/plugins</mps.plugin.root>
        <!-- Location of your custom DSL/plugin under development.
             This can point to an unpacked plugin directory or a directory containing plugins.
             Example:
             ${project.basedir}/target/dependency/local-dsl-plugin
        -->
        <mps.plugin.location>REPLACE_WITH_PATH_TO_CUSTOM_PLUGIN_LOCATION</mps.plugin.location>

        <!-- MPS module containing the class to execute. -->
        <mps.execute.module>REPLACE_WITH_MPS_MODULE_NAME</mps.execute.module>

        <!-- Fully qualified Java class name containing the static method to execute. -->
        <mps.execute.class>REPLACE_WITH_FULLY_QUALIFIED_CLASS_NAME</mps.execute.class>

        <!-- Static method name to invoke. -->
        <mps.execute.method>REPLACE_WITH_METHOD_NAME</mps.execute.method>

        <!-- Argument passed to the String[] args parameter of the executed method.
             The MPS project itself is NOT passed here.
             It is opened from mps.project.dir and passed as the first method parameter.
        -->
        <mps.execute.arg>REPLACE_WITH_ARGUMENT_VALUE</mps.execute.arg>
        <!-- Classpath configuration                                   -->
        <!-- Use ':' on Linux/macOS.
             Use ';' on Windows.
        -->
        <classpath.separator>:</classpath.separator>
        <!-- Directory where build-backends execute and its runtime dependencies are copied. -->
        <build.backends.dependency.dir>
            ${project.build.directory}/dependency/build-backends-execute
        </build.backends.dependency.dir>

        <!-- Additional classpath entries if your setup needs more jars.
             Leave empty if not needed.
             Example Linux/macOS:
             /some/path/to/extra-jars/*
        -->
        <additional.classpath></additional.classpath>
        <!-- Full runtime classpath used to start the build-backends execute CLI.
             If you need additional jars, set additional.classpath above.
             If additional.classpath is empty, the trailing separator is usually harmless.
        -->
        <mps.execute.classpath>
            ${mps.home}/lib/*${classpath.separator}${build.backends.dependency.dir}/*${classpath.separator}:${mps.home}/plugins/*/lib/*${classpath.separator}${mps.home}/lib/modules/*${classpath.separator}${mps.home}/lib/mpsant/*
        </mps.execute.classpath>
    </properties>

    <dependencies>
        <!-- Standalone build-backends execute CLI.
             This dependency provides:
             de.itemis.mps.gradle.execute.MainKt
        -->
        <dependency>
            <groupId>de.itemis.mps.build-backends</groupId>
            <artifactId>execute</artifactId>
            <version>${build.backends.version}</version>
        </dependency>
    </dependencies>
    <build>
        <plugins>
            <!-- Copy build-backends execute and its runtime dependencies -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-dependency-plugin</artifactId>
                <version>3.6.1</version>

                <executions>
                    <execution>
                        <id>copy-build-backends-execute-runtime</id>
                        <phase>generate-resources</phase>
                        <goals>
                            <goal>copy-dependencies</goal>
                        </goals>
                        <configuration>
                            <outputDirectory>${build.backends.dependency.dir}</outputDirectory>
                            <includeScope>runtime</includeScope>
                            <overWriteSnapshots>true</overWriteSnapshots>
                            <overWriteReleases>false</overWriteReleases>
                            <overWriteIfNewer>true</overWriteIfNewer>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
            <!-- Run build-backends execute CLI                          -->
            <plugin>
                <groupId>org.codehaus.mojo</groupId>
                <artifactId>exec-maven-plugin</artifactId>
                <version>3.5.0</version>
                <executions>
                    <execution>
                        <id>run-build-backends-execute</id>
                        <phase>verify</phase>
                        <goals>
                        <!--
                          Have to use `exec:exec` here instead of `exec:java` even though we are running Java, since
                          we need to pass an explicit classpath to java and `exec:java` only lets us use one of the
                          project's dependency scopes.
                         -->
                            <goal>exec</goal>
                        </goals>
                        <configuration>
                            <executable>java</executable>
                            <arguments>
                                <!-- JVM / IntelliJ / MPS runtime properties             -->
                                <argument>-Didea.home.path=${mps.home}</argument>
                                <argument>-Didea.plugins.path=${mps.plugin.root}</argument>
                                <argument>-Didea.system.path=${idea.system.path}</argument>
                                <argument>-Didea.config.path=${idea.config.path}</argument>
                                <argument>-Djna.boot.library.path=${jna.boot.library.path}</argument>
                                <!-- Optional but often useful when running IntelliJ/MPS outside the normal launcher. -->
                                <argument>-Dintellij.platform.load.app.info.from.resources=true</argument>
                                <!-- ================================================= -->
                                <!-- JVM module access flags for modern Java / MPS       -->
                                <!-- ================================================= -->
                                <argument>--add-opens=java.base/java.io=ALL-UNNAMED</argument>
                                <argument>--add-opens=java.base/java.lang=ALL-UNNAMED</argument>
                                <argument>--add-opens=java.base/java.lang.reflect=ALL-UNNAMED</argument>
                                <argument>--add-opens=java.base/java.net=ALL-UNNAMED</argument>
                                <argument>--add-opens=java.base/java.nio=ALL-UNNAMED</argument>
                                <argument>--add-opens=java.base/java.nio.charset=ALL-UNNAMED</argument>
                                <argument>--add-opens=java.base/java.text=ALL-UNNAMED</argument>
                                <argument>--add-opens=java.base/java.time=ALL-UNNAMED</argument>
                                <argument>--add-opens=java.base/java.util=ALL-UNNAMED</argument>
                                <argument>--add-opens=java.base/java.util.concurrent=ALL-UNNAMED</argument>
                                <argument>--add-opens=java.base/java.util.concurrent.atomic=ALL-UNNAMED</argument>
                                <argument>--add-opens=java.base/jdk.internal.vm=ALL-UNNAMED</argument>
                                <argument>--add-opens=java.base/sun.nio.ch=ALL-UNNAMED</argument>
                                <argument>--add-opens=java.base/sun.nio.fs=ALL-UNNAMED</argument>
                                <argument>--add-opens=java.base/sun.security.ssl=ALL-UNNAMED</argument>
                                <argument>--add-opens=java.base/sun.security.util=ALL-UNNAMED</argument>
                                <argument>--add-opens=java.desktop/java.awt=ALL-UNNAMED</argument>
                                <argument>--add-opens=java.desktop/java.awt.dnd.peer=ALL-UNNAMED</argument>
                                <argument>--add-opens=java.desktop/java.awt.event=ALL-UNNAMED</argument>
                                <argument>--add-opens=java.desktop/java.awt.image=ALL-UNNAMED</argument>
                                <argument>--add-opens=java.desktop/java.awt.peer=ALL-UNNAMED</argument>
                                <argument>--add-opens=java.desktop/javax.swing=ALL-UNNAMED</argument>
                                <argument>--add-opens=java.desktop/javax.swing.plaf.basic=ALL-UNNAMED</argument>
                                <argument>--add-opens=java.desktop/javax.swing.text.html=ALL-UNNAMED</argument>
                                <argument>--add-opens=java.desktop/javax.swing.text.html.parser=ALL-UNNAMED</argument>
                                <argument>--add-opens=java.desktop/sun.awt=ALL-UNNAMED</argument>
                                <argument>--add-opens=java.desktop/sun.awt.datatransfer=ALL-UNNAMED</argument>
                                <argument>--add-opens=java.desktop/sun.awt.image=ALL-UNNAMED</argument>
                                <argument>--add-opens=java.desktop/sun.awt.X11=ALL-UNNAMED</argument>
                                <argument>--add-opens=java.desktop/sun.font=ALL-UNNAMED</argument>
                                <argument>--add-opens=java.desktop/sun.java2d=ALL-UNNAMED</argument>
                                <argument>--add-opens=java.desktop/sun.swing=ALL-UNNAMED</argument>
                                <argument>--add-opens=jdk.attach/sun.tools.attach=ALL-UNNAMED</argument>
                                <argument>--add-opens=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED</argument>
                                <argument>--add-opens=jdk.internal.jvmstat/sun.jvmstat.monitor=ALL-UNNAMED</argument>
                                <argument>--add-opens=jdk.jdi/com.sun.tools.jdi=ALL-UNNAMED</argument>
                                <!-- JVM classpath                                      -->
                                <argument>-cp</argument>
                                <argument>${mps.execute.classpath}</argument>
                                <!-- build-backends execute CLI main class              -->
                                <argument>de.itemis.mps.gradle.execute.MainKt</argument>
                                <!-- build-backends execute arguments                   -->
                                <!-- Plugin to load. -->
                                <argument>--plugin</argument>
                                <argument>${mps.plugin.id}</argument>
                                <!-- MPS project to open.
                                     This project is passed to the executed method as the first argument.-->
                                <argument>--project</argument>
                                <argument>${mps.project.dir}</argument>
                                <!-- Module containing the class. -->
                                <argument>--module</argument>
                                <argument>${mps.execute.module}</argument>
                                <!-- Fully qualified class name. -->
                                <argument>--class</argument>
                                <argument>${mps.execute.class}</argument>
                                <!-- Static method name. -->
                                <argument>--method</argument>
                                <argument>${mps.execute.method}</argument>
                                <!-- Base MPS plugin root. -->
                                <argument>--plugin-root</argument>
                                <argument>${mps.plugin.root}</argument>
                                <!-- Additional plugin location containing user/plugin DSLs. -->
                                <argument>--plugin-location</argument>
                                <argument>${mps.plugin.location}</argument>
                                <!-- Argument passed to String[] args of the executed method. -->
                                <argument>--arg</argument>
                                <argument>${mps.execute.arg}</argument>
                            </arguments>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```
