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

import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProjectHelper;

/**
 * Construct the RPM file and attaches it as a secondary artifact.
 *
 * @author Brett Okken, Cerner Corp.
 * @since 2.0-beta-2
 */
@Mojo( name = "attached-rpm", defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyResolution = ResolutionScope.RUNTIME, threadSafe = true )
public class AttachedRPMMojo
    extends AbstractRPMMojo
{

    /**
     * The classifier for the rpm secondary artifact.
     */
    @Parameter
    private String classifier;

    @Component
    private MavenProjectHelper mavenProjectHelper;

    /**
     * Attach the rpm as a secondary artifact.
     *
     * @see MavenProjectHelper#attachArtifact(org.apache.maven.project.MavenProject, String, String, java.io.File)
     */
    protected void afterExecution()
    {
        classifier = classifier != null ? classifier : "rpm";
        File attachedArtifact = getRPMFile();
        if ( getLog().isDebugEnabled() )
        {
            getLog().debug( "Adding RPM as attached artifact with '" + classifier + "' classifier: "
                                + attachedArtifact.getAbsolutePath() );
        }
        mavenProjectHelper.attachArtifact( project, "rpm", classifier, attachedArtifact );
    }

    /**
     * Returns the <a href="../../../../../attached-rpm-mojo.html#classifier">classifier</a> for the secondary artifact.
     *
     * @return The classifier.
     */
    protected String getClassifier()
    {
        return classifier;
    }
}
