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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.mojo.rpm.VersionHelper.RPMVersionableMojo;

/**
 * Makes the rpm version and release attributes available as properties.
 * 
 * @author Brett Okken
 * @version $Id$
 * @since 2.0
 * @goal version
 * @phase initialize
 */
public class VersionMojo
    extends AbstractMojo
    implements RPMVersionableMojo
{
    /**
     * The maven project.
     * 
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * The version portion of the RPM file name.
     * 
     * @parameter alias="version" expression="${project.version}"
     * @required
     */
    private String projversion;

    /**
     * The system property to set the calculated version to.
     * 
     * @parameter default-value="rpm.version"
     * @required
     */
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
     * <li>If a modifier exists and does not end with <i>SNAPSHOT</i>, <code>"_1"</code> will be appended to end.</li>
     * </ul>
     * </p>
     * 
     * @parameter
     */
    private String release;

    /**
     * The system property to set the calculated release to.
     * 
     * @parameter default-value="rpm.release"
     * @required
     */
    private String releaseProperty;

    /**
     * {@inheritDoc}
     */
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        VersionHelper.Version version = new VersionHelper( this ).calculateVersion();

        setProperty( versionProperty, version.version );
        setProperty( releaseProperty, version.release );
    }

    private void setProperty( String key, String value )
    {
        getLog().info( MessageFormat.format( "setting [{0}] property to value [{1}].", new Object[] { key, value } ) );

        project.getProperties().put( key, value );
    }

    /**
     * @return Returns the configured version attribute.
     */
    public final String getVersion()
    {
        return this.projversion;
    }

    /**
     * @return Returns the configured release attribute.
     */
    public final String getRelease()
    {
        return this.release;
    }
}
