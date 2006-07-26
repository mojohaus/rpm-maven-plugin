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

import java.util.List;

/**
 * A description of a file or directory to be installed.  It includes the
 * properties to be assigned and the location(s) where the file(s) can be
 * found for packaging.
 * @version $Id$
 */
public class Mapping
{
    
    // // //  Properties
    
    private String directory;
    private boolean configuration;
    private boolean documentation;
    private String filemode;
    private String username;
    private String groupname;
    private List sources;
    private ArtifactMap artifact;
    
    // // //  Bean methods
    
    /**
     * Retrieve the destination during package installation.
     * @return The destination during package installation.
     */
    public String getDirectory()
    {
        return this.directory;
    }
    
    /**
     * Set the destination during package installation.
     * @param directory The new destination during package installation.
     */
    public void setDirectory(String directory)
    {
        this.directory = directory;
    }
    
    /**
     * Retrieve the configuration status.  This value is <code>true</code> if
     * the file(s) in this mapping are configuration files.
     * @return The configuration status.
     */
    public boolean isConfiguration()
    {
        return this.configuration;
    }
    
    /**
     * Set the configuration status.  This value is <code>true</code> if
     * the file(s) in this mapping are configuration files.
     * @param configuration The new configuration status.
     */
    public void setConfiguration(boolean configuration)
    {
        this.configuration = configuration;
    }
    
    /**
     * Retrieve the documentation status.  This value is <code>true</code> if
     * the file(s) in this mapping are documentation files.
     * @return The documentation status.
     */
    public boolean isDocumentation()
    {
        return this.documentation;
    }
    
    /**
     * Set the documentation status.  This value is <code>true</code> if
     * the file(s) in this mapping are documentation files.
     * @param documentation The new documentation status.
     */
    public void setDocumentation(boolean documentation)
    {
        this.documentation = documentation;
    }
    
    /**
     * Retrieve the UNIX file permissions.  This is a three-digit octal number
     * which specifies the permissions to be applied to each file in the
     * mapping when it is installed.
     * @return The UNIX file permissions.
     */
    public String getFilemode()
    {
        return this.filemode;
    }
    
    /**
     * Set the UNIX file permissions.  This is a three-digit octal number
     * which specifies the permissions to be applied to each file in the
     * mapping when it is installed.
     * @param filemode The new UNIX file permissions.
     */
    public void setFilemode(String filemode)
    {
        this.filemode = filemode;
    }
    
    /**
     * Retrieve the UNIX user name to own the installed files.  Note that this
     * must be a name, not a numeric user ID.
     * @return The UNIX user name to own the installed files.
     */
    public String getUsername()
    {
        return this.username;
    }
    
    /**
     * Set the UNIX user name to own the installed files.  Note that this
     * must be a name, not a numeric user ID.
     * @param username The new UNIX user name to own the installed files.
     */
    public void setUsername(String username)
    {
        this.username = username;
    }
    
    /**
     * Retrieve the UNIX group name to own the installed files.  Note that this
     * must be a name, not a numeric group ID.
     * @return The UNIX group name to own the installed files.
     */
    public String getGroupname()
    {
        return this.groupname;
    }
    
    /**
     * Set the UNIX group name to own the installed files.  Note that this
     * must be a name, not a numeric group ID.
     * @param groupname The new UNIX group name to own the installed files.
     */
    public void setGroupname(String groupname)
    {
        this.groupname = groupname;
    }
    
    /**
     * Retrieve the list of source file specifications.
     * @return The list of source file specifications.
     */
    public List getSources()
    {
        return this.sources;
    }
    
    /**
     * Set the list of source file specifications.
     * @param sources The new list of source file specifications.
     */
    public void setSources(List sources)
    {
        this.sources = sources;
    }
    
    /**
     * Retrieve the artifact specification.
     * @return The artifact specification.
     */
    public ArtifactMap getArtifact()
    {
        return artifact;
    }
    
    /**
     * Set the artifact specification.
     * @param am The new artifact specification.
     */
    public void setArtifact( ArtifactMap am )
    {
        artifact = am;
    }
    
    // // //  Public methods
    
    public String getAttrString()
    {
        StringBuffer sb = new StringBuffer();
        
        if (configuration)
        {
            sb.append("%config ");
        }
        
        if (documentation)
        {
            sb.append("%doc ");
        }
        
        if ((sources == null) || (sources.size() == 0))
        {
            sb.append("%dir ");
        }
        
        if (filemode != null)
        {
            sb.append("%attr(" + filemode + ",");
        }
        else
        {
            sb.append("%attr(-,");
        }
        
        if (username != null)
        {
            sb.append(username + ",");
        }
        else
        {
            sb.append("-,");
        }
        
        if (groupname != null)
        {
            sb.append(groupname + ")");
        }
        else
        {
            sb.append("-)");
        }
        
        return sb.toString();
    }
    
    public String getDestination()
    {
        if (directory == null)
        {
            return "nowhere";
        }
        else
        {
            return directory;
        }
    }
    
    /**
     * Return directory-only status.
     * @return <code>true</code> if no sources were specified in the mapping
     */
    public boolean isDirOnly()
    {
        if ( ( sources != null ) && ( !sources.isEmpty() ) )
        {
            return false;
        }
        
        if ( artifact != null )
        {
            return false;
        }
        
        return true;
    }
    
    public String toString()
    {
        boolean sourceShown = false;
        StringBuffer sb = new StringBuffer();
        
        sb.append("[\"" + getDestination() + "\" ");
        sb.append("{" + getAttrString() + "}");
        
        if (sources == null)
        {
            sb.append(" (dir only)]");
        }
        else
        {
            sb.append( " from " );
            if ( sources != null )
            {
                sb.append( sources.toString() );
                sourceShown = true;
            }
            if ( artifact != null )
            {
                if ( sourceShown )
                {
                    sb.append( ", " );
                }
                sb.append( artifact.toString() );
            }
            sb.append( "]" );
        }
        
        return sb.toString();
    }
}
