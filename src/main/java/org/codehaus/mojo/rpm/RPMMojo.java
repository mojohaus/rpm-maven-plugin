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
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.versioning.OverConstrainedVersionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.dir.DirectoryArchiver;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.StreamConsumer;


/**
 * Construct the RPM file
 * @version $Id$
 * @requiresDependencyResolution runtime
 * @goal rpm
 * @phase package
 */
public class RPMMojo extends AbstractMojo
{

    /**
     * The name portion of the output file name.
     * @parameter expression="${project.artifactId}"
     * @required
     */
    private String name;

    /**
     * The version portion of the RPM file name.
     * @parameter alias="version" expression="${project.version}"
     * @required
     */
    private String projversion;

    /**
     * The release portion of the RPM file name.
     * @parameter
     * @required
     */
    private String release;

    /**
     * Set to <code>true</code> if the package is dependent on the architecture
     * of the build machine.
     * @parameter
     */
    private boolean needarch;

    /**
     * Set to a key name to sign the package using GPG.  Note that due
     * to RPM limitations, this always requires input from the
     * terminal even if the key has no passphrase.
     * @parameter expression="${gpg.keyname}"
     */
    private String keyname;

    /**
     * The long description of the package.
     * @parameter expression="${project.description}"
     */
    private String description;

    /**
     * The one-line description of the package.
     * @parameter expression="${project.name}"
     */
    private String summary;

    /**
     * The one-line copyright information.
     * @parameter
     */
    private String copyright;

    /**
     * The distribution containing this package.
     * @parameter
     */
    private String distribution;

    /**
     * An icon for the package.
     * @parameter
     */
    private File icon;

    /**
     * The vendor supplying the package.
     * @parameter expression="${project.organization.name}"
     */
    private String vendor;

    /**
     * A URL for the vendor.
     * @parameter expression="${project.organization.url}"
     */
    private String url;

    /**
     * The package group for the package.
     * @parameter
     * @required
     */
    private String group;

    /**
     * The name of the person or group creating the package.
     * @parameter expression="${project.organization.name}"
     */
    private String packager;

    /**
     * The list of virtual packages provided by this package.
     * @parameter
     */
    private List provides;

    /**
     * The list of requirements for this package.
     * @parameter
     */
    private List requires;

    /**
     * The list of conflicts for this package.
     * @parameter
     */
    private List conflicts;

    /**
     * The relocation prefix for this package.
     * @parameter
     */
    private String prefix;

    /**
     * The area for RPM to use for building the package.
     * @parameter expression="${project.build.directory}/rpm"
     */
    private File workarea;

    /**
     * The list of file mappings.
     * @parameter
     * @required
     */
    private List mappings;

    /**
     * The pre-installation script.
     * @parameter
     */
    private String preinstall;

    /**
     * The location of the pre-installation script.
     * @parameter
     */
    private File preinstallScript;

    /**
     * The post-installation script.
     * @parameter
     */
    private String postinstall;

    /**
     * The location of the post-installation script.
     * @parameter
     */
    private File postinstallScript;

    /**
     * The installation script.
     * @parameter
     */
    private String install;

    /**
     * The location of the installation script.
     * @parameter
     */
    private File installScript;

    /**
     * The pre-removal script.
     * @parameter
     */
    private String preremove;

    /**
     * The location of the pre-removal script.
     * @parameter
     */
    private File preremoveScript;

    /**
     * The post-removal script.
     * @parameter
     */
    private String postremove;

    /**
     * The location of the post-removal script.
     * @parameter
     */
    private File postremoveScript;

    /**
     * The verification script.
     * @parameter
     */
    private String verify;

    /**
     * The location of the verification script.
     * @parameter
     */
    private File verifyScript;

    /**
     * The clean script.
     * @parameter
     */
    private String clean;

    /**
     * The location of the clean script.
     * @parameter
     */
    private File cleanScript;

    /**
     * A Plexus component to copy files and directories.
     * @component role="org.codehaus.plexus.archiver.Archiver"
     *            roleHint="dir"
     */
    private DirectoryArchiver copier;

    /**
     * The primary project artifact.
     * @parameter expression="${project.artifact}"
     * @required
     * @readonly
     */
    private Artifact artifact;

    /**
     * Auxillary project artifacts.
     * @parameter expression="${project.attachedArtifacts}
     * @required
     * @readonly
     */
    private List attachedArtifacts;

    /**
     * @parameter default-value="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * A list of %define arguments
     * @parameter
     */
    private List defineStatements;


    /** The root of the build area. */
    private File buildroot;

    /** The version string after parsing. */
    private String version;

    // // //  Consumers for rpmbuild output

    /**
     * Consumer to receive lines sent to stdout.  The lines are logged
     * as info.
     */
    private class StdoutConsumer  implements StreamConsumer
    {
        /** Logger to receive the lines. */
        private Log logger;

        /**
         * Constructor.
         * @param log The logger to receive the lines
         */
        public StdoutConsumer( Log log )
        {
            logger = log;
        }

        /**
         * Consume a line.
         * @param string The line to consume
         */
        public void consumeLine( String string )
        {
            logger.info( string );
        }
    }

    /**
     * Consumer to receive lines sent to stderr.  The lines are logged
     * as warnings.
     */
    private class StderrConsumer  implements StreamConsumer
    {
        /** Logger to receive the lines. */
        private Log logger;

        /**
         * Constructor.
         * @param log The logger to receive the lines
         */
        public StderrConsumer( Log log )
        {
            logger = log;
        }

        /**
         * Consume a line.
         * @param string The line to consume
         */
        public void consumeLine( String string )
        {
            logger.warn( string );
        }
    }

    // // //  Mojo methods

    /** {@inheritDoc} */
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        checkParams();
        buildWorkArea();
        writeSpecFile();
        installFiles();
        buildPackage();
    }

    // // //  Internal methods

    /**
     * Run the external command to build the package.
     * @throws MojoExecutionException if an error occurs
     */
    private void buildPackage() throws MojoExecutionException
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
     * @throws MojoFailureException if a directory cannot be built
     */
    private void buildWorkArea() throws MojoFailureException
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
     * @throws MojoFailureException if an invalid parameter is found
     * @throws MojoExecutionException if an error occurs reading a script
     */
    private void checkParams() throws MojoExecutionException, MojoFailureException
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
     * @param art The artifact to copy
     * @param dest The destination directory
     * @throws MojoExecutionException if a problem occurs
     */
    private void copyArtifact( Artifact art, File dest ) throws MojoExecutionException
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
     * @param src The file or directory to start copying from
     * @param dest The destination directory
     * @param incl The list of inclusions
     * @param excl The list of exclusions
     * @throws MojoExecutionException if a problem occurs
     */
    private void copySource( File src, File dest, List incl, List excl ) throws MojoExecutionException
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

            // Clear the list for the next mapping
            copier.resetArchiver();
        }
        catch ( Throwable t )
        {
            throw new MojoExecutionException( "Unable to copy files for packaging: " + t.getMessage(), t );
        }
    }

    /**
     * Determine if the dependency matches an include or exclude list.
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
            if ( item.getGroupId().equals( dep.getGroupId() ) )
            {
                getLog().debug( "... Group matches" );
                if ( item.getArtifactId().equals( dep.getArtifactId() ) )
                {
                    getLog().debug( "... Artifact matches" );
                    //ArtifactVersion av = item.getVersionRange().matchVersion( dep.getAvailableVersions() );
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
     * @throws MojoExecutionException if a problem occurs
     */
    private void installFiles() throws MojoExecutionException
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
                    for ( Iterator sit = srcs.iterator(); sit.hasNext(); )
                    {
                        Source src = (Source) sit.next();
                        if ( src.getLocation().exists() )
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
                            copySource( src.getLocation(), dest, src.getIncludes(), elist );
                        }
                        else
                        {
                            throw new MojoExecutionException( "Source location " + src.getLocation()
                                    + " does not exist" );
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
     * @param in The file to read
     * @return The file contents
     * @throws MojoExecutionException if an error occurs reading the file
     */
    private String readFile( File in ) throws MojoExecutionException
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
     * @param d The artifact mapping information
     * @return The list of artifacts to package
     */
    private List selectDependencies( Dependency d )
    {
        List retval = new ArrayList();
        List inc = d.getIncludes();
        List exc = d.getExcludes();

        List deps = project.getRuntimeArtifacts();
        if ( deps == null || deps.isEmpty() )
        {
            return retval;
        }

        for ( Iterator it = deps.iterator(); it.hasNext(); )
        {
            Artifact pdep = ( Artifact ) it.next();
            getLog().debug( "Dependency is " + pdep + " at " + pdep.getFile() );
            if ( ! depMatcher( pdep, exc ) )
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
     * @throws MojoExecutionException if an error occurs writing the file
     */
    private void writeSpecFile() throws MojoExecutionException
    {
        File f = new File( workarea, "SPECS" );
        File specf = new File( f, name + ".spec" );
        try
        {
            getLog().info( "Creating spec file " + specf.getAbsolutePath() );
            PrintWriter spec = new PrintWriter( new FileWriter( specf ) );

            if ( null != defineStatements )
            {
                Iterator defineIter = defineStatements.iterator();
                while ( defineIter.hasNext() )
                {
                    String defineStatement = (String) defineIter.next();
                    spec.println( "%define " + defineStatement );
            }
            }

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
                    } else {
                        copyrightText = year;
                    }
                }
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
            if ( provides != null )
            {
                for ( Iterator it = provides.iterator(); it.hasNext(); )
                {
                    spec.println( "Provides: " + it.next() );
                }
            }
            if ( requires != null )
            {
                for ( Iterator it = requires.iterator(); it.hasNext(); )
                {
                    spec.println( "Requires: " + it.next() );
                }
            }
            if ( conflicts != null )
            {
                for ( Iterator it = conflicts.iterator(); it.hasNext(); )
                {
                    spec.println( "Conflicts: " + it.next() );
                }
            }
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

            	boolean listFiles = false;

            	if (map.getSources() != null)
            	{
            		// Check if all sources contains only files
            		listFiles = true;
            		for ( Iterator sources = map.getSources().iterator(); sources.hasNext(); )
            		{
            			Source source = (Source) sources.next();
            			if (source.getLocation().isDirectory())
            			{
            				listFiles = false;
            				break;
            			}
            		}
            	}

            	if (listFiles)
            	{
            		// Write a line in the spec file for each file
            		for ( Iterator sources = map.getSources().iterator(); sources.hasNext(); )
            		{
            			Source source = (Source) sources.next();
            			spec.println( map.getAttrString() + " " + map.getDestination()
            					+ File.separator + source.getLocation().getName());
            		}
            	}
            	else
            	{
            		spec.println( map.getAttrString() + " " + map.getDestination() );
            	}
            }

            if ( preinstall != null )
            {
                spec.println();
                spec.println( "%pre" );
                spec.println( preinstall );
            }
            if ( install != null )
            {
                spec.println();
                spec.println( "%install" );
                spec.println( install );
            }
            if ( postinstall != null )
            {
                spec.println();
                spec.println( "%post" );
                spec.println( postinstall );
            }
            if ( preremove != null )
            {
                spec.println();
                spec.println( "%preun" );
                spec.println( preremove );
            }
            if ( postremove != null )
            {
                spec.println();
                spec.println( "%postun" );
                spec.println( postremove );
            }
            if ( verify != null )
            {
                spec.println();
                spec.println( "%verifyscript" );
                spec.println( verify );
            }
            if ( clean != null )
            {
                spec.println();
                spec.println( "%clean" );
                spec.println( clean );
            }

            spec.close();
        }
        catch ( Throwable t )
        {
            throw new MojoExecutionException( "Unable to write " + specf.getAbsolutePath(), t );
        }
    }
}
