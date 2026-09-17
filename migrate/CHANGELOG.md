# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/). This project will adhere
to [Semantic Versioning](https://semver.org/spec/v2.0.0.html) after reaching version 1.0.0. Until then any version may
contain breaking changes.

## 0.2.0

### Added

- The backend is quiet by default, supports `--verbose`, and reports the `idea.log` location on failure.

### Dependencies

- Upgrade to project-loader 6.0.0 and migration-common 1.1.0.
# 0.1.0

- Initial version: runs the full MPS migration assistant (project migrations, cleanup migrations, module/language
  migrations and refactoring scripts) on one or several projects headlessly.
