/**
 * $Id$
 * 
 * Created: Jan 22, 2009
 */
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.versioning.OverConstrainedVersionException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.dir.DirectoryArchiver;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.StreamConsumer;

/**
 * Abstract base class for building RPMs.
 * 
 * @author Carlos
 * @author Brett Okken, Cerner Corp.
 * @version $Revision$
 */
abstract class AbstractRPMMojo
    extends AbstractMojo
{

    /**
     * Message for exception indicating that a {@link Source} has a {@link Source#getDestination() destination}, but
     * refers to a {@link File#isDirectory() directory}.
     */
    private static final String DESTINATION_DIRECTORY_ERROR_MSG =
        "Source has a destination [{0}], but the location [{1}] does not refer to a file.";

    /**
     * The name portion of the output file name.
     * 
     * @parameter expression="${project.artifactId}"
     * @required
     */
    protected String name;

    /**
     * The version portion of the RPM file name.
     * 
     * @parameter alias="version" expression="${project.version}"
     * @required
     */
    protected String projversion;

    /**
     * The release portion of the RPM file name.
     * 
     * @parameter
     * @required
     */
    protected String release;

    /**
     * Set to <code>true</code> if the package is dependent on the architecture of the build machine.
     * 
     * @parameter
     */
    protected boolean needarch;

    /**
     * Set to a key name to sign the package using GPG. Note that due to RPM limitations, this always requires input
     * from the terminal even if the key has no passphrase.
     * 
     * @parameter expression="${gpg.keyname}"
     */
    protected String keyname;

    /**
     * The long description of the package.
     * 
     * @parameter expression="${project.description}"
     */
    protected String description;

    /**
     * The one-line description of the package.
     * 
     * @parameter expression="${project.name}"
     */
    protected String summary;

    /**
     * The one-line copyright information.
     * 
     * @parameter
     */
    protected String copyright;

    /**
     * The distribution containing this package.
     * 
     * @parameter
     */
    protected String distribution;

    /**
     * An icon for the package.
     * 
     * @parameter
     */
    protected File icon;

    /**
     * The vendor supplying the package.
     * 
     * @parameter expression="${project.organization.name}"
     */
    protected String vendor;

    /**
     * A URL for the vendor.
     * 
     * @parameter expression="${project.organization.url}"
     */
    protected String url;

    /**
     * The package group for the package.
     * 
     * @parameter
     * @required
     */
    protected String group;

    /**
     * The name of the person or group creating the package.
     * 
     * @parameter expression="${project.organization.name}"
     */
    protected String packager;

    /**
     * The list of virtual packages provided by this package.
     * 
     * @parameter
     */
    protected List provides;

    /**
     * The list of requirements for this package.
     * 
     * @parameter
     */
    protected List requires;

    /**
     * The list of conflicts for this package.
     * 
     * @parameter
     */
    protected List conflicts;

    /**
     * The relocation prefix for this package.
     * 
     * @parameter
     */
    protected String prefix;

    /**
     * The area for RPM to use for building the package.
     * 
     * @parameter expression="${project.build.directory}/rpm"
     */
    protected File workarea;

    /**
     * The list of file <a href="map-params.html">mappings</a>.
     * 
     * @parameter
     * @required
     */
    protected List mappings;

    /**
     * The pre-installation script.
     * 
     * @parameter
     */
    protected String preinstall;

    /**
     * The location of the pre-installation script.
     * 
     * @parameter
     */
    protected File preinstallScript;

    /**
     * The post-installation script.
     * 
     * @parameter
     */
    protected String postinstall;

    /**
     * The location of the post-installation script.
     * 
     * @parameter
     */
    protected File postinstallScript;

    /**
     * The installation script.
     * 
     * @parameter
     */
    protected String install;

    /**
     * The location of the installation script.
     * 
     * @parameter
     */
    protected File installScript;

    /**
     * The pre-removal script.
     * 
     * @parameter
     */
    protected String preremove;

    /**
     * The location of the pre-removal script.
     * 
     * @parameter
     */
    protected File preremoveScript;

    /**
     * The post-removal script.
     * 
     * @parameter
     */
    protected String postremove;

    /**
     * The location of the post-removal script.
     * 
     * @parameter
     */
    protected File postremoveScript;

    /**
     * The verification script.
     * 
     * @parameter
     */
    protected String verify;

    /**
     * The location of the verification script.
     * 
     * @parameter
     */
    protected File verifyScript;

    /**
     * The clean script.
     * 
     * @parameter
     */
    protected String clean;

    /**
     * The location of the clean script.
     * 
     * @parameter
     */
    protected File cleanScript;

    /**
     * A Plexus component to copy files and directories.
     * 
     * @component role="org.codehaus.plexus.archiver.Archiver" roleHint="dir"
     */
    protected DirectoryArchiver copier;

    /**
     * The primary project artifact.
     * 
     * @parameter expression="${project.artifact}"
     * @required
     * @readonly
     */
    protected Artifact artifact;

    /**
     * Auxillary project artifacts.
     * 
     * @parameter expression="${project.attachedArtifacts}
     * @required
     * @readonly
     */
    protected List attachedArtifacts;

    /**
     * @parameter default-value="${project}"
     * @required
     * @readonly
     */
    protected MavenProject project;

    /**
     * A list of %define arguments
     * 
     * @parameter
     */
    protected List defineStatements;

    /** The root of the build area. */
    protected File buildroot;

    /** The version string after parsing. */
    protected String version;

    // // // Consumers for rpmbuild output

    /**
     * Consumer to receive lines sent to stdout. The lines are logged as info.
     */
    private class StdoutConsumer
        implements StreamConsumer
    {
        /** Logger to receive the lines. */
        private Log logger;

        /**
         * Constructor.
         * 
         * @param log The logger to receive the lines
         */
        public StdoutConsumer( Log log )
        {
            logger = log;
        }

        /**
         * Consume a line.
         * 
         * @param string The line to consume
         */
        public void consumeLine( String string )
        {
            logger.info( string );
        }
    }

    /**
     * Consumer to receive lines sent to stderr. The lines are logged as warnings.
     */
    private class StderrConsumer
        implements StreamConsumer
    {
        /** Logger to receive the lines. */
        private Log logger;

        /**
         * Constructor.
         * 
         * @param log The logger to receive the lines
         */
        public StderrConsumer( Log log )
        {
            logger = log;
        }

        /**
         * Consume a line.
         * 
         * @param string The line to consume
         */
        public void consumeLine( String string )
        {
            logger.warn( string );
        }
    }

    // // // Mojo methods

    /** {@inheritDoc} */
    public final void execute()
        throws MojoExecutionException, MojoFailureException
    {
        checkParams();
        buildWorkArea();
        installFiles();
        writeSpecFile();
        buildPackage();
        
        afterExecution();
    }
    
    /**
     * Will be called on completion of {@link #execute()}. Provides subclasses an opportunity to
     * perform
     * @throws MojoExecutionException
     * @throws MojoFailureException
     */
    protected void afterExecution() throws MojoExecutionException, MojoFailureException
    {
        
    }
    
    protected File getRPMFile()
    {
        File rpms = new File( workarea, "RPMS" );
        File archDir = needarch ? new File( rpms, System.getProperty( "os.arch" ) ) : new File( rpms, "noarch" );
        String arch = archDir.getName();
        
        return new File( archDir, name + '-' + version + '-' + release + '.' + arch + ".rpm" );
    }

    // // // Internal methods

    /**
     * Run the external command to build the package.
     * 
     * @throws MojoExecutionException if an error occurs
     */
    private void buildPackage()
        throws MojoExecutionException
    {
        File f = new File( workarea, "SPECS" );

        Commandline cl = new Commandline();
        cl.setExecutable( "rpmbuild" );
        cl.setWorkingDirectory( f.getAbsolutePath() );
        cl.createArgument().setValue( "-bb" );
        cl.createArgument().setValue( "--buildroot" );
        cl.createArgument().setValue( buildroot.getAbsolutePath() );
        cl.createArgument().setValue( "--define" );
        cl.createArgument().setValue( "_topdir " + workarea.getAbsolutePath() );
        if ( !needarch )
        {
            cl.createArgument().setValue( "--target" );
            cl.createArgument().setValue( "noarch" );
        }
        if ( keyname != null )
        {
            cl.createArgument().setValue( "--define" );
            cl.createArgument().setValue( "_gpg_name " + keyname );
            cl.createArgument().setValue( "--sign" );
        }
        cl.createArgument().setValue( name + ".spec" );

        StreamConsumer stdout = new StdoutConsumer( getLog() );
        StreamConsumer stderr = new StderrConsumer( getLog() );
        try
        {
            if ( getLog().isDebugEnabled() )
            {
                getLog().debug( "About to execute \'" + cl.toString() + "\'" );
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
    }

    /**
     * Build the structure of the work area.
     * 
     * @throws MojoFailureException if a directory cannot be built
     */
    private void buildWorkArea()
        throws MojoFailureException
    {
        final String[] topdirs = { "BUILD", "RPMS", "SOURCES", "SPECS", "SRPMS" };

        // Build the top directory
        if ( !workarea.exists() )
        {
            getLog().info( "Creating directory " + workarea.getAbsolutePath() );
            if ( !workarea.mkdirs() )
            {
                throw new MojoFailureException( "Unable to create directory " + workarea.getAbsolutePath() );
            }
        }

        // Build each directory in the top directory
        for ( int i = 0; i < topdirs.length; i++ )
        {
            File d = new File( workarea, topdirs[i] );
            if ( !d.exists() )
            {
                getLog().info( "Creating directory " + d.getAbsolutePath() );
                if ( !d.mkdir() )
                {
                    throw new MojoFailureException( "Unable to create directory " + d.getAbsolutePath() );
                }
            }
        }

        // Build the build root
        buildroot = new File( workarea, "buildroot" );
        if ( !buildroot.exists() )
        {
            getLog().info( "Creating directory " + buildroot.getAbsolutePath() );
            if ( !buildroot.mkdir() )
            {
                throw new MojoFailureException( "Unable to create directory " + buildroot.getAbsolutePath() );
            }
        }
    }

    /**
     * Check the parameters for validity.
     * 
     * @throws MojoFailureException if an invalid parameter is found
     * @throws MojoExecutionException if an error occurs reading a script
     */
    private void checkParams()
        throws MojoExecutionException, MojoFailureException
    {
        // Check the version string
        if ( projversion.indexOf( "-" ) == -1 )
        {
            version = projversion;
        }
        else
        {
            version = projversion.substring( 0, projversion.indexOf( "-" ) );
            getLog().warn( "Version string truncated to " + version );
        }

        // Various checks in the mappings
        for ( Iterator it = mappings.iterator(); it.hasNext(); )
        {
            Mapping map = (Mapping) it.next();
            if ( map.getDirectory() == null )
            {
                throw new MojoFailureException( "<mapping> element must contain the destination directory" );
            }
            if ( map.getSources() != null )
            {
                for ( Iterator sit = map.getSources().iterator(); sit.hasNext(); )
                {
                    Source src = (Source) sit.next();
                    if ( src.getLocation() == null )
                    {
                        throw new MojoFailureException( "<mapping><source> tag must contain the source directory" );
                    }
                }
            }
        }

        // Collect the scripts, if necessary
        if ( ( preinstall == null ) && ( preinstallScript != null ) )
        {
            preinstall = readFile( preinstallScript );
        }
        if ( ( install == null ) && ( installScript != null ) )
        {
            install = readFile( installScript );
        }
        if ( ( postinstall == null ) && ( postinstallScript != null ) )
        {
            postinstall = readFile( postinstallScript );
        }
        if ( ( preremove == null ) && ( preremoveScript != null ) )
        {
            preremove = readFile( preremoveScript );
        }
        if ( ( postremove == null ) && ( postremoveScript != null ) )
        {
            postremove = readFile( postremoveScript );
        }
        if ( ( verify == null ) && ( verifyScript != null ) )
        {
            verify = readFile( verifyScript );
        }
        if ( ( clean == null ) && ( cleanScript != null ) )
        {
            clean = readFile( cleanScript );
        }
    }

    /**
     * Copy an artifact.
     * 
     * @param art The artifact to copy
     * @param dest The destination directory
     * @throws MojoExecutionException if a problem occurs
     */
    private void copyArtifact( Artifact art, File dest )
        throws MojoExecutionException
    {
        if ( art.getFile() == null )
        {
            getLog().warn( "Artifact " + art + " requested in configuration." );
            getLog().warn( "Plugin must run in standard lifecycle for this to work." );
            return;
        }
        copySource( art.getFile(), dest, null, null );
    }

    /**
     * Copy a set of files.
     * 
     * @param src The file or directory to start copying from
     * @param dest The destination directory
     * @param incl The list of inclusions
     * @param excl The list of exclusions
     * @return List of file names, relative to <i>dest</i>, copied to <i>dest</i>.
     * @throws MojoExecutionException if a problem occurs
     */
    private List copySource( File src, File dest, List incl, List excl )
        throws MojoExecutionException
    {
        try
        {
            // Set the destination
            copier.setDestFile( dest );

            // Set the source
            if ( src.isDirectory() )
            {
                String[] ia = null;
                if ( incl != null )
                {
                    ia = (String[]) incl.toArray( new String[0] );
                }

                String[] ea = null;
                if ( excl != null )
                {
                    ea = (String[]) excl.toArray( new String[0] );
                }

                copier.addDirectory( src, "", ia, ea );
            }
            else
            {
                copier.addFile( src, src.getName() );
            }

            // Perform the copy
            copier.createArchive();

            Map copiedFilesMap = copier.getFiles();
            List copiedFiles = new ArrayList( copiedFilesMap.size() );
            for ( Iterator i = copiedFilesMap.keySet().iterator(); i.hasNext(); )
            {
                String key = (String) i.next();
                if ( key != null && key.length() > 0 )
                {
                    copiedFiles.add( key );
                }
            }

            // Clear the list for the next mapping
            copier.resetArchiver();

            return copiedFiles;
        }
        catch ( Throwable t )
        {
            throw new MojoExecutionException( "Unable to copy files for packaging: " + t.getMessage(), t );
        }
    }

    /**
     * Determine if the dependency matches an include or exclude list.
     * 
     * @param dep The dependency to check
     * @param list The list to check against
     * @return <code>true</code> if the dependency was found on the list
     */
    private boolean depMatcher( Artifact dep, List list )
    {
        if ( list == null )
        {
            // No list, not possible to match
            return false;
        }

        for ( Iterator it = list.iterator(); it.hasNext(); )
        {
            Artifact item = (Artifact) it.next();
            getLog().debug( "Compare " + dep + " to " + item );
            if ( StringUtils.isEmpty( item.getGroupId() ) || item.getGroupId().equals( dep.getGroupId() ) )
            {
                getLog().debug( "... Group matches" );
                if ( StringUtils.isEmpty( item.getArtifactId() ) || item.getArtifactId().equals( dep.getArtifactId() ) )
                {
                    getLog().debug( "... Artifact matches" );
                    // ArtifactVersion av = item.getVersionRange().matchVersion( dep.getAvailableVersions() );
                    try
                    {
                        if ( item.getVersionRange().containsVersion( dep.getSelectedVersion() ) )
                        {
                            getLog().debug( "... Version matches" );
                            return true;
                        }
                    }
                    catch ( OverConstrainedVersionException ocve )
                    {
                        getLog().debug( "... caught OverConstrainedVersionException" );
                    }
                }
            }
        }

        // Not found
        return false;
    }

    /**
     * Copy the files from the various mapping sources into the build root.
     * 
     * @throws MojoExecutionException if a problem occurs
     */
    private void installFiles()
        throws MojoExecutionException
    {
        // Copy icon, if specified
        if ( icon != null )
        {
            File icondest = new File( workarea, "SOURCES" );
            copySource( icon, icondest, null, null );
        }

        // Process each mapping
        for ( Iterator it = mappings.iterator(); it.hasNext(); )
        {
            Mapping map = (Mapping) it.next();
            File dest = new File( buildroot + map.getDestination() );

            if ( map.isDirOnly() )
            {
                // Build the output directory if it doesn't exist
                if ( !dest.exists() )
                {
                    getLog().info( "Creating empty directory " + dest.getAbsolutePath() );
                    if ( !dest.mkdirs() )
                    {
                        throw new MojoExecutionException( "Unable to create " + dest.getAbsolutePath() );
                    }
                }
            }
            else
            {
                List srcs = map.getSources();
                if ( srcs != null )
                {
                    // it is important that for each Source we set the files that are "installed".
                    for ( Iterator sit = srcs.iterator(); sit.hasNext(); )
                    {
                        Source src = (Source) sit.next();
                        File location = src.getLocation();
                        if ( location.exists() )
                        {
                            String destination = src.getDestination();
                            if ( destination == null )
                            {
                                List elist = src.getExcludes();
                                if ( !src.getNoDefaultExcludes() )
                                {
                                    if ( elist == null )
                                    {
                                        elist = new ArrayList();
                                    }
                                    elist.addAll( FileUtils.getDefaultExcludesAsList() );
                                }
                                List copiedFiles = copySource( src.getLocation(), dest, src.getIncludes(), elist );
                                src.addCopiedFileNamesRelativeToDestination( copiedFiles );
                            }
                            else
                            {
                                if ( !location.isFile() )
                                {
                                    throw new MojoExecutionException(
                                                                      MessageFormat.format(
                                                                                            DESTINATION_DIRECTORY_ERROR_MSG,
                                                                                            new Object[] { destination,
                                                                                                location.getName() } ) );
                                }

                                File destFile = new File( dest, destination );

                                try
                                {
                                    if ( !destFile.createNewFile() )
                                    {
                                        throw new IOException( "Unable to create file: " + destFile.getAbsolutePath() );
                                    }

                                    FileInputStream fis = new FileInputStream( location );
                                    try
                                    {
                                        FileOutputStream fos = new FileOutputStream( destFile );
                                        try
                                        {
                                            fis.getChannel().transferTo( 0, location.length(), fos.getChannel() );
                                        }
                                        finally
                                        {
                                            fos.close();
                                        }
                                    }
                                    finally
                                    {
                                        fis.close();
                                    }
                                }
                                catch ( IOException e )
                                {
                                    throw new MojoExecutionException( "Unable to copy files", e );
                                }

                                src.addCopiedFileNameRelativeToDestination( destination );
                            }
                        }
                        else
                        {
                            throw new MojoExecutionException( "Source location " + location + " does not exist" );
                        }
                    }
                }

                ArtifactMap art = map.getArtifact();
                if ( art != null )
                {
                    List artlist = selectArtifacts( art );
                    for ( Iterator ait = artlist.iterator(); ait.hasNext(); )
                    {
                        copyArtifact( (Artifact) ait.next(), dest );
                    }
                }

                Dependency dep = map.getDependency();
                if ( dep != null )
                {
                    List deplist = selectDependencies( dep );
                    for ( Iterator dit = deplist.iterator(); dit.hasNext(); )
                    {
                        copyArtifact( (Artifact) dit.next(), dest );
                    }
                }
            }
        }
    }

    /**
     * Read a file into a string.
     * 
     * @param in The file to read
     * @return The file contents
     * @throws MojoExecutionException if an error occurs reading the file
     */
    private String readFile( File in )
        throws MojoExecutionException
    {
        try
        {
            StringBuffer sb = new StringBuffer();
            BufferedReader br = new BufferedReader( new FileReader( in ) );
            while ( br.ready() )
            {
                String line = br.readLine();
                sb.append( line + "\n" );
            }
            br.close();
            return sb.toString();
        }
        catch ( Throwable t )
        {
            throw new MojoExecutionException( "Unable to read " + in.getAbsolutePath(), t );
        }
    }

    /**
     * Make a list of the artifacts to package in this mapping.
     * 
     * @param am The artifact mapping information
     * @return The list of artifacts to package
     */
    private List selectArtifacts( ArtifactMap am )
    {
        List retval = new ArrayList();
        List clist = am.getClassifiers();

        if ( clist == null )
        {
            retval.add( artifact );
            retval.addAll( attachedArtifacts );
        }
        else
        {
            if ( clist.contains( null ) )
            {
                retval.add( artifact );
            }
            for ( Iterator ait = attachedArtifacts.iterator(); ait.hasNext(); )
            {
                Artifact aa = (Artifact) ait.next();
                if ( ( aa.hasClassifier() ) && ( clist.contains( aa.getClassifier() ) ) )
                {
                    retval.add( aa );
                }
            }
        }

        return retval;
    }

    /**
     * Make a list of the dependencies to package in this mapping.
     * 
     * @param d The artifact mapping information
     * @return The list of artifacts to package
     */
    private List selectDependencies( Dependency d )
    {
        List retval = new ArrayList();
        List inc = d.getIncludes();
        List exc = d.getExcludes();

        Collection deps = project.getArtifacts();
        if ( deps == null || deps.isEmpty() )
        {
            return retval;
        }

        for ( Iterator it = deps.iterator(); it.hasNext(); )
        {
            Artifact pdep = (Artifact) it.next();
            getLog().debug( "Dependency is " + pdep + " at " + pdep.getFile() );
            if ( !depMatcher( pdep, exc ) )
            {
                getLog().debug( "--> not excluded" );
                if ( ( inc == null ) || ( depMatcher( pdep, inc ) ) )
                {
                    getLog().debug( "--> included" );
                    retval.add( pdep );
                }
            }
        }

        return retval;
    }

    /**
     * Write the SPEC file.
     * 
     * @throws MojoExecutionException if an error occurs writing the file
     */
    private void writeSpecFile()
        throws MojoExecutionException
    {
        File f = new File( workarea, "SPECS" );
        File specf = new File( f, name + ".spec" );
        try
        {
            getLog().info( "Creating spec file " + specf.getAbsolutePath() );
            PrintWriter spec = new PrintWriter( new FileWriter( specf ) );

            writeList( spec, defineStatements, "%define " );

            spec.println( "Name: " + name );
            spec.println( "Version: " + version );
            spec.println( "Release: " + release );
            if ( summary != null )
            {
                spec.println( "Summary: " + summary );
            }

            /* copyright composition */
            String copyrightText = copyright;
            if ( copyrightText == null )
            {
                copyrightText = generateCopyrightText();
            }
            if ( copyrightText != null )
            {
                spec.println( "License: " + copyrightText );
            }
            if ( distribution != null )
            {
                spec.println( "Distribution: " + distribution );
            }
            if ( icon != null )
            {
                spec.println( "Icon: " + icon.getName() );
            }
            if ( vendor != null )
            {
                spec.println( "Vendor: " + vendor );
            }
            if ( url != null )
            {
                spec.println( "URL: " + url );
            }
            if ( group != null )
            {
                spec.println( "Group: " + group );
            }
            if ( packager != null )
            {
                spec.println( "Packager: " + packager );
            }
            writeList( spec, provides, "Provides: " );
            writeList( spec, requires, "Requires: " );
            writeList( spec, conflicts, "Conflicts: " );
            if ( prefix != null )
            {
                spec.println( "Prefix: " + prefix );
            }
            spec.println( "BuildRoot: " + buildroot.getAbsolutePath() );
            spec.println();
            spec.println( "%description" );
            if ( description != null )
            {
                spec.println( description );
            }

            spec.println();
            spec.println( "%files" );
            for ( Iterator it = mappings.iterator(); it.hasNext(); )
            {
                Mapping map = (Mapping) it.next();

                // For each mapping we need to determine which files in the destination were defined by this
                // mapping so that we can write the %attr statement correctly.

                List includes = determineFileIncludes( map );

                DirectoryScanner scanner = new DirectoryScanner();
                final String destination = map.getDestination();
                scanner.setBasedir( buildroot.getAbsolutePath() + destination );
                scanner.setIncludes( includes.isEmpty() ? null
                                : (String[]) includes.toArray( new String[includes.size()] ) );
                scanner.setExcludes( null );
                scanner.scan();

                final String attrString = map.getAttrString();
                if ( scanner.isEverythingIncluded() && map.isDirectoryIncluded() )
                {
                    getLog().debug( "writing attriute string for directory: " + destination );
                    spec.println( attrString + " " + destination );
                }
                else
                {
                    getLog().debug( "writing attribute string for identified files in directory: " + destination );

                    String[] files = scanner.getIncludedFiles();

                    String baseFileString = attrString + " " + destination + File.separatorChar;

                    for ( int i = 0; i < files.length; ++i )
                    {
                        spec.println( baseFileString + files[i] );
                    }
                }
            }

            printScripts( spec );

            spec.close();
        }
        catch ( Throwable t )
        {
            throw new MojoExecutionException( "Unable to write " + specf.getAbsolutePath(), t );
        }
    }

    /**
     * Print all the script commands to the <i>writer</i>.
     * 
     * @param writer to print script tags to
     */
    private void printScripts( PrintWriter writer )
    {
        if ( preinstall != null )
        {
            writer.println();
            writer.println( "%pre" );
            writer.println( preinstall );
        }
        if ( install != null )
        {
            writer.println();
            writer.println( "%install" );
            writer.println( install );
        }
        if ( postinstall != null )
        {
            writer.println();
            writer.println( "%post" );
            writer.println( postinstall );
        }
        if ( preremove != null )
        {
            writer.println();
            writer.println( "%preun" );
            writer.println( preremove );
        }
        if ( postremove != null )
        {
            writer.println();
            writer.println( "%postun" );
            writer.println( postremove );
        }
        if ( verify != null )
        {
            writer.println();
            writer.println( "%verifyscript" );
            writer.println( verify );
        }
        if ( clean != null )
        {
            writer.println();
            writer.println( "%clean" );
            writer.println( clean );
        }
    }

    /**
     * Builds a list of all the file names (relative to {@link Mapping#getDirectory()}) that are included in the
     * {@link Mapping#getSources() sources}.
     * 
     * @param map The Mapping to examine.
     * @return all the file names (relative to {@link Mapping#getDirectory()}) that are included in the
     *         {@link Mapping#getSources() sources}.
     */
    private static List determineFileIncludes( Mapping map )
    {
        // list of relative files to dest that were defined for this mapping (from all sources).
        List includes = new LinkedList();

        List sources = map.getSources();
        if ( sources != null )
        {
            for ( Iterator sit = sources.iterator(); sit.hasNext(); )
            {
                Source source = (Source) sit.next();

                List copiedFiles = source.getCopiedFileNamesRelativeToDestination();
                if ( copiedFiles != null && !copiedFiles.isEmpty() )
                {
                    includes.addAll( copiedFiles );
                }
            }
        }
        return includes;
    }

    /**
     * Writes a new line for each element in <i>strings</i> to the <i>writer</i> with the <i>prefix</i>.
     * 
     * @param writer <tt>PrintWriter</tt> to write to.
     * @param strings <tt>List</tt> of <tt>String</tt>s to write.
     * @param prefix Prefix to write on each line before the string.
     */
    private static void writeList( PrintWriter writer, List strings, String prefix )
    {
        if ( strings != null )
        {
            for ( Iterator it = strings.iterator(); it.hasNext(); )
            {
                writer.print( prefix );
                writer.println( it.next() );
            }
        }
    }

    /**
     * Generates the copyright text from {@link MavenProject#getOrganization()} and
     * {@link MavenProject#getInceptionYear()}.
     * 
     * @return Generated copyright text from the organization name and inception year.
     */
    private String generateCopyrightText()
    {
        String copyrightText;
        String year = project.getInceptionYear();
        String organization = project.getOrganization() == null ? null : project.getOrganization().getName();
        if ( ( year != null ) && ( organization != null ) )
        {
            copyrightText = year + " " + organization;
        }
        else
        {
            if ( year == null )
            {
                copyrightText = organization;
            }
            else
            {
                copyrightText = year;
            }
        }
        return copyrightText;
    }
}
