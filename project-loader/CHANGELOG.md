# Changelog

All notable changes to this component (project-loader) since version 2.0.0 will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres
to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

Changes in versions before 2.0.0 are documented in the [root changelog](../CHANGELOG.md).

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
