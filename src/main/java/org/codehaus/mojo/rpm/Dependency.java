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

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.plugin.MojoExecutionException;

/**
 * A description of the set of project dependencies to include in the mapping. If no includes or excludes are specified,
 * all dependencies will be included in the mapping.
 * <p>
 * Each include or exclude should be specified in the form: "<i>groupID</i><code>:</code><i>artifactID</i>[
 * <code>:</code><i>version</i>]" Any field can be specified as "<code>*</code>" which means any value is a match. If
 * version is omitted (it usually is), it is the same as specifying "<code>*</code>".
 * </p>
 */
public class Dependency
{

    // // // Properties

    /** List of dependencies to include. */
    private List<Artifact> includes;

    /** List of dependencies to exclude. */
    private List<Artifact> excludes;

    /**
     * Strip version is false by default.
     *
     * @since 2.0-beta-4
     */
    private boolean stripVersion = false;

    // // // Bean methods

    /**
     * Retrieve the list of dependencies to include.
     *
     * @return The list of dependencies to include.
     */
    public List<Artifact> getIncludes()
    {
        return includes;
    }

    /**
     * Set the list of dependencies to include.
     *
     * @param incls The new list of dependencies to include.
     * @throws MojoExecutionException if the parse fails
     */
    public void setIncludes( List<String> incls )
        throws MojoExecutionException
    {
        includes = parseList( incls );
    }

    /**
     * Retrieve the list of dependencies to exclude.
     *
     * @return The list of dependencies to exclude.
     */
    public List<Artifact> getExcludes()
    {
        return excludes;
    }

    /**
     * Set the list of dependencies to exclude.
     *
     * @param excls The new list of dependencies to exclude.
     * @throws MojoExecutionException if the parse fails
     */
    public void setExcludes( List<String> excls )
        throws MojoExecutionException
    {
        excludes = parseList( excls );
    }

    /**
     * Retrieve the stripVersion property
     *
     * @return The stripVersion property
     */
    public boolean getStripVersion()
    {
        return stripVersion;
    }

    /**
     * Set the stripVersion property
     *
     * @param stripVersion
     * @throws MojoExecutionException if the parse fails
     */
    public void setStripVersion( boolean stripVersion )
        throws MojoExecutionException
    {
        this.stripVersion = stripVersion;
    }

    // // // Public methods

    /** {@inheritDoc} */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append( "[dependencies" );

        if ( includes != null )
        {
            sb.append( " include [" + includes + "]" );
        }

        if ( excludes != null )
        {
            sb.append( " exclude [" + excludes + "]" );
        }

        sb.append( " stripVersion (" + stripVersion + ")" );

        sb.append( "]" );
        return sb.toString();
    }

    // // // Private methods

    /**
     * Parse the list of dependencies.
     *
     * @param in The list specified in the configuration
     * @return A list of parsed artifact identifiers
     * @throws MojoExecutionException if the parse fails
     */
    private List<Artifact> parseList( List<String> in )
        throws MojoExecutionException
    {
        List<Artifact> retval = new ArrayList<Artifact>();

        for ( String s : in )
        {
            String[] parts = s.split(":");
            // Make sure we have group and artifact
            if(parts.length == 0)
            {
                throw new MojoExecutionException( "Include and exclude must include both group and artifact IDs." );
            }
            String groupId = parts[0];
            String artifactId = parts[1];
            String versionStr = null;
            String type = "";
            String classifier = "";
            VersionRange vr = null;

            if(parts.length > 2)
            {
                versionStr = parts[2];
                if(parts.length > 3)
                {
                    type = parts[3];
                }

                if(parts.length > 4)
                {
                    classifier = parts[4];
                }
            }
            else
            {
                versionStr = "[0,]";
            }

            try
            {
                vr = VersionRange.createFromVersionSpec( versionStr );
            }
            catch ( InvalidVersionSpecificationException ex )
            {
                throw new MojoExecutionException( "Default version string is invalid!" );
            }

            retval.add( new DefaultArtifact( groupId, artifactId, vr, null, type, classifier, null ) );
        }

        return retval;
    }
}