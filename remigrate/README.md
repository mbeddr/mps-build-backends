# Remigrate

Execute rerunnable module migrations and project migrations in one or several projects.

## Supported arguments

```
usage: remigrate [-h] [--plugin PLUGIN]... [--macro MACRO]... [--plugin-location PLUGIN_LOCATION]
                 [--plugin-root PLUGIN_ROOT]... [--build-number BUILD_NUMBER] [--test-mode] [--environment ENVIRONMENT]
                 [--log-level LOG_LEVEL] [--verbose] [--no-libraries] [--force-indexing FORCE_INDEXING]
                 [--project PROJECT]... [--exclude-module-migration EXCLUDE_MODULE_MIGRATION]...
                 [--exclude-project-migration EXCLUDE_PROJECT_MIGRATION]...

optional arguments:
  -h, --help                                              show this help message and exit

  --plugin PLUGIN                                         plugin to load. The format is --plugin=<id>::<path>

  --macro MACRO                                           macro to define. The format is --macro=<name>::<value>

  --plugin-location PLUGIN_LOCATION                       location to load additional plugins from

  --plugin-root PLUGIN_ROOT                               directory to search for plugins in. This detection method is
                                                          independent from --plugin and --plugin-location

  --build-number BUILD_NUMBER                             build number used to determine if the plugins are compatible

  --test-mode                                             run in test mode

  --environment ENVIRONMENT                               kind of environment to initialize, supported values are
                                                          'idea' (default), 'mps'

  --log-level LOG_LEVEL                                   console log level. Supported values: all, info, warn, error,
                                                          off. Default: warn.

  --verbose                                               show MPS and IntelliJ Platform log messages on the console

  --no-libraries                                          do not load project libraries under MPS environment

  --force-indexing FORCE_INDEXING                         whether to force full indexing at startup to work around
                                                          MPS-37926. Supported values: always, never, auto. Default:
                                                          auto.

  --project PROJECT                                       project to migrate.

  --exclude-module-migration EXCLUDE_MODULE_MIGRATION     module migration to exclude from execution or check. Format
                                                          is language:version

  --exclude-project-migration EXCLUDE_PROJECT_MIGRATION   ID of project migration to exclude from execution.
```
