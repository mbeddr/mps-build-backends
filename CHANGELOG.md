# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres
to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## 1.12

### Added

- `generate`: added `--no-strict-mode` option. Strict mode places additional limitations on generators, but
  is required for parallel generation.

## 1.11

`project-loader` library now has a separate version number and a [separate changelog](project-loader/CHANGELOG.md)
because it had to receive some backwards incompatible changes.

### Added

- MPS 2022.x is now supported:
  * commons-logging is used to support both log4j (MPS 2021.3 and below) and java.util.logging (MPS 2022.2 and above)
    frameworks.
  * `generate`: The new API in 2022.x to determine applicable facets is supported.
  * Note that MPS 2022.2 seems to have a race condition which makes it wait 100 seconds on startup. A workaround is to
    start the backend in test mode (`--test-mode`). This is reported to JetBrains as
    [MPS-35992](https://youtrack.jetbrains.com/issue/MPS-35992/MPSHeadlessPlatformStarter-race-condition-causes-unnecessary-wait).

## 1.10

### Added

- `project-loader`: new `ProjectLoader` and related classes using builder pattern to simplify maintaining backward
  compatibility in the future.

### Deprecated

- `project-loader`: global methods `executeWithEnvironment` and `executeWithEnvironmentAndProject` are now deprecated,
  use the new `ProjectLoader` class instead.

## 1.9

### Added

- `generate`: when there is nothing to generate the exit code is `254` now, to distinguish it from the general 
  generation error (exit code `255`). This is still treated as an error like in the previous versions, which matches 
  MPS behaviour.

### Changed 

- `generate`: exit code for a general MPS error is now `255` on all systems. Previously `-1` was returned, which could
  be interpreted as `255` or `-1` depending on the system.

## 1.8

### Added

- `project-loader`: new overload of `executeWithEnvironmentAndProject` that allows passing an `EnvironmentConfig`,
  new method to create an `EnvironmentConfig` from a list of plugins and macros. These two methods can be used together
  to customize the configuration of the environment beyond the possibilities offered by the existing methods (e.g. one
  can add libraries to the environment).

## 1.7

### Added

- `generate`: option `--parallel-generation-threads` to specify the number of threads to use for parallel generation.
  The default is 0, which means no parallel generation.

## 1.6

### Added

- `generate`: options to include and exclude lists of models and solutions, similar to `modelcheck`.

### Fixed

- `generate`: applicable make facets are now retrieved correctly on MPS 2021.3. 

## 1.5

### Added

- `project-loader`: move argparser and jackson dependencies from 'implementation' to 'api' ('runtime' to 'compile' in
  Maven). 

## 1.4

### Added

- `--log-level` flag to set the default log level of backend-specific loggers (categories starting with
  `de.itemis.mps`). The default log level is `warn` but can be set to `off` for backward compatibility.

## 1.3

### Added

- modelcheck: added `--result-format=module-and-model` to report on both module and model failures. The default option
  `model` is kept for backward compatibility, but a warning is emitted with a hint for migration.

### Changed

- generate: IDEA IconLoader is now activated when run in IDEA environment. This is useful when generating cell
  screenshots using mbeddr documentation language from the command line. If the loader is not activated, any icons are
  replaced by a single black pixel.

## 1.2.6.903d328

### Changed

- modelcheck now reports unresolved references.

## 1.2

### Added

- `--environment` option with two values, `IDEA` (default, for backward compatibility) and `MPS`.

## 1.1

### Added

- modelcheck: can now exclude models or modules from checking.

### Fixed

- If `--warning-as-error` was enabled, success would be erroneously reported there were no warnings, only errors. 

## 1.0

### Added

- Extracted `execute-generators` and `modelcheck` backends along with `project-loader` library from
  [`mbeddr/mps-gradle-plugin`]  (https://github. com/mbeddr/mps-gradle-plugin)
  using [`git-filter-repo`](https://github.com/newren/git-filter-repo) to keep Git commit history.
- Added integration tests (`integration-tests`).
