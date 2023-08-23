# mps-build-backends

Command-line utilities used e.g. by Gradle plugins to generate or check models.

This project contains the following subprojects:

* `project-loader` – a library for command-line utilities providing a function to load MPS with specified plugins and
  macros, and execute code in the context of a running MPS instance, optionally with a particular project open.
* `execute-generators` – a command line tool to execute the MPS generator on given models.
* `modelcheck` – a command line tool to check given models or modules for errors.
* `integration-tests` – tests that exercise `execute-generators` and `modelcheck` on sample projects.

The command-line tools are documented in their respective README.md files:

* [`execute-generators`](execute-generators/README.md)
* [`modelcheck`](modelcheck/README.md)

## Relationship to `mbeddr/mps-gradle-plugin`

This project was extracted from [`mbeddr/mps-gradle-plugin`](https://github.com/mbeddr/mps-gradle-plugin) using
[`git-filter-repo`](https://github.com/newren/git-filter-repo) to keep Git commit history.

The new project will use a different versioning scheme. The original project published separate versions for each MPS
major release (20xx.y). This project will strive to support multiple MPS versions from one code base and will use
semantic versioning instead.

## Supported MPS versions

The current version supports MPS 2021.1 to MPS 2022.3. It may work with earlier MPS versions but is not tested with them.
