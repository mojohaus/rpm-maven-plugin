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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.StreamConsumer;

/**
 * Utility that uses <a href="http://expect.nist.gov/">expect</a> to sign rpms automatically (no user interaction).
 * 
 * @author Brett Okken, Cerner Corporation
 * @version $Id$
 * @since 2.0-beta-4
 */
final class RPMSigner
{
    /**
     * GPG name as defined in the rpm database.
     */
    private final String gpgName;

    /**
     * The passphrase for the gpg key.
     */
    private final char[] passphrase;

    /**
     * {@code Log} to log to.
     */
    private final Log log;

    /**
     * Constructor takes all necessary attributes to sign an rpm.
     * 
     * @param gpgName The name of the gpg key in the rpm database.
     * @param passphrase The passphrase for the gpg key.
     * @param log Used for logging information in the signing process.
     */
    public RPMSigner( String gpgName, char[] passphrase, Log log )
    {
        super();
        this.gpgName = gpgName;
        this.passphrase = passphrase;
        this.log = log;
    }

    /**
     * Signs the rpm using the gpgName and passphrase given. All output from the signing will be written to <i>log</i>.
     * @param rpm RPM file to sign. Must exist and be readable.
     * @throws IOException
     */
    public void sign( final File rpm )
        throws IOException
    {
        if ( !rpm.exists() || !rpm.canRead() )
        {
            throw new IllegalStateException( rpm.getAbsolutePath() + " is not a valid rpm file or cannot be read" );
        }       
        
        // use option to provide "script" via stdin
        final Commandline cl = new Commandline();
        cl.setExecutable( "expect" );
        cl.setWorkingDirectory( rpm.getParentFile() );
        cl.createArg().setValue( "-" );

        final StreamConsumer stdout = new LogStreamConsumer( LogStreamConsumer.INFO, log );
        final StreamConsumer stderr = new LogStreamConsumer( LogStreamConsumer.WARN, log );

        try
        {
            if ( log.isDebugEnabled() )
            {
                log.debug( "About to execute \'" + cl.toString() + "\'" );
            }
            
            final InputStream is = new ByteArrayInputStream( writeExpectScriptFile( rpm ) );

            int result = CommandLineUtils.executeCommandLine( cl, is, stdout, stderr );
            if ( result != 0 )
            {
                throw new IllegalStateException( "RPM sign execution returned: \'" + result + "\' executing \'"
                    + cl.toString() + "\'" );
            }
        }
        catch ( CommandLineException e )
        {
            final IllegalStateException ise = new IllegalStateException( "Unable to sign the RPM" );
            ise.initCause( e );
            throw ise;
        }
    }

    /**
     * Writes the expect "script".
     * 
     * @param rpm The rpm to sign.
     * @return The expect script as a {@code byte[]}.
     * @throws IOException
     */
    private byte[] writeExpectScriptFile( final File rpm )
        throws IOException
    {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream( 512 );

        final PrintWriter writer = new PrintWriter( new OutputStreamWriter( baos ) );
        try
        {
            writer.print( "spawn rpm --define \"_gpg_name " );
            writer.print( gpgName );
            writer.print( "\" --addsign " );
            writer.println( rpm.getName() );
            writer.println( "expect -exact \"Enter pass phrase: \"" );
            writer.print( "send -- \"" );
            writer.print( new String( this.passphrase ) );
            writer.println( "\r\"" );
            writer.println( "expect eof" );
            writer.println();

            writer.flush();
        }
        finally
        {
            writer.close();
        }

        return baos.toByteArray();
    }
}
