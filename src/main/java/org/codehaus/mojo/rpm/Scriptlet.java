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

import org.apache.maven.shared.utils.io.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;

/**
 * Defines a scriptlet including the optinal {@link #getSubpackage()} and {@link #getProgram()}. The (optional) contents
 * can be provided by either {@link #getScript()} or {@link #getScriptFile()}.
 *
 * @author Brett Okken, Cerner Corp.
 * @since 2.0-beta-4
 */
public class Scriptlet
{
    /**
     * Optional subpackage.
     *
     * @see #getSubpackage()
     */
    private String subpackage;

    /**
     * Program to execute script.
     *
     * @see #getProgram()
     */
    private String program;

    /**
     * Contents of the script. Mutually exclusive with {@link #scriptFile}.
     *
     * @see #getScript()
     */
    private String script;

    /**
     * Script to execute. Mutually exclusive with {@link #script}.
     *
     * @see #getScriptFile()
     */
    private String scriptFile;

    /**
     * Encoding of {@link #scriptFile}.
     *
     * @see #getFileEncoding().
     */
    private String fileEncoding;

    /**
     * The default encoding of the project, used if {@link #fileEncoding} is not set.
     *
     * @see #getFileEncoding().
     */
    private String sourceEncoding;

    /**
     * Switch to filter the scriptlet
     *
     * @see #isFilter().
     */
    private boolean filter;

    /**
     * The optional subpackage. This is passed as a <i>-n</i> argument to the scriptlet directive.
     *
     * @return Returns the {@link #subpackage}.
     */
    public String getSubpackage()
    {
        return this.subpackage;
    }

    /**
     * @param subpackage The {@link #subpackage} to set.
     */
    public void setSubpackage( String subpackage )
    {
        this.subpackage = subpackage;
    }

    /**
     * The program to use to execute the script. This is passed as a <i>-p</i> argument to the scriptlet directive.
     *
     * @return Returns the {@link #program}.
     */
    public String getProgram()
    {
        return this.program;
    }

    /**
     * @param program The {@link #program} to set.
     */
    public void setProgram( String program )
    {
        this.program = program;
    }

    /**
     * The contents of the script as a {@code String}. This value will override anything at {@link #getScriptFile()}.
     *
     * @return Returns the {@link #script}.
     */
    public String getScript()
    {
        return this.script;
    }

    /**
     * @param script The {@link #script} to set.
     */
    public void setScript( String script )
    {
        this.script = script;
    }

    /**
     * The contents of the script from a file in the project or in the classpath. This will be ignored if
     * {@link #getScript()} is populated.
     *
     * @return Returns the {@link #scriptFile}.
     */
    public String getScriptFile()
    {
        return this.scriptFile;
    }

    /**
     * @param scriptFile The {@link #scriptFile} to set.
     */
    public void setScriptFile( String scriptFile )
    {
        this.scriptFile = scriptFile;
    }

    /**
     * The encoding to use to read {@link #getScriptFile()}. If {@code null}, the default character encoding for th
     * platform will be used.
     *
     * @return Returns the {@link #fileEncoding}.
     */
    public String getFileEncoding()
    {
        if ( fileEncoding != null && !"".equals( fileEncoding ) )
        {
            return this.fileEncoding;
        }
        return this.sourceEncoding;
    }

    /**
     * @param fileEncoding The {@link #fileEncoding} to set.
     */
    public void setFileEncoding( String fileEncoding )
    {
        this.fileEncoding = fileEncoding;
    }

    /**
     * This is the maven property: project.build.sourceEncoding
     *
     * @param sourceEncoding The {@link #sourceEncoding} to set.
     */
    public void setSourceEncoding( String sourceEncoding )
    {
        this.sourceEncoding = sourceEncoding;
    }

    /**
     * @return Returns the {@link #filter}.
     */
    public boolean isFilter()
    {
        return this.filter;
    }

    /**
     * @param filter The {@link #filter} to set.
     */
    public void setFilter( boolean filter )
    {
        this.filter = filter;
    }

    /**
     * Writes the scriptlet.
     *
     * @param writer {@code PrintWriter} to write content to.
     * @param directive The directive for the scriptlet.
     * @param mojo the {@code AbstractRPMMojo} which contains any resources needed when writing the scriptlet.
     * @throws IOException
     */
    protected final void write( final PrintWriter writer, final String directive,
                                final AbstractRPMMojo mojo ) throws IOException
    {
        if ( script != null || scriptFile != null || program != null )
        {
            writer.println();
            writer.println( buildScriptletLine( directive ) );

            writeContent( writer, mojo );
        }
    }

    /**
     * Builds the scriptlet line including any optional args.
     *
     * @param directive The directive for the scriptlet.
     * @return The scriptlet line - this does not include the script.
     */
    protected String buildScriptletLine( final String directive )
    {
        final StringBuilder builder = new StringBuilder();

        builder.append( directive );
        if ( subpackage != null )
        {
            builder.append( " -n " );
            builder.append( subpackage );
        }

        if ( program != null )
        {
            builder.append( " -p " );
            builder.append( program );
        }

        return builder.toString();
    }

    private Reader getScriptReader( String path, AbstractRPMMojo mojo ) throws FileNotFoundException,
            UnsupportedEncodingException
    {
        String classpathPrefix = "classpath:";
        Reader reader;
        if ( path.startsWith( classpathPrefix ) )
        {
            String classpathResource = path.substring( classpathPrefix.length() );
            InputStream inputStream = Thread.currentThread().getContextClassLoader()
                    .getResourceAsStream( classpathResource );
            if ( inputStream == null )
            {
                throw new RuntimeException( "Invalid scriptlet declaration found - defined scriptFile does not exist: "
                        + path );
            }
            reader = new InputStreamReader( inputStream );
        }
        else
        {
            File file = FileUtils.resolveFile( mojo.getProject().getBasedir(), path );
            if ( !file.exists() )
            {
                throw new RuntimeException( "Invalid scriptlet declaration found - defined scriptFile does not exist: "
                        + path );
            }
            reader = fileEncoding != null ? new InputStreamReader( new FileInputStream( file ), fileEncoding )
                    : new FileReader( file );
        }
        return reader;
    }

    /**
     * Writes the content (either {@link #getScript()} or {@link #getScriptFile()}) to <i>writer</i>.
     *
     * @param writer {@code PrintWriter} to write content to.
     * @param mojo the {@code AbstractRPMMojo} which contains any resources needed when writing the scriptlet.
     * @throws IOException
     */
    protected final void writeContent( PrintWriter writer, final AbstractRPMMojo mojo )
        throws IOException
    {
        if ( script != null )
        {
            writer.println( script );
        }
        else if ( scriptFile != null )
        {
            Reader reader = getScriptReader( scriptFile, mojo );
            if ( filter )
            {
                for ( FileUtils.FilterWrapper filterWrapper : mojo.getFilterWrappers() )
                {
                    reader = filterWrapper.getReader( reader );
                }
            }
            BufferedReader bufferedReader = null;
            try
            {
                bufferedReader = new BufferedReader( reader );
                String line;
                while ( ( line = bufferedReader.readLine() ) != null )
                {
                    writer.println( line );
                }
            }
            finally
            {
                if ( bufferedReader != null )
                {
                    try
                    {
                        bufferedReader.close();
                    }
                    catch ( IOException e )
                    {
                        // ignore - it does not matter
                    }
                }
                try
                {
                    reader.close();
                }
                catch ( IOException e )
                {
                    // ignore - it does not matter
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        StringBuilder buffer = new StringBuilder( 64 );
        buffer.append( "Scriptlet[" );
        buffer.append( "subpackage=" );
        buffer.append( subpackage );
        buffer.append( ",program=" );
        buffer.append( program );
        buffer.append( ",script=" );
        buffer.append( script );
        buffer.append( ",scriptFile=" );
        buffer.append( scriptFile );
        buffer.append( ",fileEncoding=" );
        buffer.append( fileEncoding );
        buffer.append( "]" );
        return buffer.toString();
    }
}
