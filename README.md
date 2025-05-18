# mps-build-backends

Command-line utilities used e.g. by Gradle plugins to generate or check models.

This project contains the following subprojects:

* `project-loader` – a library for command-line utilities providing a function to load MPS with specified plugins and
  macros, and execute code in the context of a running MPS instance, optionally with a particular project open.
* `execute` - a command line tool to execute a piece of code from an MPS module with a particular project open.
* `execute-generators` – a command line tool to execute the MPS generator on given models.
* `launcher` - a Gradle plugin for configuring the Java toolchain and command line arguments for a particular version
  of MPS.
* `modelcheck` – a command line tool to check given models or modules for errors.
* `remigrate` - a command line tool to run re-runnable migrations on a project (or multiple projects).
* `integration-tests` – tests that exercise the command line tools on sample projects.

The subprojects are documented in their respective README.md files:

* [`execute`](execute/README.md)
* [`execute-generators`](execute-generators/README.md)
* [`launcher`](launcher/README.md)
* [`modelcheck`](modelcheck/README.md)
* [`remigrate`](remigrate/README.md)

## Relationship to `mbeddr/mps-gradle-plugin`

This project was originally extracted from [`mbeddr/mps-gradle-plugin`](https://github.com/mbeddr/mps-gradle-plugin)
using [`git-filter-repo`](https://github.com/newren/git-filter-repo) to keep Git commit history.

## Versioning

This project strives to support multiple MPS versions from one code base and uses semantic versioning.

## Supported MPS versions

See `supportedMpsVersions` in [gradle.properties](gradle.properties#L7) for a list of supported
MPS versions.
