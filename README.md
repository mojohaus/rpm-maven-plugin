# MojoHaus RPM Maven Plugin

This is the [rpm-maven-plugin](http://www.mojohaus.org/rpm-maven-plugin/).
 
[![Build Status](https://travis-ci.org/mojohaus/rpm-maven-plugin.svg?branch=master)](https://travis-ci.org/mojohaus/rpm-maven-plugin)

## Releasing

* Make sure `gpg-agent` is running.
* Execute `mvn -B release:prepare release:perform`

For publishing the site do the following:

```
cd target/checkout
mvn verify site site:stage scm-publish:publish-scm
```