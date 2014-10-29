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

import java.io.File;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * Construct the RPM file.
 */
@Mojo( name = "rpm", defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyResolution = ResolutionScope.RUNTIME, threadSafe = true )
public class RPMMojo
    extends AbstractRPMMojo
{
    /**
     * If the {@link org.apache.maven.project.MavenProject#getPackaging() packaging} is <i>rpm</i>, sets the rpm as the
     * primary artifact.
     */
    protected void afterExecution()
    {
        if ( "rpm".equals( project.getPackaging() ) )
        {
            File primaryArtifact = getRPMFile();
            if ( getLog().isDebugEnabled() )
            {
                getLog().debug( "Adding RPM as primary artifact: " + primaryArtifact.getAbsolutePath() );
            }
            project.getArtifact().setFile( getRPMFile() );
        }
        else
        {
            getLog().debug( "RPM not added as an artifact because project's packaging type is not 'rpm'" );
        }
    }
}
