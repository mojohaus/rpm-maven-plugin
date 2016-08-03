<?xml version="1.0" encoding="UTF-8"?>
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
<faqs id="FAQ" title="Frequently Asked Questions">
  <part id="General">
    <title>General</title>
    <faq id="config">
      <question>How do I mark a file as config?</question>
      <answer>
        <p>
            Use the <a href="map-params.html#configuration">
            configuration</a> option in the <a href="map-params.html">
            Mapping</a>.
        </p>
      </answer>
    </faq>
    <faq id="soft links">
      <question>How do I create softlinks as part of the RPM?</question>
      <answer>
        <p>
            Use the <a href="source-params.html#softlinkSource">
            softlink source</a> option in the <a href="source-params.html">
            Sources</a>. This will create the soft link in %install and list
            the link in the %files.
        </p>
      </answer>
    </faq>
    <faq id="multiple rpms">
        <question>How can I provide multiple rpms (i.e. different architectures) from one project (pom)?</question>
        <answer>
            <p>
                Set the <i>packaging</i> type to <i>pom</i> and use multiple executions
                of the <a href="attached-rpm-mojo.html">attached-rpm</a> goal.<br/>
                In each execution, you need to define a unique
                <a href="attached-rpm-mojo.html#classifier">classifier</a>.
            </p>
        </answer>
    </faq>
    <faq id="calculated version and release">
        <question>Can I access the calculated project version and release attributes?</question>
        <answer>
            <p>
                Yes! The <a href="version-mojo.html">version</a> goal will set the calculated version and release to
                properties. This goal will execute by default as part of the default rpm build lifecycle (for project or 
                packaging type of rpm).
            </p>
        </answer>
    </faq>
    <faq id="cannot resolve artifact">
        <question>When trying to build an RPM I get an exception saying 'Unable to resolve artifact'!</question>
        <answer>
            <p>
                If you want to include an artifact from the same project that you're building the RPM package in,
                the RPM execution needs to be built in Maven's build lifecycle. Otherwise Maven can't resolve the
                project's artifact(s).<br/>
                The easiest way to do this is to bind the rpm goal to Maven's lifecycle as explained on the
                Usage page under <a href="usage.html#RPM_as_Side_Effect">RPM as Side Effect</a>.
            </p>
        </answer>
    </faq>
    <faq id="speed up build">
        <question>My build takes an unexpectedly long time. How can I speed it up?</question>
        <answer>
            <p>
                One possible cause is that your RPM includes mainly JAR files, and RPM by default tries to
                compress them, which is both slow and inefficient.<br/>
                If that is the case, adding the following snippet to the configuration of the plugin will
                solve your problem by disabling compression.
            </p>
            <p><pre><![CDATA[
<defineStatements>
    <defineStatement>_source_payload w0.gzdio</defineStatement>
    <defineStatement>_binary_payload w0.gzdio</defineStatement>
</defineStatements>]]></pre></p>
        </answer>
    </faq>
  </part>
</faqs>
