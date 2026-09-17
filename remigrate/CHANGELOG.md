# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

# 1.1.0

## Added

- The backend is quiet by default, supports `--verbose`, and reports the `idea.log` location on failure.

## Dependencies

- Upgrade to project-loader 6.0.0 and migration-common 1.1.0.
# 1.0.3

## Changed

- extract common code with migrate backend into migration-common folder

# 1.0.2

## Dependencies

- Upgrade to project-loader 5.1.1.

# 1.0.1

## Dependencies

- Use project-loader 5.0.1.

# 1.0.0

## Added

- The backend will now wait until project indices are built for each project before running migrations.

# 0.3.0

## Added

- Support for MPS 2025.1 and .2 prerelease.

# 0.2.0

## Removed

- Support for MPS versions below 2022.3 (due to upgrading of the project-loader dependency)

# 0.1.0

## Changed

- Upgraded to Kotlin 2.1, keeping compatibility with 1.6. 

# 0.0.5

## Fixed

- Updated to run on the current MPS master (2024.2 prerelease).

# 0.0.4

- Renamed from `rerun-migrations` to `remigrate`.
