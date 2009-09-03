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

import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.util.cli.StreamConsumer;

/**
 * A {@link StreamConsumer} that writes to a {@link Log}.
 * 
 * @author Brett Okken, Cerner Corporation
 * @version $Id$
 * @since 2.0-beta-4
 */
final class LogStreamConsumer
    implements StreamConsumer
{
    /**
     * @see Log#debug(CharSequence)
     */
    public static final int DEBUG = 0;

    /**
     * @see Log#info(CharSequence)
     */
    public static final int INFO = 1;

    /**
     * @see Log#warn(CharSequence)
     */
    public static final int WARN = 2;

    /**
     * @see Log#error(CharSequence)
     */
    public static final int ERROR = 3;

    /**
     * Level to log at.
     */
    private final int level;

    /**
     * Log to use.
     */
    private final Log log;

    /**
     * Constructor takes the <i>level</i> and <i>log</i> to log all {@link #consumeLine(String) lines} to.
     * @param level The level to log at. Must be valid value (see constants).
     * @param log The log to send messages to. Must not be {@code null}.
     */
    public LogStreamConsumer( int level, Log log )
    {
        if ( level != DEBUG && level != INFO && level != WARN && level != ERROR )
        {
            throw new IllegalArgumentException( "invalid level: " + level );
        }

        if ( log == null )
        {
            throw new NullPointerException( "log is null" );
        }
        this.level = level;
        this.log = log;
    }

    /**
     * {@inheritDoc}
     */
    public void consumeLine( String line )
    {
        switch ( level )
        {
            case DEBUG:
                log.debug( line );
                break;
            case INFO:
                log.info( line );
                break;
            case WARN:
                log.warn( line );
                break;
            case ERROR:
                log.error( line );
                break;
        }
    }
}
