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


import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

/**
 * A base class to support <a href="http://rpm.org/api/4.4.2.2/triggers.html">triggers</a>.
 *
 * @author Brett Okken, Cerner Corporation
 * @since 2.0-beta-4
 */
public abstract class BaseTrigger
    extends Scriptlet
{
    /**
     * List of triggers.
     */
    private List<String> triggers;

    /**
     * Gets the packages/versions to trigger on.
     * <p>
     * This is syntactically equivalent to a "Requires" specification (version numbers may be used). If multiple items
     * are given, the trigger is run when *any* of those conditions becomes true.
     * </p>
     *
     * @return Returns the {@link #triggers}.
     */
    public List<String> getTriggers()
    {
        return this.triggers;
    }

    /**
     * Sets the packages/versions to trigger on.
     *
     * @param triggers The {@link #triggers} to set.
     * @see #getTriggers()
     */
    public void setTriggers( List<String> triggers )
    {
        this.triggers = triggers;
    }

    /**
     * {@inheritDoc}
     */
    protected String buildScriptletLine( String directive )
    {
        final StringBuilder builder = new StringBuilder( super.buildScriptletLine( directive ) );

        builder.append( " -- " );

        final int size = triggers.size();
        for ( int i = 0; i < size; ++i )
        {
            final String trigger = triggers.get( i );

            if ( i != 0 )
            {
                builder.append( ", " );
            }

            builder.append( trigger );
        }

        return builder.toString();
    }

    /**
     * Writes the complete trigger directive.
     *
     * @param writer {@code PrintWriter} to write the trigger directive to.
     * @param mojo the {@code AbstractRPMMojo} which contains any resources needed when writing the trigger.
     * @throws IOException
     */
    protected void writeTrigger( PrintWriter writer, final AbstractRPMMojo mojo )
        throws IOException
    {
        write( writer, getDirective(), mojo );
    }

    /**
     * Provides the trigger specific directive.
     *
     * @return The implementation specific directive.
     */
    protected abstract String getDirective();

    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        StringBuilder buffer = new StringBuilder( 128 );
        buffer.append( getClass().getName().substring( getClass().getName().lastIndexOf( '.' ) ) );
        buffer.append( '[' );
        buffer.append( "subpackage=" );
        buffer.append( getSubpackage() );
        buffer.append( ",program=" );
        buffer.append( getProgram() );
        buffer.append( ",script=" );
        buffer.append( getScript() );
        buffer.append( ",scriptFile=" );
        buffer.append( getScriptFile() );
        buffer.append( ",fileEncoding=" );
        buffer.append( getFileEncoding() );
        buffer.append( ",triggers=" );
        buffer.append( triggers );
        buffer.append( "]" );
        return buffer.toString();
    }
}