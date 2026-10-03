# Changelog

All notable changes to migration-common since version 1.1.2 will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres
to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## 1.1.2

### Dependencies

- Upgrade to project-loader 6.0.2 to request a full roots rescan on MPS 2025.2 and newer, fixing instance searches
  that miss existing model nodes after initial indexing completes
  ([MPS-40233](https://youtrack.jetbrains.com/issue/MPS-40233)).
