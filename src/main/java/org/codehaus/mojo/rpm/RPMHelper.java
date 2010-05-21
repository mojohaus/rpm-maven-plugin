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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.StreamConsumer;
import org.codehaus.plexus.util.cli.CommandLineUtils.StringStreamConsumer;

/**
 * Utility to interact with rpm and rpmbuild commands.
 * 
 * @author Brett Okken
 * @version $Id$
 * @since 2.0
 */
final class RPMHelper
{
    private final AbstractRPMMojo mojo;

    /**
     * @param mojo
     */
    public RPMHelper( AbstractRPMMojo mojo )
    {
        super();
        this.mojo = mojo;
    }

    /**
     * Gets the default host vendor for system by executing <i>rpm -E %{_host_vendor}</i>.
     */
    public String getHostVendor()
        throws MojoExecutionException
    {
        final Log log = mojo.getLog();

        final Commandline cl = new Commandline();
        cl.setExecutable( "rpm" );
        cl.addArguments( new String[] { "-E", "%{_host_vendor}" } );

        final StringStreamConsumer stdout = new StringStreamConsumer();
        final StreamConsumer stderr = new LogStreamConsumer( LogStreamConsumer.WARN, log );
        try
        {
            if ( log.isDebugEnabled() )
            {
                log.debug( "About to execute \'" + cl.toString() + "\'" );
            }

            int result = CommandLineUtils.executeCommandLine( cl, stdout, stderr );
            if ( result != 0 )
            {
                throw new MojoExecutionException( "RPM query for default vendor returned: \'" + result
                    + "\' executing \'" + cl.toString() + "\'" );
            }
        }
        catch ( CommandLineException e )
        {
            throw new MojoExecutionException( "Unable to query for default vendor from RPM", e );
        }

        return stdout.getOutput().trim();
    }

    /**
     * Run the external command to build the package.
     * <p>
     * Uses the following attributes from the {@link #mojo}:
     * <ul>
     * <li>{@link AbstractRPMMojo#getBuildroot() build root}</li>
     * <li>{@link AbstractRPMMojo#getKeyname() key name}</li>
     * <li>{@link AbstractRPMMojo#getKeyPassphrase() key passphrase}</li>
     * <li>{@link AbstractRPMMojo#getName() name}</li>
     * <li>{@link AbstractRPMMojo#getRPMFile() rpm file}</li>
     * <li>{@link AbstractRPMMojo#getTargetArch() target arch}</li>
     * <li>{@link AbstractRPMMojo#getTargetOS() target OS}</li>
     * <li>{@link AbstractRPMMojo#getTargetVendor() target vendor}</li>
     * <li>{@link AbstractRPMMojo#getWorkarea() workarea}</li>
     * </ul>
     * </p>
     * 
     * @throws MojoExecutionException if an error occurs
     */
    public void buildPackage()
        throws MojoExecutionException
    {
        final File workarea = mojo.getWorkarea();
        final File f = new File( workarea, "SPECS" );

        final Commandline cl = new Commandline();
        cl.setExecutable( "rpmbuild" );
        cl.setWorkingDirectory( f.getAbsolutePath() );
        cl.createArg().setValue( "-bb" );
        cl.createArg().setValue( "--buildroot" );
        cl.createArg().setValue( mojo.getRPMBuildroot().getAbsolutePath() );
        cl.createArg().setValue( "--define" );
        cl.createArg().setValue( "_topdir " + workarea.getAbsolutePath() );
        cl.createArg().setValue( "--target" );
        cl.createArg().setValue( mojo.getTargetArch() + '-' + mojo.getTargetVendor() + '-' + mojo.getTargetOS() );

        // maintain passive behavior for keyPassphrase not being present
        final String keyname = mojo.getKeyname();
        final Passphrase keyPassphrase = mojo.getKeyPassphrase();
        if ( keyname != null && keyPassphrase == null )
        {
            cl.createArg().setValue( "--define" );
            cl.createArg().setValue( "_gpg_name " + keyname );
            cl.createArg().setValue( "--sign" );
        }

        cl.createArg().setValue( mojo.getName() + ".spec" );

        final Log log = mojo.getLog();

        final StreamConsumer stdout = new LogStreamConsumer( LogStreamConsumer.INFO, log );
        final StreamConsumer stderr = new LogStreamConsumer( LogStreamConsumer.WARN, log );
        try
        {
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
            throw new MojoExecutionException( "Unable to build the RPM", e );
        }

        // now if the passphrase has been provided and we want to try and sign automatically
        if ( keyname != null && keyPassphrase != null )
        {
            RPMSigner signer = new RPMSigner( keyname, keyPassphrase.getPassphrase(), log );

            try
            {
                signer.sign( mojo.getRPMFile() );
            }
            catch ( Exception e )
            {
                throw new MojoExecutionException( "Unable to sign RPM", e );
            }
        }
    }

    /**
     * Evaluates the <i>macro</i> by executing <code>rpm --eval %<i>macro</i></code>.
     * 
     * @param macro The macro to evaluate.
     * @return The result of rpm --eval.
     * @throws MojoExecutionException
     * @since 2.1-alpha-1
     */
    public String evaluateMacro( String macro ) throws MojoExecutionException
    {
        final Commandline cl = new Commandline();
        cl.setExecutable( "rpm" );
        cl.createArg().setValue( "--eval" );
        cl.createArg().setValue( '%' + macro );

        final Log log = mojo.getLog();
        
        final StringStreamConsumer stdout = new StringStreamConsumer();
        final StreamConsumer stderr = new LogStreamConsumer( LogStreamConsumer.WARN, log );
        try
        {
            if ( log.isDebugEnabled() )
            {
                log.debug( "About to execute \'" + cl.toString() + "\'" );
            }

            int result = CommandLineUtils.executeCommandLine( cl, stdout, stderr );
            if ( result != 0 )
            {
                throw new MojoExecutionException( "rpm --eval returned: \'" + result + "\' executing \'"
                    + cl.toString() + "\'" );
            }
        }
        catch ( CommandLineException e )
        {
            throw new MojoExecutionException( "Unable to evaluate macro: " + macro, e );
        }
        
        return stdout.getOutput().trim();
    }
}
