package org.codehaus.mojo.rpm;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * A description of a location where files to be packaged can be found.
 * @version $Id: Source.java,v 1.1 2005/11/08 20:11:06 allisord Exp $
 */
public class Source
{
    
    // // //  Properties
    
    private File location;
    private List includes;
    private List excludes;
    
    // // //  Bean methods
    
    /**
     * Retrieve the location holding the file(s) to install.
     * @return The location holding the file(s) to install.
     */
    public File getLocation()
    {
        return this.location;
    }
    
    /**
     * Set the location holding the file(s) to install.
     * @param location The new location holding the file(s) to install.
     */
    public void setLocation(File location)
    {
        this.location = location;
    }
    
    /**
     * Retrieve the list of files to include in the package.
     * @return The list of files to include in the package.
     */
    public List getIncludes()
    {
        return this.includes;
    }
    
    /**
     * Set the list of files to include in the package.
     * @param includes The new list of files to include in the package.
     */
    public void setIncludes(List includes)
    {
        this.includes = includes;
    }
    
    /**
     * Retrieve the list of files to exclude from the package.
     * @return The list of files to exclude from the package.
     */
    public List getExcludes()
    {
        return this.excludes;
    }
    
    /**
     * Set the list of files to exclude from the package.
     * @param excludes The new list of files to exclude from the package.
     */
    public void setExcludes(List excludes)
    {
        this.excludes = excludes;
    }
    
    // // //  Public methods
    
    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        
        if ((includes != null) || (excludes != null))
        {
            sb.append("{");
        }
        
        if (location == null)
        {
            sb.append("nowhere");
        }
        else
        {
            sb.append("\"" + location + "\"");
        }
        
        if (includes != null)
        {
            sb.append(" incl:" + includes);
        }
        
        if (excludes != null)
        {
            sb.append(" excl:" + excludes);
        }
        
        if ((includes != null) || (excludes != null))
        {
            sb.append("}");
        }
        
        return sb.toString();
    }
}