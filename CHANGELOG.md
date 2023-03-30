# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres
to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.6]

### Added

- `generator`: extended plugin with the possibility to define black/whitel lists of models/solutions similar like the modelchecking plugin

## [1.5]

### Added

- `project-loader`: move argparser and jackson dependencies from 'implementation' to 'api' ('runtime' to 'compile' in
  Maven). 

## [1.4]

### Added

- `--log-level` flag to set the default log level of backend-specific loggers (categories starting with
  `de.itemis.mps`). The default log level is `warn` but can be set to `off` for backward compatibility.

## [1.3]

### Added

- modelcheck: added `--result-format=module-and-model` to report on both module and model failures. The default option
  `model` is kept for backward compatibility, but a warning is emitted with a hint for migration.

### Changed

- generate: IDEA IconLoader is now activated when run in IDEA environment. This is useful when generating cell
  screenshots using mbeddr documentation language from the command line. If the loader is not activated, any icons are
  replaced by a single black pixel.

## [1.2.6.903d328]

### Changed

- modelcheck now reports unresolved references.

## [1.2]

### Added

- `--environment` option with two values, `IDEA` (default, for backward compatibility) and `MPS`.

## [1.1]

### Added

- modelcheck: can now exclude models or modules from checking.

### Fixed

- If `--warning-as-error` was enabled, success would be erroneously reported there were no warnings, only errors. 

## [1.0]

### Added

- Extracted `execute-generators` and `modelcheck` backends along with `project-loader` library from
  [`mbeddr/mps-gradle-plugin`]  (https://github. com/mbeddr/mps-gradle-plugin)
  using [`git-filter-repo`](https://github.com/newren/git-filter-repo) to keep Git commit history.
- Added integration tests (`integration-tests`).
