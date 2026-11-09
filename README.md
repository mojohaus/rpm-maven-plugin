# MojoHaus RPM Maven Plugin

[![GitHub CI](https://github.com/mojohaus/rpm-maven-plugin/actions/workflows/maven.yml/badge.svg)](https://github.com/mojohaus/rpm-maven-plugin/actions/workflows/maven.yml)
[![Maven Central](https://img.shields.io/maven-central/v/org.codehaus.mojo/rpm-maven-plugin)](https://central.sonatype.com/artifact/org.codehaus.mojo/rpm-maven-plugin)

The RPM Maven Plugin packages a Maven project's artifacts, dependencies and other files as an RPM.
It writes the spec file from the plugin configuration and calls `rpmbuild` to build the package.

Documentation: <https://www.mojohaus.org/rpm-maven-plugin/>, with the
[usage scenarios](https://www.mojohaus.org/rpm-maven-plugin/usage.html) and the
[parameters](https://www.mojohaus.org/rpm-maven-plugin/params.html).

## Requirements

- Maven 3.9.0 or later and Java 8 or later.
- `rpmbuild` on the `PATH` of the build host. Signing with a passphrase also needs `expect`.

## Building

```shell
mvn verify                 # unit tests; no rpmbuild needed
mvn -P run-its verify      # integration tests; needs rpmbuild
```

See [AGENTS.md](AGENTS.md) for build, test and pull request conventions.

## Releasing

- Make sure `gpg-agent` is running and unlocked.
- Execute `mvn -B release:prepare release:perform`.

To publish the site:

```shell
cd target/checkout
mvn verify site site:stage scm-publish:publish-scm
```

