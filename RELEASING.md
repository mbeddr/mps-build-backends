# Releases and snapshots

## Release units

The projects in this repository are released in units. Each unit has its own version and is released by pushing a tag
`<unit>-<version>`, for example `project-loader-6.1.1`. The units are `launcher`, `project-loader`, `backends`,
`migration-common`, `migrate` and `remigrate`. `backends` covers `execute-generators`, `modelcheck` and `execute`, which
share one version. The [release workflow](.github/workflows/release.yml) maps each unit to its projects.

A unit's version is the `version.*` property in the `gradle.properties` of its directory, or of the root directory for
`project-loader` and `backends`. It is published exactly as written.

## Versions during development

When you change a unit, bump its version in the same pull request and give it a `-SNAPSHOT` suffix, for example
`6.1.1-SNAPSHOT`. Also bump the units that depend on it. After a release, the version stays at the released value until
the next change.

Every successful build of `main` publishes the units that have a `-SNAPSHOT` version to the snapshot repository. Units
with a release version are not published from `main`.

## Releasing a unit

1. Remove the `-SNAPSHOT` suffix from the unit's version, update its changelog, and commit the change to `main`.
2. Optionally, check that the unit can be released, for example `./gradlew :project-loader:checkRelease`. The check
   fails if the version is not `MAJOR.MINOR.PATCH` or if the unit depends on a `-SNAPSHOT` version.
3. Tag the commit and push the tag:

   ```shell
   git tag project-loader-6.1.1
   git push origin project-loader-6.1.1
   ```

   Push tags one at a time. GitHub starts no workflow at all when more than three tags are pushed at once.

The release workflow runs the same check, also comparing the version with the tag, and then publishes the unit to the
release repositories, in one job per repository. It does not run tests, so tag only a commit whose `main` build passed.

Release units in dependency order, because a released unit refers to the versions of its dependencies at the tagged
commit: `project-loader` first, then `migration-common` and `backends`, then `migrate` and `remigrate`.

### If the check fails

Re-running the workflow does not help, because it runs on the same commit. Delete the tag locally and on GitHub, fix the
problem, commit, and tag the new commit:

```shell
git tag -d project-loader-6.1.1
git push origin --delete project-loader-6.1.1
```

### If publishing fails

Re-run only the failed job. Repositories usually refuse to overwrite a released version, so if the job failed after
uploading some of the unit's projects, delete the uploaded version from that repository before re-running the job.

## Repositories and credentials

The repositories and the credentials they use are defined in
[publishing-conventions.gradle.kts](build-logic/src/main/kotlin/publishing-conventions.gradle.kts). The workflows take
the credentials from repository secrets. To publish from your machine, set the Gradle properties named in that file.
