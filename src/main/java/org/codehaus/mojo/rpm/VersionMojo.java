package org.codehaus.mojo.rpm;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.text.MessageFormat;
import java.util.Date;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.mojo.rpm.VersionHelper.RPMVersionableMojo;

/**
 * Makes the rpm version and release attributes available as properties.
 *
 * @author Brett Okken
 * @since 2.0
 */
@Mojo(name = "version", defaultPhase = LifecyclePhase.INITIALIZE, threadSafe = true)
public class VersionMojo extends AbstractMojo implements RPMVersionableMojo {
    /**
     * The maven project.
     */
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    /**
     * The maven session.
     */
    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    private MavenSession session;

    /**
     * The version portion of the RPM file name.
     */
    @Parameter(required = true, alias = "version", property = "rpm.version", defaultValue = "${project.version}")
    private String projversion;

    /**
     * The system property to set the calculated version to.
     */
    @Parameter(required = true, defaultValue = "rpm.version")
    private String versionProperty;

    /**
     * The release portion of the RPM file name.
     * <p>
     * This is an optional parameter. By default, the release will be generated from the modifier portion of the <a
     * href="#projversion">project version</a> using the following rules:
     * <ul>
     * <li>If no modifier exists, the release will be <code>1</code>.</li>
     * <li>If the modifier ends with <i>SNAPSHOT</i>, the timestamp (in UTC) of the build will be appended to end.</li>
     * <li>All instances of <code>'-'</code> in the modifier will be replaced with <code>'_'</code>.</li>
     * <li>If the modifier is numeric and <a href="#numericModifierAsRelease">numericModifierAsRelease</a> is
     * <code>true</code>, it will be used as the release.</li>
     * <li>If a modifier exists, does not end with <i>SNAPSHOT</i>, and is not used as the release, <code>"_1"</code>
     * will be appended to end.</li>
     * </ul>
     * </p>
     */
    @Parameter(property = "rpm.release")
    private String release;

    /**
     * Indicates if an all-numeric modifier of the version, such as the <code>1</code> in <code>1.0.0-1</code>, is
     * used as the release instead of having <code>_1</code> appended to it. With <code>true</code>, version
     * <code>1.0.0-1</code> gives version <code>1.0.0</code> and release <code>1</code>; the default gives release
     * <code>1_1</code>. Only applies if <a href="#release">release</a> is not set.
     *
     * @since 2.5.0
     */
    @Parameter(property = "rpm.numericModifierAsRelease", defaultValue = "false")
    private boolean numericModifierAsRelease;

    /**
     * The system property to set the calculated release to.
     */
    @Parameter(required = true, defaultValue = "rpm.release")
    private String releaseProperty;

    /**
     * {@inheritDoc}
     */
    public void execute() throws MojoExecutionException, MojoFailureException {
        VersionHelper.Version version = new VersionHelper(this).calculateVersion();

        setProperty(versionProperty, version.version);
        setProperty(releaseProperty, version.release);
    }

    private void setProperty(String key, String value) {
        getLog().info(MessageFormat.format("setting [{0}] property to value [{1}].", new Object[] {key, value}));

        project.getProperties().put(key, value);
    }

    /**
     * @return Returns the configured version attribute.
     */
    public final String getVersion() {
        return this.projversion;
    }

    /**
     * @return Returns the configured release attribute.
     */
    public final String getRelease() {
        return this.release;
    }

    /**
     * @return Returns the configured numericModifierAsRelease attribute.
     */
    public final boolean isNumericModifierAsRelease() {
        return this.numericModifierAsRelease;
    }

    public Date getBuildTimestamp() {
        return session.getStartTime();
    }
}
