# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres
to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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
