# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres
to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## 2.5.1

### Fixed

- Temporary directory of any `Task` passed to `configure` will be used, not just if the task is a `JavaExec`. 

## 2.5.0

### Changed

- `MpsBackendBuilder#configure` now accepts the more general interface `JavaForkOptions` instead of `JavaExecSpec`. It
  now can be used to configure the Gradle `Test` task, for example.

## 2.4.2

### Fixed

- Reverted to the previous behavior (as in 2.4.0) because the original behavior makes more sense and is relied upon in
  existing code. Documented the existing behavior and added tests for it.

## 2.4.1

### Fixed

- The launcher configured by `MpsBackendBuilder` will no longer override the launcher that was configured explicitly on
  a `JavaExec` task.

## 2.4.0

### Added

- `MpsBackendBuilder#withJavaLauncher` methods to specify the Java executable via a `JavaLauncher` object (or a provider
  thereof).

## 2.3.0

### Added

- Support for MPS 2023.3 (specifying `-Dintellij.platform.load.app.info.from.resources=true` JVM argument).

## 2.2.0

### Added

- `MpsBackendBuilder#withJavaExecutable(Provider<String>)` method to use a specific Java executable.

## 2.1.0

### Added

- Made `MpsVersionDetection` class public because it may be needed to specify the convention for MPS version in
  downstream plugins.

### Fixed

- Removed premature checks for `mpsHome` and `mpsVersion` being present.

## 2.0.0

### Changed

- The API is reworked to the Builder pattern to make the configuration more flexible. By default, the requested JVM
  toolchain no longer has to be the JetBrains one, JetBrains as a vendor has to be requested explicitly.

## 1.0.0

- Initial release.
