# MojoHaus RPM Maven Plugin

This is the [rpm-maven-plugin](http://www.mojohaus.org/rpm-maven-plugin/).

[![GitHub CI](https://github.com/mojohaus/rpm-maven-plugin/actions/workflows/maven.yml/badge.svg)](https://github.com/mojohaus/rpm-maven-plugin/actions/workflows/maven.yml)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.codehaus.mojo/rpm-maven-plugin/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.codehaus.mojo/rpm-maven-plugin)

## Releasing

* Make sure `gpg-agent` is running.
* Execute `mvn -B release:prepare release:perform`

For publishing the site do the following:

```
cd target/checkout
mvn verify site site:stage scm-publish:publish-scm
```
