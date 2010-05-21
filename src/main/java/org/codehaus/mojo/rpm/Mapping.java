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
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * A description of a file or directory to be installed. It includes the properties to be assigned and the location(s)
 * where the file(s) can be found for packaging.
 * 
 * @version $Id$
 */
public class Mapping
{

    // // // Properties

    /** Destination directory name. */
    private String directory;
    
    /**
     * Indicates the configuration value for the files.
     * <p>
     * For passivity purposes, a value of <i>true</i> or <i>false</i> will indicate whether the <code>%config</code>
     * descriptor will be written in the spec file.<br/> However, any other value (such as <i>noreplace</i>) can be
     * passed and will be written into the spec file after <code>%config</code>. In the case of <i>noreplace</i>, it
     * would look like <code>%config(noreplace)</code>.
     * </p>
     */
    private String configuration;

    /** <code>true</code> if the files are documentation. */
    private boolean documentation;

    /** File mode (octal string) to assign to files when installed. */
    private String filemode;

    /** User name for files when installed. */
    private String username;

    /** Group name for files when installed. */
    private String groupname;

    /** Mapping information for source directories. */
    private List sources;

    /** Mapping information for artifacts. */
    private ArtifactMap artifact;

    /** Mapping information for dependencies. */
    private Dependency dependency;
    
    /**
     * Indicates if the {@link #directory} should be used for the {@link #getAttrString() attribute string}.
     * 
     * @since 2.0-beta-2
     */
    private boolean directoryIncluded = true;

    /**
     * Indicates if sub-directories contained in the {@link #getSources()} should be explicitly listed in 
     * {@code %files}. This includes listing the {@link #getDestination()}, if {@link #isDirectoryIncluded()}
     * is {@code true}.
     * 
     * @since 2.1-alpha-1
     */
    private boolean recurseDirectories = false;

    /**
     * List of files actually copied for the Mapping.
     * <p>
     * This is a <tt>List</tt> of <tt>String</tt> objects which identify files relative to the
     * {@link #getDestination()}.
     * </p>
     * <p>
     * This is populated by {@link #sources}, {@link #artifact}, and {@link #dependency}.
     * </p>
     */
    private List copiedFileNamesRelativeToDestination;
    
    /**
     * List of files that will be added by soft link for the Mapping.
     * <p>
     * This is a <tt>List</tt> of <tt>String</tt> objects which identify files relative to the
     * {@link #getDestination()}.
     * </p>
     * @since 2.0-beta-3
     */
    private List linkedFileNamesRelativeToDestination;
    
    /**
     * Indicates if the {@link #sources} contain any {@link SoftlinkSource} instances.
     * @since 2.0-beta-3
     */
    private boolean hasSoftLinks = false;

    /**
     * The absolute destination in the {@link AbstractRPMMojo#getBuildroot() buildroot}. This includes evaluating
     * any/all macros.
     * 
     * @since 2.1-alpha-1
     */
    private File absoluteDestination;

    /**
     * Retrieve the destination during package installation.
     * 
     * @return The destination during package installation.
     */
    public String getDirectory()
    {
        return directory;
    }

    /**
     * Set the destination during package installation.
     * 
     * @param dir The new destination during package installation.
     */
    public void setDirectory( String dir )
    {
        directory = dir;
    }

    /**
     * Returns if the {@link #getDirectory()} should be used for the
     * {@link #getAttrString(String, String, String) attribute string} (if and only if {@link #getSources() sources}
     * make up everything that gets copied to the directory).<br/> By default, this returns <code>true</code>.
     * 
     * @return Whether the {@link #getDirectory()} should be used for the
     *         {@link #getAttrString(String, String, String) attribute string}.
     */
    public boolean isDirectoryIncluded()
    {
        return this.directoryIncluded;
    }

    /**
     * Sets if the {@link #getDirectory()} should be used for the
     * {@link #getAttrString(String, String, String) attribute string} (if and only if {@link #getSources() sources}
     * make up everything that gets copied to the directory).<br/> By default, this is <code>true</code>.
     * 
     * @param directoryIncluded The {@link #directoryIncluded} to set.
     */
    public void setDirectoryIncluded( boolean directoryIncluded )
    {
        this.directoryIncluded = directoryIncluded;
    }

    /**
     * Retrieve the configuration status. This value is <code>true</code> if the file(s) in this mapping are
     * configuration files.
     * 
     * @return The configuration status.
     * @deprecated use {@link #getConfiguration()}
     */
    public boolean isConfiguration()
    {
        return configuration == null || !"FALSE".equalsIgnoreCase( configuration );
    }
    
    /**
     * Retrieves the configuration value. This may be just a string representation of {@link #isConfiguration()}.
     * However, modifications to the <i>%config</i> declaration (such as <i>noreplace</i>) are allowed.
     * 
     * @return The configuration value.
     */
    public String getConfiguration()
    {
        return configuration;
    }
    
    /**
     * Set the configuration status. This value is <code>true</code> if the file(s) in this mapping are configuration
     * files. Any value other than <code>true</code> or <code>false</code> will be considered a modifier of the
     * <i>%config</i> declaration in the spec file.
     * 
     * @param cfg The new configuration value.
     */
    public void setConfiguration( String cfg )
    {
        configuration = cfg;
    }

    /**
     * Retrieve the documentation status. This value is <code>true</code> if the file(s) in this mapping are
     * documentation files.
     * 
     * @return The documentation status.
     */
    public boolean isDocumentation()
    {
        return documentation;
    }

    /**
     * Set the documentation status. This value is <code>true</code> if the file(s) in this mapping are documentation
     * files.
     * 
     * @param isDoc The new documentation status.
     */
    public void setDocumentation( boolean isDoc )
    {
        documentation = isDoc;
    }

    /**
     * Retrieve the UNIX file permissions. This is a three-digit octal number which specifies the permissions to be
     * applied to each file in the mapping when it is installed.
     * 
     * @return The UNIX file permissions.
     */
    public String getFilemode()
    {
        return filemode;
    }

    /**
     * Set the UNIX file permissions. This is a three-digit octal number which specifies the permissions to be applied
     * to each file in the mapping when it is installed.
     * 
     * @param fmode The new UNIX file permissions.
     */
    public void setFilemode( String fmode )
    {
        filemode = fmode;
    }

    /**
     * Retrieve the UNIX user name to own the installed files. Note that this must be a name, not a numeric user ID.
     * 
     * @return The UNIX user name to own the installed files.
     */
    public String getUsername()
    {
        return username;
    }

    /**
     * Set the UNIX user name to own the installed files. Note that this must be a name, not a numeric user ID.
     * 
     * @param uname The new UNIX user name to own the installed files.
     */
    public void setUsername( String uname )
    {
        username = uname;
    }

    /**
     * Retrieve the UNIX group name to own the installed files. Note that this must be a name, not a numeric group ID.
     * 
     * @return The UNIX group name to own the installed files.
     */
    public String getGroupname()
    {
        return groupname;
    }

    /**
     * Set the UNIX group name to own the installed files. Note that this must be a name, not a numeric group ID.
     * Indicates if sub-directories contained in the {@link #getSources()} should be explicitly listed in 
     * {@code %files}.
     * @param grpname The new UNIX group name to own the installed files.
     */
    public void setGroupname( String grpname )
    {
        groupname = grpname;
    }

    /**
     * Retrieve the list of source file specifications.
     * 
     * @return The list of source file specifications.
     */
    public List getSources()
    {
        return sources;
    }

    /**
     * Set the list of source file specifications.
     * 
     * @param srclist The new list of source file specifications.
     */
    public void setSources( List srclist )
    {
        sources = srclist;
    }

    /**
     * Retrieve the artifact specification.
     * 
     * @return The artifact specification.
     */
    public ArtifactMap getArtifact()
    {
        return artifact;
    }

    /**
     * Set the artifact specification.
     * 
     * @param am The new artifact specification.
     */
    public void setArtifact( ArtifactMap am )
    {
        artifact = am;
    }

    /**
     * Retrieve the dependency specification.
     * 
     * @return The dependency specification.
     */
    public Dependency getDependency()
    {
        return dependency;
    }

    /**
     * Set the dependency specification.
     * 
     * @param am The new dependency specification.
     */
    public void setDependency( Dependency am )
    {
        dependency = am;
    }

    /**
     * Indicates if sub-directories contained in the {@link #getSources()} should be explicitly listed in 
     * {@code %files}. This includes listing the {@link #getDestination()}, if {@link #isDirectoryIncluded()}
     * is {@code true}.
     * 
     * @return Returns the {@link #recurseDirectories}.
     * @since 2.1-alpha-1
     */
    public final boolean isRecurseDirectories()
    {
        return this.recurseDirectories;
    }

    /**
     * Indicates if sub-directories contained in the {@link #getSources()} should be explicitly listed in 
     * {@code %files}. This includes listing the {@link #getDestination()}, if {@link #isDirectoryIncluded()}
     * is {@code true}.
     * 
     * @param recurseDirectories The {@link #recurseDirectories} to set.
     * @since 2.1-alpha-1
     */
    public final void setRecurseDirectories( boolean recurseDirectories )
    {
        this.recurseDirectories = recurseDirectories;
    }

    /**
     * Assemble the RPM SPEC file attributes for a mapping.
     * 
     * @param defaultFileMode Default file mode to use if not set for this mapping. 
     * @param defaultGrp Default group to use if not set for this mapping.
     * @param defaultUsr Default user to use if not set for this mapping.
     * 
     * @return The attribute string for the SPEC file.
     */
    public String getAttrString( String defaultFileMode, String defaultGrp, String defaultUsr )
    {
        defaultFileMode = defaultFileMode == null ? "-" : defaultFileMode;
        defaultGrp = defaultGrp == null ? "-" : defaultGrp;
        defaultUsr = defaultUsr == null ? "-" : defaultUsr;
        
        StringBuffer sb = new StringBuffer();

        if ( configuration != null  && !"FALSE".equalsIgnoreCase( configuration ) )
        {
            sb.append( "%config" );
            if ( configuration.length() > 0 && !"TRUE".equalsIgnoreCase( configuration ) )
            {
                sb.append( '(' );
                sb.append( configuration );
                sb.append( ')' );
            }
            sb.append( ' ' );
        }

        if ( documentation )
        {
            sb.append( "%doc " );
        }

        if ( ( ( sources == null ) || ( sources.size() == 0 ) ) && ( dependency == null ) && ( artifact == null ) )
        {
            sb.append( "%dir " );
        }
        
        /* do not include %attr if no attributes are specified */
        if ( !( filemode == null && username == null && groupname == null ) )
        {
            sb.append( "%attr(" );

            sb.append( filemode != null ? filemode : defaultFileMode );
            sb.append( ',' );

            sb.append( username != null ? username : defaultUsr );
            sb.append( ',' );

            sb.append( groupname != null ? groupname : defaultGrp );
            sb.append( ')' );
        }
        
        return sb.toString();
    }

    /**
     * Return the destination directory name.
     * 
     * @return The name of the destination directory.
     */
    public String getDestination()
    {
        if ( directory == null )
        {
            return "nowhere";
        }
        
        return directory;
    }

    /**
     * Return directory-only status.
     * 
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

        if ( dependency != null )
        {
            return false;
        }

        return true;
    }

    /**
     * Returns the names of files copied to the {@link #getDestination() destination}.<br/>
     * This is a <tt>List</tt> of <tt>String</tt> objects which identify files relative to the 
     * <tt>destination</tt>. 
     * 
     * @return The names of files copied to the <tt>destination</tt>. The <tt>List</tt> returned will never be
     * <code>null</code>, but may be immutable.
     */
    List getCopiedFileNamesRelativeToDestination()
    {
        return this.copiedFileNamesRelativeToDestination != null ? this.copiedFileNamesRelativeToDestination
                        : Collections.EMPTY_LIST;
    }

    /**
     * Add a <tt>List</tt> of relative file names which have c
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
     * Adds a relative file name that has been copied to the {@link #getDestination() destination}.
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

    /**
     * Returns the names of files linked to in the {@link #getDestination() destination}.<br/> This is a <tt>List</tt>
     * of <tt>String</tt> objects which identify files which will be created by link relative to the
     * <tt>destination</tt>.
     * 
     * @return The names of files copied to the <tt>destination</tt>. The <tt>List</tt> returned will never be
     *         <code>null</code>, but may be immutable.
     * @since 2.0-beta-3
     */
    List getLinkedFileNamesRelativeToDestination()
    {
        return this.linkedFileNamesRelativeToDestination != null ? this.linkedFileNamesRelativeToDestination
                        : Collections.EMPTY_LIST;
    }
    
    /**
     * Adds a relative file name that will be linked to the {@link #getDestination() destination}.
     * 
     * @param linkedFileNameRelativeToDestination
     * @see #getLinkedFileNamesRelativeToDestination()
     * @since 2.0-beta-3
     */
    void addLinkedFileNameRelativeToDestination( String linkedFileNameRelativeToDestination )
    {
        if ( this.linkedFileNamesRelativeToDestination == null )
        {
            this.linkedFileNamesRelativeToDestination = new LinkedList();
        }
        
        linkedFileNamesRelativeToDestination.add( linkedFileNameRelativeToDestination );
    }

    /**
     * @return Returns the {@link #hasSoftLinks}.
     * @since 2.0-beta-3
     */
    boolean hasSoftLinks()
    {
        return this.hasSoftLinks;
    }

    /**
     * @param hasSoftLinks The {@link #hasSoftLinks} to set.
     * @since 2.0-beta-3
     */
    void setHasSoftLinks( boolean hasSoftLinks )
    {
        this.hasSoftLinks = hasSoftLinks;
    }

    /**
     * @return Returns the {@link #absoluteDestination}.
     * @since 2.1-alpha-1
     */
    final File getAbsoluteDestination()
    {
        return this.absoluteDestination;
    }

    /**
     * @param absoluteDestination The {@link #absoluteDestination} to set.
     * @since 2.1-alpha-1
     */
    final void setAbsoluteDestination( File absoluteDestination )
    {
        this.absoluteDestination = absoluteDestination;
    }

    /** {@inheritDoc} */
    public String toString()
    {
        boolean sourceShown = false;
        StringBuffer sb = new StringBuffer();

        sb.append( "[\"" + getDestination() + "\" " );
        sb.append( "{" + getAttrString( null, null, null ) + "}" );

        if ( isDirOnly() )
        {
            sb.append( " (dir only)]" );
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
                sourceShown = true;
            }
            if ( dependency != null )
            {
                if ( sourceShown )
                {
                    sb.append( ", " );
                }
                sb.append( dependency.toString() );
                sourceShown = true;
            }
            sb.append( "]" );
        }

        return sb.toString();
    }
}
