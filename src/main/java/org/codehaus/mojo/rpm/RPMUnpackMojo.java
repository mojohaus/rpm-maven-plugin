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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.StreamConsumer;
import org.codehaus.plexus.util.cli.shell.Shell;

/**
 * Unpack a RPM file. Experimental only. Settings may change in next version
 */
@Mojo( name = "unpack", requiresProject = false, aggregator = true,
        defaultPhase = LifecyclePhase.GENERATE_RESOURCES, threadSafe = true )
public class RPMUnpackMojo
    extends AbstractMojo
{

    /**
     * RPM file to unpack
     *
     * @since 2.2.0
     */
    @Parameter( property = "rpm.file", required = true )
    private File rpmFile;

    /**
     * Directory to unpack to
     *
     * @since 2.2.0
     */
    @Parameter( property = "rpm.unpackDirectory", defaultValue = "${project.build.directory}/rpm/unpack" )
    private File unpackDirectory;

    @Override
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        final Log log = this.getLog();

        unpackDirectory.getParentFile().mkdirs();

        Commandline cl = new Commandline( new Shell() );

        cl.setWorkingDirectory( this.unpackDirectory );
        cl.setExecutable( "sh" );
        cl.createArg().setValue( "-c" );

        String cmd = "'" + "rpm2cpio " + this.rpmFile + " | cpio -idm" + "'";
        if ( this.getLog().isDebugEnabled() )
        {
            cmd = "'" + "rpm2cpio " + this.rpmFile + " | cpio -idmv" + "'";
        }
        cl.createArg().setLine( cmd );

        final StreamConsumer stdout = new LogStreamConsumer( LogStreamConsumer.INFO, getLog() );
        final StreamConsumer stderr = new LogStreamConsumer( LogStreamConsumer.INFO, getLog() );
        try
        {
            log.info( "Unpacking " + this.rpmFile + "to " + this.unpackDirectory + "..." );
            if ( log.isDebugEnabled() )
            {
                log.debug( "About to execute \'" + cl.toString() + "\'" );
            }

            int result = CommandLineUtils.executeCommandLine( cl, stdout, stderr );
            if ( result != 0 )
            {
                throw new MojoExecutionException( "RPM build execution returned: \'" + result + "\' executing \'"
                    + cl.toString() + "\'" );
            }
        }
        catch ( CommandLineException e )
        {
            throw new MojoExecutionException( "Unable to unpack the RPM", e );
        }

    }

}
