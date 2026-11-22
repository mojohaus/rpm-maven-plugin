# Maven RPM Plugin - Usage Scenarios

Usage Scenarios
===============

Listed below are the various scenarios where the RPM plugin can be used
along with the proper way to configure the project(s) involved.

RPM on Demand
=============

In this scenario, an RPM is generated from the project on demand by
executing the goal &quot;`rpm:rpm`&quot; within a project. The project itself
may be configured to produce any sort of packaging, since the RPM is not
the primary artifact produced by the project.

If you want the project&apos;s artifacts packaged in the RPM, you must generate
the RPM as a side effect of the standard lifecycle.

If this goal is run on a project with modules, it runs the
&quot;`rpm:rpm`&quot; goal on each module, rather than packaging the modules into
a single RPM.

RPM as Side Effect
==================

In this scenario, an RPM is generated from the project whenever the project
is packaged. This must be done whenever the project&apos;s primary artifact is
intended to be part of the RPM package but can be done anytime. To set up
this configuration, include the following lines in the plugin definition
in the project&apos;s POM file (see the [sample configuration](./example1.html) for
an example of this):

```xml
<executions>
  <execution>
    <id>generate-rpm</id>
    <goals>
      <goal>rpm</goal>
    </goals>
  </execution>
</executions>
```

RPM as Secondary Artifact
=========================

In this scenario, the rpm is not the primary artifact, but will be deployed as
a secondary artifact.

```xml
<executions>
  <execution>
    <id>attach-rpm</id>
    <goals>
      <goal>attached-rpm</goal>
    </goals>
  </execution>
</executions>
```

RPM as Primary Artifact
=======================

In this scenario, the RPM is the primary artifact of the project and is
created by executing the phase &quot;`package`&quot; within the project. If
properly configured, this allows the user to execute &quot;`mvn package`&quot;
on a top-level project (which has the items to build and the RPM project
as modules) and have Maven package the items to build into jars, wars, ears,
etc. and them assemble the RPM of those items for distribution.

The packaging value should be set to &quot;`rpm`&quot;.

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd ">
    <modelVersion>4.0.0</modelVersion>
    ...
    <packaging>rpm</packaging>
    ...
</project>
```

The extensions tag for this plugin should be marked true.

```xml
<build>
  <plugins>
    <plugin>
      <groupId>org.codehaus.mojo</groupId>
      <artifactId>rpm-maven-plugin</artifactId>
      <version><!-- latest release --></version>
      <extensions>true</extensions>
       ...
    </plugin>
  </plugins>
</build>
```

Calculate the version and release
=================================

The [rpm:version](./version-mojo.html) goal calculates the RPM version and release from the project version, with the
rules of the [version and release parameters](./ident-params.html), and stores them in the project properties
`rpm.version` and `rpm.release` (the `versionProperty` and `releaseProperty` parameters). The `rpm` and
`attached-rpm` goals of the same build use these properties, so other plugins can read the values before the RPM is
built. The goal runs in the `initialize` phase.

```xml
<executions>
  <execution>
    <id>rpm-version</id>
    <goals>
      <goal>version</goal>
    </goals>
  </execution>
</executions>
```

Unpack an RPM
=============

The [rpm:unpack](./unpack-mojo.html) goal unpacks the files of an RPM into a directory, for example to inspect the
result of a build. It is experimental and its settings may change. It calls `rpm2cpio` and `cpio`, so both must be on
the `PATH`.

```shell
mvn rpm:unpack -Drpm.file=target/my.rpm -Drpm.unpackDirectory=target/unpacked
```

Configure GPG passphrase under settings.xml
===========================================

GPG passphrase can be configured under local settings.xml
either in clear or [encrypted](https://maven.apache.org/guides/mini/guide-encryption.html) text. The server id is
`gpg.passphrase`, or the value of the `passphraseServerId` parameter, and the plugin reads it when no
`keyPassphrase` is configured.

```xml
<settings>
  [...]
  <servers>
    [...]
    <server>
      <id>gpg.passphrase</id>
      <passphrase>clear or encrypted text</passphrase>
    </server>
  </servers>
</settings>
```

