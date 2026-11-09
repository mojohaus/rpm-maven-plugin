# Operation Parameters

The parameters described in this page control the operation of the plugin.

## workarea

The directory to use to build the RPM. By default, this is a directory
named `rpm` in the project&apos;s build directory.

**NOTE:** The absolute path to the workarea *MUST NOT* have a space in any of the directory names.

Beginning with release 2.0-beta-3, the plugin builds in a sub-directory of the
workarea, named `workarea/<name>` or, when the execution has a classifier,
`workarea/<name>-<classifier>`. The directory is not named after the execution,
so executions with the same `name` and no classifier share one directory and
overwrite each other&apos;s files. Only the `attached-rpm` goal has a classifier.
Give each `attached-rpm` execution that builds a package of the same `name` a
distinct `classifier`.

## defineStatements

A list of %define statements that should be included in the top of the spec file

```xml
<plugin>
  ...
  <configuration>
    ...
    <defineStatements>
      <defineStatement>_unpackaged_files_terminate_build 0</defineStatement>
    </defineStatements>
    ...
  </configuration>
  ...
</plugin>
```

## preambleLines

A list of free-form lines that are written verbatim to the preamble of the spec
file, after the tags the plugin generates and before `%description`. Use it for
tags and macros that have no parameter of their own. The plugin does not check
the lines. The default is no lines.

```xml
<plugin>
  ...
  <configuration>
    ...
    <preambleLines>
      <preambleLine>%{?systemd_requires}</preambleLine>
      <preambleLine>BuildRequires: systemd</preambleLine>
    </preambleLines>
    ...
  </configuration>
  ...
</plugin>
```

## attachSpec

If given the value `true`, the generated spec file is attached to the project as
a secondary artifact with type `spec` and classifier `rpm-spec`, so that
`install` and `deploy` publish it next to the RPM. The default is `false`.

Maven allows one attached artifact per type and classifier, so enable this in a
single execution of a project.

## rpmbuildStage

The stage to build, e.g. the parameters passed to rpmbuild.
By default this is `-bb`: Build a binary package (after doing the %prep, %build, and %install stages).

If you want to build binary and source packages, use `-ba`.

## rpmbuildArgs

A list of additional arguments for `rpmbuild`, given in order and placed just
before the spec file name. Use it for options the plugin has no parameter for,
such as `--rcfile`. An option with a value takes two entries. The plugin does
not check the arguments. The default is no arguments. The command line property
is `rpm.rpmbuildArgs`, a comma-separated list.

```xml
<plugin>
  ...
  <configuration>
    ...
    <rpmbuildArgs>
      <rpmbuildArg>--rcfile</rpmbuildArg>
      <rpmbuildArg>/etc/rpm/custom.rc</rpmbuildArg>
    </rpmbuildArgs>
    ...
  </configuration>
  ...
</plugin>
```

