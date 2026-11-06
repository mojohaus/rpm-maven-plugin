# Frequently Asked Questions

<!--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->

# Frequently Asked Questions

## General

1. [How do I mark a file as config?](#config)
2. [How do I create softlinks as part of the RPM?](#soft_links)
3. [How can I provide multiple rpms (i.e. different architectures) from one project (pom)?](#multiple_rpms)
4. [Can I access the calculated project version and release attributes?](#calculated_version_and_release)
5. [When trying to build an RPM I get an exception saying 'Unable to resolve artifact'!](#cannot_resolve_artifact)
6. [My build takes an unexpectedly long time. How can I speed it up?](#speed_up_build)

## General

<a id="config"></a>

### How do I mark a file as config?

Use the [configuration](map-params.html#configuration) option in the [Mapping](map-params.html).

<a id="soft_links"></a>
<a id="soft links"></a>

### How do I create softlinks as part of the RPM?

Use the [softlink source](source-params.html#softlinkSource) option in the [Sources](source-params.html). This will create the soft link in %install and list the link in the %files.

<a id="multiple_rpms"></a>
<a id="multiple rpms"></a>

### How can I provide multiple rpms (i.e. different architectures) from one project (pom)?

Set the *packaging* type to *pom* and use multiple executions of the [attached-rpm](attached-rpm-mojo.html) goal.
In each execution, you need to define a unique [classifier](attached-rpm-mojo.html#classifier).

<a id="calculated_version_and_release"></a>
<a id="calculated version and release"></a>

### Can I access the calculated project version and release attributes?

Yes! The [version](version-mojo.html) goal will set the calculated version and release to properties. This goal will execute by default as part of the default rpm build lifecycle (for project or packaging type of rpm).

<a id="cannot_resolve_artifact"></a>
<a id="cannot resolve artifact"></a>

### When trying to build an RPM I get an exception saying 'Unable to resolve artifact'!

If you want to include an artifact from the same project that you're building the RPM package in, the RPM execution needs to be built in Maven's build lifecycle. Otherwise Maven can't resolve the project's artifact(s).
The easiest way to do this is to bind the rpm goal to Maven's lifecycle as explained on the Usage page under [RPM as Side Effect](usage.html#RPM_as_Side_Effect).

<a id="speed_up_build"></a>
<a id="speed up build"></a>

### My build takes an unexpectedly long time. How can I speed it up?

One possible cause is that your RPM includes mainly JAR files, and RPM by default tries to compress them, which is both slow and inefficient.
If that is the case, adding the following snippet to the configuration of the plugin will solve your problem by disabling compression.

```xml
<defineStatements>
    <defineStatement>_source_payload w0.gzdio</defineStatement>
    <defineStatement>_binary_payload w0.gzdio</defineStatement>
</defineStatements>
```

