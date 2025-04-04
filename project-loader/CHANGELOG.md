# Changelog

All notable changes to this component (project-loader) since version 2.0.0 will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres
to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

Changes in versions before 2.0.0 are documented in the [root changelog](../CHANGELOG.md).

## 3.0.2

### Fixed

- Use `Application.getInstance().getBuild()` build number for detecting necessary workarounds instead of
  `BuildNumber.currentVersion()` because the former appears to be more correct.

## 3.0.1

### Fixed

- Indexing workaround no longer triggers `java.lang.NoClassDefFoundError: com/intellij/testFramework/IndexingTestUtil`
  on older MPS versions.

## 3.0.0

### Added

- Indexing workaround updated to support 2024.1+ API (`IndexingTestUtil#waitUntilIndexesAreReady`).

### Removed

- Support for Log4J logging (in MPS 2021.3 and below).

## 2.5.0

### Changed

- Upgraded to Kotlin 2.1, keeping compatibility with 1.6.

## 2.4.1

### Fixed

- Project libraries with path relative to `$PROJECT_DIR$` are now correctly processed.
- Found project libraries are now properly logged.

## 2.4.0

### Added

- Helper methods to implement workaround for
  the [MPS-37926](https://youtrack.jetbrains.com/issue/MPS-37926/Indices-not-built-properly-in-IdeaEnvironment) indexing
  bug.

## 2.3.1

### Fixed

- Any exception caught while disposing the environment is now logged and swallowed.

## 2.3.0

### Added

- Load project libraries from `.mps/libraries.xml` under the project directory when using MPS environment (can be
  disabled via `--no-libraries`).
- New `--plugin-root` argument. Automatically detect plugins in subdirectories of plugin roots.

## 2.2.0

### Added

- `EnvironmentArgs` superclass of `Args` for situations where the user can specify no, or multiple projects.

## 2.1.0

### Added

- `checkArgument` function for checking invalid arguments in a consistent way.

## 2.0.0

### Added

- MPS 2022.x is now supported:
  * commons-logging is used to support both log4j (MPS 2021.3 and below) and java.util.logging (MPS 2022.2 and above)
    frameworks.
  * Note that MPS 2022.2 seems to have a race condition which makes it wait 100 seconds on startup. A workaround is to
    start the backend in test mode (`--test-mode`). This is reported to JetBrains as
    [MPS-35992](https://youtrack.jetbrains.com/issue/MPS-35992/MPSHeadlessPlatformStarter-race-condition-causes-unnecessary-wait).

### Changed

- (Breaking change.) Logging subsystem changed and moved from the default package to `de.itemis.mps.gradle.logging`.

### Fixed

- Project is now closed via `Environment#closeProject()` rather than `Project#dispose()` which should fix an exception
  after shutdown (in test mode).

### Deleted

- Classes related to JUnit XML were only useful for the `modelcheck` backend and were moved there.
