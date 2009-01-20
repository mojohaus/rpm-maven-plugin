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
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * A description of a location where files to be packaged can be found.
 * 
 * @version $Id$
 */
public class Source
{

    // // // Properties

    /** The source location. */
    private File location;

    /** The list of inclusions. */
    private List includes;

    /** The list of exclusions. */
    private List excludes;
    
    /**
     * List of files actually copied into rpm for the Mapping.
     * 
     * This is a <tt>List</tt> of <tt>String</tt> objects which identify files relative to 
     * the mapping destination. In the case that {@link #location} is a {@link File#isFile() file}, 
     * it will be {@link #destination} or the {@link File#getName() name}.
     */
    private List copiedFileNamesRelativeToDestination;
    
    /**
     * Optional destination name for the file identified by {@link #location}.<br/>
     * 
     * <b>NOTE:</b> This is only applicable if the {@link #location} is a {@link File#isFile() file},
     * not a {@link File#isDirectory() directory}.
     */
    private String destination;

    /** <code>true</code> to omit the default exclusions. */
    private boolean noDefaultExcludes;

    // // // Bean methods

    /**
     * Retrieve the location holding the file(s) to install.
     * 
     * @return The location holding the file(s) to install.
     */
    public File getLocation()
    {
        return location;
    }

    /**
     * Set the location holding the file(s) to install.
     * 
     * @param loc The new location holding the file(s) to install.
     */
    public void setLocation( File loc )
    {
        location = loc;
    }

    /**
     * Retrieve the list of files to include in the package.
     * 
     * @return The list of files to include in the package.
     */
    public List getIncludes()
    {
        return includes;
    }

    /**
     * Set the list of files to include in the package.
     * 
     * @param incl The new list of files to include in the package.
     */
    public void setIncludes( List incl )
    {
        includes = incl;
    }

    /**
     * Retrieve the list of files to exclude from the package.
     * 
     * @return The list of files to exclude from the package.
     */
    public List getExcludes()
    {
        return excludes;
    }

    /**
     * Set the list of files to exclude from the package.
     * 
     * @param excl The new list of files to exclude from the package.
     */
    public void setExcludes( List excl )
    {
        excludes = excl;
    }

    /**
     * Retrieve the default exclude status.
     * 
     * @return <code>true</code> if the default excludes should be omitted.
     */
    public boolean getNoDefaultExcludes()
    {
        return noDefaultExcludes;
    }

    /**
     * Set the default exclude status.
     * 
     * @param noDefExcl <code>true</code> if the default excludes should be omitted.
     */
    public void setNoDefaultExcludes( boolean noDefExcl )
    {
        noDefaultExcludes = noDefExcl;
    }

    // // // Public methods

    /**
     * @return Returns the {@link #destination}.
     * @see #setDestination(String)
     */
    public String getDestination()
    {
        return this.destination;
    }

    /**
     * Sets the destination file name.
     * <p>
     * <b>NOTE:</b> This is only applicable if the {@link #getLocation() location} is a {@link File#isFile() file},
     * not a {@link File#isDirectory() directory}.
     * </p>
     * 
     * @param destination The destination that the {@link #getLocation() location} should be in the final rpm.
     */
    public void setDestination( String destination )
    {
        this.destination = destination;
    }

    /**
     * Returns the names of files copied to the {@link Mapping} {@link Mapping#getDestination() destination}.<br/>
     * This is a <tt>List</tt> of <tt>String</tt> objects which identify files relative to the <tt>Mapping</tt>
     * destination. In the case that {@link #getLocation()} is a {@link File#isFile() file}, the only element will be
     * either the {@link #getDestination()} (if set) or the {@link File#getName() location name}.
     * 
     * @return The names of files copied to the {@link Mapping#getDestination() destination}.
     */
    List getCopiedFileNamesRelativeToDestination()
    {
        return this.copiedFileNamesRelativeToDestination;
    }

    /**
     * Add a <tt>List</tt> of relative file names which have been copied for this <tt>Source</tt>.
     * 
     * @param copiedFileNamesRelativeToDestination relative names of files to add
     * @see #getCopiedFileNamesRelativeToDestination()
     */
    void addCopiedFileNamesRelativeToDestination( List copiedFileNamesRelativeToDestination )
    {
        if ( this.copiedFileNamesRelativeToDestination == null )
        {
            this.copiedFileNamesRelativeToDestination = new ArrayList( copiedFileNamesRelativeToDestination );
        }
        else
        {
            this.copiedFileNamesRelativeToDestination.addAll( copiedFileNamesRelativeToDestination );
        }
    }

    /**
     * Adds a relative file name that has been copied for this <tt>Source</tt>.
     * 
     * @param copiedFileNameRelativeToDestination relative name of file to add to list
     * @see #getCopiedFileNamesRelativeToDestination()
     */
    void addCopiedFileNameRelativeToDestination( String copiedFileNameRelativeToDestination )
    {
        if ( this.copiedFileNamesRelativeToDestination == null )
        {
            this.copiedFileNamesRelativeToDestination = new LinkedList();
        }
        this.copiedFileNamesRelativeToDestination.add( copiedFileNameRelativeToDestination );
    }

    /** {@inheritDoc} */
    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        sb.append( "{" );

        if ( location == null )
        {
            sb.append( "nowhere" );
        }
        else
        {
            sb.append( "\"" + location + "\"" );
        }

        if ( includes != null )
        {
            sb.append( " incl:" + includes );
        }

        if ( excludes != null )
        {
            sb.append( " excl:" + excludes );
        }
        
        if ( destination != null )
        {
            sb.append( " destination: " );
            sb.append( destination );
        }

        if ( noDefaultExcludes )
        {
            sb.append( " [no default excludes]" );
        }

        sb.append( "}" );
        return sb.toString();
    }
}
