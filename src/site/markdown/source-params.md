# Source Parameters

The parameters described in this page specify the set of files to be copied
into the work area for inclusion in the RPM package. By default, files which
are related to source control and editor temporary files are excluded.

The set of parameters which define a single file set are contained within a
`source` tag. The set of all `source` tags are contained within
a `sources` tag. A minimal source configuration looks like:

```xml
<plugin>
  ...
  <configuration>
    ...
    <mappings>
      <mapping>
        ...
        <sources>
          <source>
            <location>target/classes</location>
          </source>
        </sources>
        ...
      </mapping>
    </mappings>
    ...
  </configuration>
  ...
</plugin>
```

<a id="location"></a>

## location (**required**)

The file or directory to include. If a directory is specified, all files
and subdirectories are also included. If the path does not start with
`/`, it is taken to be relative to the project's base directory.

Note that the entire source specification is optional, but each source
specification that is present must have this parameter.

<a id="noDefaultExcludes"></a>

## noDefaultExcludes

If given the value `true`, the default exclusions are not used.

<a id="includes"></a>

## includes

Contains one or more `include` elements with a wildcard filename
expression which identifies the files to be included.

<a id="excludes"></a>

## excludes

Contains one or more `exclude` elements with a wildcard filename
expression which identifies the files to be excluded. If
`noDefaultExcludes` is not specified, the default exclusions are added
to this list.

<a id="destination"></a>

## destination (**optional**)

If the **location** is a file (not a directory), this is the name of the file
in the rpm. If this value is not provided, the name of the `file` will
be used.

<a id="filter"></a>

## filter (**optional**)

Will filter the file(s) and replace special values with properties.

<a id="targetArchitecture"></a>

## targetArchitecture (**optional**)

If this is populated, the contents defined by this source will only be included
if the value for targetArchitecture matches the target architecture of the rpm.

Beginning with 2.0-beta-3, this is a regular expression. In order to provide backward
compatibility, both the architecture (defined for MOJO) and this value will be
converted to lower case to make comparison case insensitive.

This can be used to flex the contents of the rpm based on the
[architecture](./rpm-mojo.html#needarch):

```xml
<build>
  <plugins>
    <plugin>
      <groupId>org.codehaus.mojo</groupId>
      <artifactId>rpm-maven-plugin</artifactId>
      ...
      <configuration>
        ...
        <needarch>true</needarch>
        ...
        <mapping>
          ...
          <sources>
            <source>
              <location>src/main/native/x86_64</location>
              <targetArchitecture>x86_64</targetArchitecture>
            </source>
            <source>
              <location>src/main/native/i586</location>
              <targetArchitecture>i586</targetArchitecture>
            </source>
          </sources>
          ...
        </mapping>
        ...
      </configuration>
    </plugin>
    ...
  </plugins>
    ...
</build>
```

<a id="softlinkSource"></a>

## softlinkSource (**optional**)

Provides the opportunity to link to item(s) in the package. The location,
therefore, must be a location that exists after all the mappings with
traditional sources have been created in the `buildroot` directory.

Example: Create a link from `ccc.txt` to `/usr/files/aaa.txt`.

```xml
...
<mapping>
  <directory>/usr/files</directory>
  <sources>
    <softlinkSource>
      <destination>ccc.txt</destination>
      <location>aaa.txt</location>
    </softlinkSource>
  </sources>
</mapping>
```

Example: Create a link from `/usr/bin/script.sh` to `/usr/files/script.sh`.

```xml
...
<mapping>
  <directory>/usr/bin</directory>
  <sources>
    <softlinkSource>
      <location>/usr/files/script.sh</location>
    </softlinkSource>
  </sources>
</mapping>
```

Example: Create a link from `/usr/local/something` to `/usr/local/something-A`

```xml
...
<mapping>
  <directory>/usr/local/something</directory>
  <sources>
    <softlinkSource>
      <location>/usr/local/something-A</location>
    </softlinkSource>
  </sources>
</mapping>
```

<a id="targetOSName"></a>

## targetOSName (**optional**)

A [regular expression](http://java.sun.com/j2se/1.5.0/docs/api/java/util/regex/Pattern.html) that, if populated, indicates
that the files defined are only applicable if the expression matches the [MOJO.targetOS](./ident-params.html#targetOS) value.

```xml
<build>
  <plugins>
    <plugin>
      <groupId>org.codehaus.mojo</groupId>
      <artifactId>rpm-maven-plugin</artifactId>
      ...
      <configuration>
         <mapping>
           ...
            <sources>
              <source>
                <location>src/main/native/x86_64</location>
                <targetArchitecture>x86_64</targetArchitecture>
              </source>
              <source>
                <location>src/main/native/x86</location>
                <targetArchitecture>i.86</targetArchitecture>
              </source>
            </sources>
            ...
          </mapping>
          ...
        </configuration>
      </plugin>
      ...
  </plugins>
  ...
<build>
```

