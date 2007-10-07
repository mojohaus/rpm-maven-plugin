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
import java.util.List;

/**
 * A description of a location where files to be packaged can be found.
 * @version $Id$
 */
public class Source
{
    
    // // //  Properties
    
    /** The source location. */
    private File location;
    
    /** The list of inclusions. */
    private List includes;
    
    /** The list of exclusions. */
    private List excludes;
    
    /** <code>true</code> to omit the default exclusions. */
    private boolean noDefaultExcludes;
    
    // // //  Bean methods
    
    /**
     * Retrieve the location holding the file(s) to install.
     * @return The location holding the file(s) to install.
     */
    public File getLocation()
    {
        return location;
    }
    
    /**
     * Set the location holding the file(s) to install.
     * @param loc The new location holding the file(s) to install.
     */
    public void setLocation( File loc )
    {
        location = loc;
    }
    
    /**
     * Retrieve the list of files to include in the package.
     * @return The list of files to include in the package.
     */
    public List getIncludes()
    {
        return includes;
    }
    
    /**
     * Set the list of files to include in the package.
     * @param incl The new list of files to include in the package.
     */
    public void setIncludes( List incl )
    {
        includes = incl;
    }
    
    /**
     * Retrieve the list of files to exclude from the package.
     * @return The list of files to exclude from the package.
     */
    public List getExcludes()
    {
        return excludes;
    }
    
    /**
     * Set the list of files to exclude from the package.
     * @param excl The new list of files to exclude from the package.
     */
    public void setExcludes( List excl )
    {
        excludes = excl;
    }
    
    /**
     * Retrieve the default exclude status.
     * @return <code>true</code> if the default excludes should be omitted.
     */
    public boolean getNoDefaultExcludes()
    {
        return noDefaultExcludes;
    }
    
    /**
     * Set the default exclude status.
     * @param noDefExcl <code>true</code> if the default excludes
     *        should be omitted.
     */
    public void setNoDefaultExcludes( boolean noDefExcl )
    {
        noDefaultExcludes = noDefExcl;
    }
    
    // // //  Public methods
    
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
        
        if ( noDefaultExcludes )
        {
            sb.append( " [no default excludes]" );
        }
        
        sb.append( "}" );
        return sb.toString();
    }
}
