# Project Settings

The following items should be specified in the project descriptor. They
are used in the RPM spec file and influence what is packaged in the RPM.
They are overridable at plugin's configuration

<a id="artifactId"></a>

## artifactId

The name to use for the RPM package. This value cannot contain spaces. The
RPM builder will silently discard everything after the first space.

<a id="description"></a>

## description

The description of the software.

<a id="name"></a>

## name

A one-line description of the software.

<a id="organization"></a>

## organization

The `name` element is used to populate the vendor tag, and the
`url` element is used for the package's URL.
