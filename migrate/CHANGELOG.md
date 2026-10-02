# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/). This project will adhere
to [Semantic Versioning](https://semver.org/spec/v2.0.0.html) after reaching version 1.0.0. Until then any version may
contain breaking changes.

## Unreleased

### Changed

- Raise the Java source and bytecode target from 11 to 17.
- Raise the Kotlin language and API compatibility baseline from 1.6 to 1.9.

### Dependencies

- Upgrade project-loader and migration-common.

## 0.2.3

### Dependencies

- Upgrade to project-loader 6.0.3 and migration-common 1.1.3 to wait for the project's index roots before the full
  rescan on MPS 2025.2 and newer, fixing searches that returned no matches when the rescan ran before the project's
  files were indexable.

## 0.2.2

### Dependencies

- Upgrade to project-loader 6.0.2 and migration-common 1.1.2 to request a full roots rescan on MPS 2025.2 and newer,
  fixing instance searches that miss existing model nodes after initial indexing completes
  ([MPS-40233](https://youtrack.jetbrains.com/issue/MPS-40233)).

## 0.2.1

### Dependencies

- Upgrade to project-loader 6.0.1 and migration-common 1.1.1 to suppress IntelliJ Platform shutdown logging in quiet mode.

## 0.2.0

### Added

- The backend is quiet by default, supports `--verbose`, and reports the `idea.log` location on failure.

### Dependencies

- Upgrade to project-loader 6.0.0 and migration-common 1.1.0.
# 0.1.0

- Initial version: runs the full MPS migration assistant (project migrations, cleanup migrations, module/language
  migrations and refactoring scripts) on one or several projects headlessly.
