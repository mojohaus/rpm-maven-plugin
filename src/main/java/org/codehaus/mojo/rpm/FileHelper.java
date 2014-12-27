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
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.versioning.OverConstrainedVersionException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.archiver.ArchiveEntry;
import org.codehaus.plexus.archiver.util.DefaultFileSet;
import org.codehaus.plexus.util.StringUtils;

/**
 * Utility to interact with files (source, dependencies, artifacts, etc.).
 *
 * @author Brett Okken
 * @since 2.0
 */
final class FileHelper
{
    private static final List<String> EMPTY_STRING_LIST = new ArrayList<String>();

    /**
     * Message for exception indicating that a {@link Source} has a {@link Source#getDestination() destination}, but
     * refers to a {@link File#isDirectory() directory}.
     */
    private static final String DESTINATION_DIRECTORY_ERROR_MSG =
        "Source has a destination [{0}], but the location [{1}] does not refer to a file.";

    /**
     * {@code Pattern} to identify macros.
     *
     * @since 2.1-alpha-1
     */
    private static final Pattern MACRO_PATTERN = Pattern.compile( "%\\{([^}]*)\\}" );

    /**
     * A Plexus component to copy files and directories. Using our own custom version of the DirectoryArchiver to allow
     * filtering of files.
     */
    private final FilteringDirectoryArchiver copier;

    private final AbstractRPMMojo mojo;

    /**
     * @param mojo
     * @param copier
     */
    public FileHelper( AbstractRPMMojo mojo, FilteringDirectoryArchiver copier )
    {
        super();
        this.mojo = mojo;
        this.copier = copier;
    }

    /**
     * Copy the files from the various mapping sources into the build root.
     *
     * @throws MojoExecutionException if a problem occurs
     * @throws MojoFailureException
     */
    public void installFiles()
        throws MojoExecutionException, MojoFailureException
    {
        final File workarea = mojo.getWorkarea();
        final File buildroot = mojo.getBuildroot();

        final File icon = mojo.getIcon();
        // Copy icon, if specified
        if ( icon != null )
        {
            File icondest = new File( workarea, "SOURCES" );
            copySource( icon, null, icondest, null, null, false, false );
        }

        final Log log = mojo.getLog();

        // Process each mapping
        for ( Mapping map : mojo.getMappings() )
        {
            final String destinationString = map.getDestination();
            final String macroEvaluatedDestination = evaluateMacros( destinationString );

            File dest = new File( buildroot, macroEvaluatedDestination );
            map.setAbsoluteDestination( dest );

            if ( map.isDirOnly() )
            {
                // Build the output directory if it doesn't exist
                if ( !dest.exists() )
                {
                    log.info( "Creating empty directory " + dest.getAbsolutePath() );
                    if ( !dest.mkdirs() )
                    {
                        throw new MojoExecutionException( "Unable to create " + dest.getAbsolutePath() );
                    }
                }
            }
            else
            {
                processSources( map, dest );

                ArtifactMap art = map.getArtifact();
                if ( art != null )
                {
                    List<Artifact> artlist = selectArtifacts( art );
                    for ( Artifact artifactInstance : artlist )
                    {
                        copyArtifact( artifactInstance, dest, false );
                        map.addCopiedFileNameRelativeToDestination( artifactInstance.getFile().getName() );
                    }
                }

                Dependency dep = map.getDependency();
                if ( dep != null )
                {
                    List<Artifact> deplist = selectDependencies( dep );
                    for ( Artifact artifactInstance : deplist )
                    {
                        // pass in dependency stripVersion parameter
                        String outputFileName = copyArtifact( artifactInstance, dest, dep.getStripVersion() );
                        map.addCopiedFileNameRelativeToDestination( outputFileName );
                    }
                }

                if ( map.getCopiedFileNamesRelativeToDestination().isEmpty() )
                {
                    log.info( "Mapping empty with destination: " + dest.getName() );
                    // Build the output directory if it doesn't exist
                    if ( !dest.exists() )
                    {
                        log.info( "Creating empty directory " + dest.getAbsolutePath() );
                        if ( !dest.mkdirs() )
                        {
                            throw new MojoExecutionException( "Unable to create " + dest.getAbsolutePath() );
                        }
                    }
                }
            }
        }
    }

    /**
     * Copy a set of files.
     *
     * @param src The file or directory to start copying from
     * @param srcName The src file name to be used in the copy, only used if the src is not a directory.
     * @param dest The destination directory
     * @param incl The list of inclusions
     * @param excl The list of exclusions
     * @param filter Indicates if the file(s) being copied should be filtered.
     * @return List of file names, relative to <i>dest</i>, copied to <i>dest</i>.
     * @throws MojoExecutionException if a problem occurs
     */
    private List<String> copySource( File src, String srcName, File dest, List<String> incl, List<String> excl,
                                     boolean filter, boolean dontUseDefaultExcludes )
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
                    ia = incl.toArray( new String[0] );
                }

                String[] ea = null;
                if ( excl != null )
                {
                    ea = excl.toArray( new String[0] );
                }

                // same as copier.addDirectory( src, "", ia, ea ) with dontUseDefaultExcludes
                DefaultFileSet fileset = new DefaultFileSet( src );
                fileset.setPrefix( "" );
                fileset.setIncludes( ia );
                fileset.setExcludes( ea );
                fileset.setIncludingEmptyDirectories( copier.getIncludeEmptyDirs() );
                fileset.setUsingDefaultExcludes( !dontUseDefaultExcludes );

                copier.addFileSet( fileset );
            }
            else
            {
                // set srcName to default if null
                srcName = srcName != null ? srcName : src.getName();
                copier.addFile( src, srcName );
            }

            copier.setFilter( filter );
            copier.setFilterWrappers( mojo.getFilterWrappers() );

            Map<String, ArchiveEntry> copiedFilesMap = copier.getFiles();

            // Perform the copy
            copier.createArchive();

            List<String> copiedFiles = new ArrayList<String>( copiedFilesMap.size() );
            for ( String key : copiedFilesMap.keySet() )
            {
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

    public static DefaultFileSet fileSet( File directory )
    {
        final DefaultFileSet defaultFileSet = new DefaultFileSet( directory );
        return defaultFileSet;
    }

    /**
     * Copy an artifact.
     *
     * @param art The artifact to copy
     * @param dest The destination directory
     * @param stripVersion Whether or not to strip the artifact version from the filename
     * @return Artifact file name
     * @throws MojoExecutionException if a problem occurs
     */
    private String copyArtifact( Artifact art, File dest, boolean stripVersion )
        throws MojoExecutionException
    {
        if ( art.getFile() == null )
        {
            final Log log = mojo.getLog();
            log.warn( "Artifact " + art + " requested in configuration." );
            log.warn( "Plugin must be run in Maven's build lifecycle for this to work." );
            throw new MojoExecutionException( "Unable to resolve artifact." );
        }

        String outputFileName;
        if ( stripVersion )
        {
            final String classifier = art.getClassifier();
            // strip the version from the file name
            outputFileName = art.getArtifactId();
            if ( classifier != null )
            {
                outputFileName += '-';
                outputFileName += classifier;
            }
            outputFileName += '.';
            outputFileName += art.getType();
        }
        else
        {
            outputFileName = art.getFile().getName();
        }

        copySource( art.getFile(), outputFileName, dest, null, null, false, false );
        return outputFileName;
    }

    /**
     * Make a list of the artifacts to package in this mapping.
     *
     * @param am The artifact mapping information
     * @return The list of artifacts to package
     */
    private List<Artifact> selectArtifacts( ArtifactMap am )
    {
        final List<Artifact> retval = new ArrayList<Artifact>();
        final List<String> clist = am.getClassifiers();

        final Artifact artifact = mojo.getArtifact();
        final List<Artifact> attachedArtifacts = mojo.getAttachedArtifacts();

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
            for ( Artifact aa : attachedArtifacts )
            {
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
    private List<Artifact> selectDependencies( Dependency d )
    {
        List<Artifact> retval = new ArrayList<Artifact>();
        List<Artifact> inc = d.getIncludes();
        List<Artifact> exc = d.getExcludes();

        @SuppressWarnings( "unchecked" )
        Set<Artifact> deps = mojo.project.getArtifacts();
        if ( deps == null || deps.isEmpty() )
        {
            return retval;
        }

        final Log log = mojo.getLog();

        for ( Artifact pdep : deps )
        {
            log.debug( "Dependency is " + pdep + " at " + pdep.getFile() );
            if ( !depMatcher( pdep, exc ) )
            {
                log.debug( "--> not excluded" );
                if ( ( inc == null ) || ( depMatcher( pdep, inc ) ) )
                {
                    log.debug( "--> included" );
                    retval.add( pdep );
                }
            }
        }

        return retval;
    }

    /**
     * Installs the {@link Mapping#getSources() sources} to <i>dest</i>
     *
     * @param map The <tt>Mapping</tt> to process the {@link Source sources} for.
     * @param dest The destination directory for the sources.
     * @throws MojoExecutionException
     * @throws MojoFailureException
     */
    private void processSources( Mapping map, File dest )
        throws MojoExecutionException, MojoFailureException
    {
        if ( !dest.exists() )
        {
            if ( !dest.mkdirs() )
            {
                throw new MojoExecutionException( "unable to create directory: " + dest.getAbsolutePath() );
            }
        }

        String relativeDestination = map.getDestination();
        if ( !relativeDestination.endsWith( File.separator ) )
        {
            relativeDestination += File.separatorChar;
        }

        List<Source> srcs = map.getSources();
        if ( srcs != null )
        {
            // for passivity, we will always use lowercase representation of architecture
            // for comparison purposes.
            final String targetArchComparison = mojo.getTargetArch().toLowerCase( Locale.ENGLISH );

            final String targetOS = mojo.getTargetOS();

            final Map<String, List<SoftlinkSource>> linkTargetToSources = mojo.getLinkTargetToSources();

            // it is important that for each Source we set the files that are "installed".
            for ( Source src : srcs )
            {
                if ( !src.matchesArchitecture( targetArchComparison ) )
                {
                    mojo.getLog().debug( "Source does not match target architecture: " + src.toString() );
                    continue;
                }

                if ( !src.matchesOSName( targetOS ) )
                {
                    mojo.getLog().debug( "Source does not match target os name: " + src.toString() );
                    continue;
                }

                final File macroEvaluatedLocation = new File( evaluateMacros( src.getLocation() ) );
                src.setMacroEvaluatedLocation( macroEvaluatedLocation.getPath() );

                final File locationFile =
                    macroEvaluatedLocation.isAbsolute() ? macroEvaluatedLocation
                                    : new File( mojo.project.getBasedir(), macroEvaluatedLocation.getPath() );
                // better with just macroEvaluatedLocation.getAbsoluteFile(), but not tested yet

                // it is important that we check if softlink source first as the "location" may
                // exist in the filesystem of the build machine
                if ( src instanceof SoftlinkSource )
                {
                    SoftlinkSource softlinkSource = (SoftlinkSource) src;
                    List<SoftlinkSource> sources = linkTargetToSources.get( relativeDestination );
                    if ( sources == null )
                    {
                        sources = new LinkedList<SoftlinkSource>();
                        linkTargetToSources.put( relativeDestination, sources );
                    }

                    sources.add( softlinkSource );

                    softlinkSource.setSourceMapping( map );
                    map.setHasSoftLinks( true );
                }
                else if ( locationFile.exists() )
                {
                    final String destination = src.getDestination();
                    if ( destination == null )
                    {
                        map.addCopiedFileNamesRelativeToDestination( copySource( locationFile, null, dest,
                                                                                 src.getIncludes(), src.getExcludes(),
                                                                                 src.isFilter(),
                                                                                 src.getNoDefaultExcludes() ) );
                    }
                    else
                    {
                        if ( !locationFile.isFile() )
                        {
                            throw new MojoExecutionException( MessageFormat.format( DESTINATION_DIRECTORY_ERROR_MSG,
                                                                                    new Object[] { destination,
                                                                                        macroEvaluatedLocation } ) );
                        }

                        copySource( locationFile, destination, dest, EMPTY_STRING_LIST, EMPTY_STRING_LIST,
                                    src.isFilter(), src.getNoDefaultExcludes() );

                        map.addCopiedFileNameRelativeToDestination( destination );
                    }
                }
                else if ( !src.getFailIfLocationNotExists() )
                {
                    mojo.getLog().warn( "Source location " + macroEvaluatedLocation
                                            + " does not exist, but failIfLocationNotExist was set to false" );
                }
                else
                {
                    throw new MojoExecutionException( "Source location " + macroEvaluatedLocation + " does not exist" );
                }
            }
        }
    }

    /**
     * Determine if there are any macros in the <i>value</i> and replace any/all occurrences with the
     * {@link AbstractRPMMojo#evaluateMacro(String) evaluated} value.
     *
     * @param value String to replace macros in.
     * @return Result of evaluating all macros in <i>value</i>.
     * @throws MojoExecutionException
     * @since 2.1-alpha-1
     */
    private String evaluateMacros( String value )
        throws MojoExecutionException
    {
        final Matcher matcher = MACRO_PATTERN.matcher( value );

        final StringBuffer newValue = new StringBuffer( value.length() );
        while ( matcher.find() )
        {
            final String macro = matcher.group( 1 );
            final String evaluatedValue = mojo.evaluateMacro( macro );
            matcher.appendReplacement( newValue, evaluatedValue.replaceAll( "\\\\", "\\\\\\\\" ) );
        }

        matcher.appendTail( newValue );
        return newValue.toString();
    }

    /**
     * Determine if the dependency matches an include or exclude list.
     *
     * @param dep The dependency to check
     * @param list The list to check against
     * @return <code>true</code> if the dependency was found on the list
     */
    private boolean depMatcher( Artifact dep, List<Artifact> list )
    {
        if ( list == null )
        {
            // No list, not possible to match
            return false;
        }

        final Log log = mojo.getLog();

        for ( Artifact item : list )
        {
            log.debug( "Compare " + dep + " to " + item );
            final String groupId = item.getGroupId();
            if ( StringUtils.isEmpty( groupId ) || "*".equals( groupId ) || groupId.equals( dep.getGroupId() ) )
            {
                log.debug( "... Group matches" );
                final String artifactId = item.getArtifactId();
                if ( StringUtils.isEmpty( artifactId ) || "*".equals( artifactId )
                    || artifactId.equals( dep.getArtifactId() ) )
                {
                    log.debug( "... Artifact matches" );
                    // ArtifactVersion av = item.getVersionRange().matchVersion( dep.getAvailableVersions() );
                    try
                    {
                        if ( item.getVersionRange().containsVersion( dep.getSelectedVersion() ) )
                        {
                            log.debug( "... Version matches" );
                            return true;
                        }
                    }
                    catch ( OverConstrainedVersionException ocve )
                    {
                        log.debug( "... caught OverConstrainedVersionException" );
                    }
                }
            }
        }

        // Not found
        return false;
    }

    //
    // Convenient utils to convert to cygwin if needed
    //

    public static final String UNIX_FILE_SEPARATOR = "/";

    public static String toUnixPath( File path )
    {
        return toUnixPath( path.getAbsolutePath() );
    }

    public static String toUnixPath( String path )
    {
        path = StringUtils.replace( path, "\\", "/" );

        String[] tokens = StringUtils.split( path, ":" );

        if ( tokens.length == 2 && tokens[0].length() == 1 )
        {
            StringBuilder buffer = new StringBuilder();
            buffer.append( "/cygdrive/" ).append( tokens[0] ).append( tokens[1] );
            path = buffer.toString();
        }

        return path;
    }
}
