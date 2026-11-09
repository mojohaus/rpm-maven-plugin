# Agent guide for rpm-maven-plugin

This file is for automated agents and human contributors working on this repository. It records
what the code and the POMs do not make obvious.

## Layout

- One module with four goals in `org.codehaus.mojo.rpm`: `rpm` (`RPMMojo`),
  `attached-rpm` (`AttachedRPMMojo`), `unpack` (`RPMUnpackMojo`) and `version` (`VersionMojo`). The
  `rpm` and `attached-rpm` goals share `AbstractRPMMojo`, write the spec file with `SpecWriter` and
  call `rpmbuild` through `RPMHelper`; `RPMSigner` signs the package.
- Unit tests in `src/test/java` check the spec file and version output and run without `rpmbuild`.
  `src/test/resources/gnupg` is the GnuPG home that the signing ITs use. The integration tests cover
  the rest.
- The site is built from the Markdown pages in `src/site/markdown`; the `.md.vm` pages are
  filtered by Velocity first. Keep every anchor the published site has: write `<a id="X"></a>` on
  its own line, and leave it out where the heading below already generates the id `X`, or
  `mvn site` warns that the anchor is used more than once.

## Build gates

- On a local build Spotless applies the formatting; it also reformats Markdown, so run
  `mvn spotless:apply` after editing the README or this file.
- The parent, `mojo-parent`, sets `maven.compiler.release` to 8, so main code must use the Java 8 API.
- The plugin compiles against Maven 3.9.16 (`mavenVersion`) and declares a literal
  `<prerequisites><maven>3.9.0</maven>`; keep the two apart, so raising `mavenVersion` does not
  raise the prerequisite.

## Integration tests

- They need `rpmbuild` on the `PATH`, and `cpio` for the `verify.groovy` scripts that unpack a
  package. The Ubuntu runner image provides both and macOS and Windows do not, which is why CI runs
  on Linux only. Elsewhere, run them in a Linux container:

  ```shell
  docker run --rm -v "$PWD":/src -v m2-rpm:/root/.m2 -w /src maven:3.9.11-eclipse-temurin-17 \
    bash -c 'apt-get update -qq && apt-get install -y -qq rpm cpio expect && mvn -B -P run-its verify'
  ```
- They live in `src/it`, one directory per test, and run with maven-invoker-plugin in the `run-its`
  profile. The plugin is installed into `target/local-repo` first. The profile also starts
  mrm-maven-plugin, which serves `src/it-repo` as the repository named in
  `repository.proxy.url`; `src/it/settings.xml` points the forked builds at it.
- Run a subset:

  ```shell
  mvn -B -P run-its verify -Dinvoker.test=rpm-1,rpm-artifact
  ```
- `rpm-dirmode` is excluded in the `run-its` profile (`pomExcludes`).
- Write `verify.groovy` so that its last expression is `true`: the existing scripts end with
  `return true`. Invoker fails a script whose last expression is anything other than `true` or
  `null`. Most scripts check files with `assert`; a few throw `AssertionError` from a helper.

## Fixtures and dependencies

- Fixture coordinates that a test only packages and never needs from Central use the `localhost`
  group ID and resolve from the mock repository (`rpm-artifact`, `rpm-dependency-hardlink`,
  `rpm-sign-with-keyname-lookup`).
- Deliberate pins in `.github/dependabot.yml`: `maven-artifact`, `maven-core`, `maven-model`,
  `maven-plugin-api` and `maven-settings` stay below 3.10.0, because the plugin baseline is the
  Maven 3.9.x line. Dependabot excludes `src/it`, `src/it-repo` and `src/test/resources` from
  version updates, but security updates can still propose them; close those pull requests.

## Continuous integration and pull requests

- `.github/workflows/maven.yml` calls the shared `maven-verify.yml@v5` workflow from
  `apache/maven-gh-actions-shared`, with `-P run-its verify` as the verify goal. Branches in this
  repository build under the push event, so look at the push run for a pull request from a
  same-repository branch.
- Pull requests are squash- or rebase-merged; merge commits are disabled. Release Drafter builds
  the release notes from the titles and labels, so the title should describe the change for a
  user.

