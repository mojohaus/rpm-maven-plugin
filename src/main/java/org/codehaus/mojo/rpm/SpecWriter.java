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
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.util.DirectoryScanner;

/**
 * Utility to write spec file based on {@link AbstractRPMMojo} instance.
 * 
 * @author Brett Okken
 * @version $Revision$
 * @since 2.0
 */
final class SpecWriter
{
    private final AbstractRPMMojo mojo;

    private final PrintWriter spec;

    /**
     * Creates instance with the <i>mojo</i> to use and the <i>spec</i> to write to.
     * 
     * @param mojo The mojo with the attributes to generate a spec file for.
     * @param spec The target to write the spec file to.
     */
    public SpecWriter( AbstractRPMMojo mojo, PrintWriter spec )
    {
        super();
        this.mojo = mojo;
        this.spec = spec;
    }

    /**
     * Writes the spec file to <i>spec</i> using the attributes of <i>mojo</i>.
     * 
     * @param mojo The mojo with the attributes to generate a spec file for.
     * @param spec The target to write the spec file to.
     * @throws MojoExecutionException
     * @throws IOException
     */
    public void writeSpecFile()
        throws MojoExecutionException, IOException
    {
        writeList( mojo.getDefineStatements(), "%define " );

        spec.println( "Name: " + mojo.getName() );
        spec.println( "Version: " + mojo.getVersion() );
        spec.println( "Release: " + mojo.getRelease() );

        writeNonNullDirective( "Summary", mojo.getSummary() );
        writeNonNullDirective( "License", mojo.getCopyright() );
        writeNonNullDirective( "Distribution", mojo.getDistribution() );

        if ( mojo.getIcon() != null )
        {
            spec.println( "Icon: " + mojo.getIcon().getName() );
        }

        writeNonNullDirective( "Vendor", mojo.getVendor() );
        writeNonNullDirective( "URL", mojo.getUrl() );
        writeNonNullDirective( "Group", mojo.getGroup() );
        writeNonNullDirective( "Packager", mojo.getPackager() );

        writeList( mojo.getProvides(), "Provides: " );
        writeList( mojo.getRequires(), "Requires: " );
        writeList( mojo.getPrereqs(), "PreReq: " );
        writeList( mojo.getObsoletes(), "Obsoletes: " );
        writeList( mojo.getConflicts(), "Conflicts: " );

        spec.println( "autoprov: " + ( mojo.isAutoProvides() ? "yes" : "no" ) );
        spec.println( "autoreq: " + ( mojo.isAutoRequires() ? "yes" : "no" ) );

        if ( mojo.getPrefix() != null )
        {
            spec.println( "Prefix: " + mojo.getPrefix() );
        }
        spec.println( "BuildRoot: " + mojo.getRPMBuildroot().getAbsolutePath() );
        spec.println();
        spec.println( "%description" );
        if ( mojo.getDescription() != null )
        {
            spec.println( mojo.getDescription() );
        }
        
        writeMove();

        writeLinks();

        if ( mojo.getInstallScriptlet() != null )
        {
            spec.println();
            mojo.getInstallScriptlet().writeContent( spec );
        }

        writeFiles();

        writeScripts();

        if ( mojo.getTriggers() != null )
        {
            for ( Iterator i = mojo.getTriggers().iterator(); i.hasNext(); )
            {
                BaseTrigger trigger = (BaseTrigger) i.next();
                trigger.writeTrigger( spec );
            }
        }

        if ( mojo.getChangelog() != null )
        {
            spec.println();
            spec.println( "%changelog" );
            spec.println( mojo.getChangelog() );
        }
    }

    /**
     * Writes the %files directive based on {@link AbstractRPMMojo#mappings}.
     */
    private void writeFiles()
    {
        final Log log = mojo.getLog();

        spec.println();
        spec.println( "%files" );
        spec.println( getDefAttrString() );

        for ( Iterator it = mojo.getMappings().iterator(); it.hasNext(); )
        {
            Mapping map = (Mapping) it.next();

            // For each mapping we need to determine which files in the destination were defined by this
            // mapping so that we can write the %attr statement correctly.

            final String destination = map.getDestination();
            final File absoluteDestination = map.getAbsoluteDestination();

            if ( map.hasSoftLinks() && !absoluteDestination.exists() )
            {
                log.debug( "writing attribute string for directory created by soft link: " + destination );

                final String attributes =
                    map.getAttrString( mojo.getDefaultFilemode(), mojo.getDefaultGroupname(), mojo.getDefaultUsername() );

                spec.print( attributes );
                spec.print( ' ' );
                spec.println( destination );

                continue;
            }

            final List includes = map.getCopiedFileNamesRelativeToDestination();
            final List links = map.getLinkedFileNamesRelativeToDestination();

            final DirectoryScanner scanner = new DirectoryScanner();
            scanner.setBasedir( absoluteDestination );

            // the linked files are not present yet (will be "installed" during rpm build)
            // so they cannot be "included"
            scanner.setIncludes( includes.isEmpty() ? null : 
                (String[]) includes.toArray( new String[ includes.size() ] ) );
            scanner.setExcludes( null );
            scanner.scan();

            final String attrString =
                map.getAttrString( mojo.getDefaultFilemode(), mojo.getDefaultGroupname(), mojo.getDefaultUsername() );
            if ( scanner.isEverythingIncluded() && links.isEmpty() && map.isDirectoryIncluded()
                && !map.isRecurseDirectories() )
            {
                log.debug( "writing attribute string for directory: " + destination );
                spec.println( attrString + " " + destination );
            }
            else
            {
                log.debug( "writing attribute string for identified files in directory: " + destination );

                final String baseFileString = attrString + " " + destination + File.separatorChar;

                // only list files if requested (directoryIncluded == false) or we have to
                if ( !( map.isDirectoryIncluded() && scanner.isEverythingIncluded() && links.isEmpty() ) )
                {
                    final String[] files = scanner.getIncludedFiles();

                    for ( int i = 0; i < files.length; ++i )
                    {
                        spec.print( baseFileString );
                        spec.println( files[i] );
                    }
                }

                if ( map.isRecurseDirectories() )
                {
                    final String[] dirs = scanner.getIncludedDirectories();
                    
                    if ( map.isDirectoryIncluded() )
                    {
                        // write out destination first
                        spec.println( baseFileString );
                    }

                    for ( int i = 0; i < dirs.length; ++i )
                    {
                        // do not write out base file (destination) again
                        if ( dirs[i].length() > 0 )
                        {
                            spec.print( baseFileString );
                            spec.println( dirs[i] );
                        }
                    }
                }

                // since the linked files are not present in directory (yet), the scanner will not find them
                for ( Iterator linkIter = links.iterator(); linkIter.hasNext(); )
                {
                    String link = (String) linkIter.next();

                    spec.print( baseFileString );
                    spec.println( link );
                }
            }
        }
    }

    /**
     * Writes the beginning of the <i>%install</i> which includes moving all files from the
     * {@link AbstractRPMMojo#getBuildroot()} to {@link AbstractRPMMojo#getRPMBuildroot()}.
     */
    private void writeMove()
    {
        final String tmpBuildRoot = mojo.getBuildroot().getAbsolutePath();

        spec.println();
        spec.println( "%install" );
        spec.println( "if [ -e $RPM_BUILD_ROOT ];" );
        spec.println( "then" );
        spec.print( "  mv " );
        spec.print( tmpBuildRoot );
        spec.println( "/* $RPM_BUILD_ROOT" );
        spec.println( "else" );
        spec.print( "  mv " );
        spec.print( tmpBuildRoot );
        spec.println( " $RPM_BUILD_ROOT" );
        spec.println( "fi" );
    }

    /**
     * Writes the install commands to link files.
     */
    private void writeLinks()
    {
        if ( !mojo.getLinkTargetToSources().isEmpty() )
        {
            spec.println();

            for ( Iterator entryIter = mojo.getLinkTargetToSources().entrySet().iterator(); entryIter.hasNext(); )
            {
                final Map.Entry directoryToSourcesEntry = (Entry) entryIter.next();
                String directory = (String) directoryToSourcesEntry.getKey();
                if ( directory.startsWith( "/" ) )
                {
                    directory = directory.substring( 1 );
                }
                if ( directory.endsWith( "/" ) )
                {
                    directory = directory.substring( 0, directory.length() - 1 );
                }

                final List sources = (List) directoryToSourcesEntry.getValue();
                final int sourceCnt = sources.size();

                if ( sourceCnt == 1 )
                {
                    final SoftlinkSource linkSource = (SoftlinkSource) sources.get( 0 );

                    final String macroEvaluatedLocation = linkSource.getMacroEvaluatedLocation();
                    
                    final File buildSourceLocation;
                    if ( macroEvaluatedLocation.startsWith( "/" ) )
                    {
                        buildSourceLocation = new File( mojo.getBuildroot(), macroEvaluatedLocation );
                    }
                    else
                    {
                        buildSourceLocation =
                            new File( mojo.getBuildroot(), directory + '/' + macroEvaluatedLocation );
                    }
                    
                    if ( buildSourceLocation.isDirectory() )
                    {
                        final DirectoryScanner scanner = scanLinkSource( linkSource, buildSourceLocation );

                        if ( scanner.isEverythingIncluded() )
                        {
                            final File destinationFile = linkSource.getSourceMapping().getAbsoluteDestination();
                            destinationFile.delete();

                            spec.print( "ln -s " );
                            spec.print( linkSource.getLocation() );
                            spec.print( " $RPM_BUILD_ROOT/" );
                            spec.print( directory );

                            final String dest = linkSource.getDestination();
                            if ( dest != null )
                            {
                                spec.print( '/' );
                                spec.print( dest );
                                linkSource.getSourceMapping().addLinkedFileNameRelativeToDestination( dest );
                            }

                            spec.println();
                        }
                        else
                        {
                            linkScannedFiles( directory, linkSource, scanner );
                        }
                    }
                    else
                    {
                        linkSingleFile( directory, linkSource );
                    }
                }
                else
                {
                    for ( Iterator sourceIter = sources.iterator(); sourceIter.hasNext(); )
                    {
                        final SoftlinkSource linkSource = (SoftlinkSource) sourceIter.next();
                        final String sourceLocation = linkSource.getMacroEvaluatedLocation();
                        
                        final File buildSourceLocation;
                        if ( sourceLocation.startsWith( "/" ) )
                        {
                            buildSourceLocation = new File( mojo.getBuildroot(), sourceLocation );
                        }
                        else
                        {
                            buildSourceLocation =
                                new File( mojo.getBuildroot(), directory + '/' + sourceLocation );
                        }
                        
                        if ( buildSourceLocation.isDirectory() )
                        {
                            final DirectoryScanner scanner = scanLinkSource( linkSource, buildSourceLocation );

                            linkScannedFiles( directory, linkSource, scanner );
                        }
                        else
                        {
                            linkSingleFile( directory, linkSource );
                        }
                    }
                }
            }
        }
    }

    /**
     * Writes soft link from <i>linkSource</i> to <i>directory</i> for all files in the <i>scanner</i>.
     * 
     * @param directory Directory to link to.
     * @param linkSource Source to link from. {@link SoftlinkSource#getLocation()} must be a {@link File#isDirectory()
     *            directory}.
     * @param scanner Scanner used to scan the {@link SoftlinkSource#getLocation() linSource location}.
     */
    private void linkScannedFiles( String directory, final SoftlinkSource linkSource, final DirectoryScanner scanner )
    {
        final String[] files = scanner.getIncludedFiles();
        final String sourceLocation = linkSource.getLocation();

        final String targetPrefix = sourceLocation + File.separatorChar;
        final String sourcePrefix = directory + File.separatorChar;

        for ( int i = 0; i < files.length; ++i )
        {
            spec.print( "ln -s " );
            spec.print( targetPrefix + files[i] );
            spec.print( " $RPM_BUILD_ROOT/" );
            spec.println( sourcePrefix + files[i] );

            linkSource.getSourceMapping().addLinkedFileNameRelativeToDestination( files[i] );
        }
    }

    /**
     * {@link DirectoryScanner#scan() Scans} the <i>buildSourceLocation</i> using the
     * {@link SoftlinkSource#getIncludes()} and {@link SoftlinkSource#getExcludes()} from <i>linkSource</i>. Returns the
     * {@link DirectoryScanner} used for scanning.
     * 
     * @param linkSource Source
     * @param buildSourceLocation Build location where content exists.
     * @return {@link DirectoryScanner} used for scanning.
     */
    private static DirectoryScanner scanLinkSource( final SoftlinkSource linkSource, final File buildSourceLocation )
    {
        final DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir( buildSourceLocation );
        List includes = linkSource.getIncludes();
        scanner.setIncludes( ( includes == null || includes.isEmpty() ) ? null
                        : (String[]) includes.toArray( new String[includes.size()] ) );
        List excludes = linkSource.getExcludes();
        scanner.setExcludes( ( excludes == null || excludes.isEmpty() ) ? null
                        : (String[]) excludes.toArray( new String[excludes.size()] ) );
        scanner.scan();
        return scanner;
    }

    /**
     * Assemble the RPM SPEC default file attributes.
     * 
     * @return The attribute string for the SPEC file.
     */
    private final String getDefAttrString()
    {
        final String defaultFilemode = mojo.getDefaultFilemode();
        final String defaultUsername = mojo.getDefaultUsername();
        final String defaultGroupname = mojo.getDefaultGroupname();
        final String defaultDirmode = mojo.getDefaultDirmode();

        /* do not include %defattr if no default attributes are specified */
        if ( defaultFilemode == null && defaultUsername == null && defaultGroupname == null
            && mojo.getDefaultDirmode() == null && defaultDirmode == null )
        {
            return "";
        }

        StringBuffer sb = new StringBuffer();

        if ( defaultFilemode != null )
        {
            sb.append( "%defattr(" ).append( defaultFilemode ).append( "," );
        }
        else
        {
            sb.append( "%defattr(-," );
        }

        if ( defaultUsername != null )
        {
            sb.append( defaultUsername ).append( "," );
        }
        else
        {
            sb.append( "-," );
        }

        if ( defaultGroupname != null )
        {
            sb.append( defaultGroupname ).append( "," );
        }
        else
        {
            sb.append( "-," );
        }

        if ( defaultDirmode != null )
        {
            sb.append( defaultDirmode ).append( ")" );
        }
        else
        {
            sb.append( "-)" );
        }

        return sb.toString();
    }

    /**
     * Writes soft link from <i>linkSource</i> to <i>directory</i> using optional
     * {@link SoftlinkSource#getDestination()} as the name of the link in <i>directory</i> if present.
     * 
     * @param directory Directory to link to.
     * @param linkSource Source to link from.
     */
    private void linkSingleFile( String directory, final SoftlinkSource linkSource )
    {
        spec.print( "ln -s " );
        spec.print( linkSource.getLocation() );
        spec.print( " $RPM_BUILD_ROOT/" );
        spec.print( directory );
        spec.print( '/' );
        final String destination = linkSource.getDestination();
        final String linkedFileName =
            destination == null ? new File( linkSource.getMacroEvaluatedLocation() ).getName() : destination;
        spec.println( linkedFileName );

        linkSource.getSourceMapping().addLinkedFileNameRelativeToDestination( linkedFileName );
    }

    /**
     * Writes all the scriptlets to the <i>spec</i>.
     */
    private void writeScripts()
        throws IOException
    {
        // all scriptlets in order to write
        final Scriptlet[] scriptlets =
            new Scriptlet[] { mojo.getPrepareScriptlet(), mojo.getPretransScriptlet(), mojo.getPreinstallScriptlet(),
                mojo.getPostinstallScriptlet(), mojo.getPreremoveScriptlet(), mojo.getPostremoveScriptlet(),
                mojo.getPosttransScriptlet(), mojo.getVerifyScriptlet(), mojo.getCleanScriptlet() };

        // all directives, in parallel to scriptlets
        final String[] directives =
            new String[] { "%prep", "%pretrans", "%pre", "%post", "%preun", "%postun", "%posttrans", "%verifyscript",
                "%clean" };

        for ( int i = 0; i < scriptlets.length; ++i )
        {
            if ( scriptlets[i] != null )
            {
                scriptlets[i].write( spec, directives[i] );
            }
        }
    }

    /**
     * If<i>value</i> is not <code>null</code>, writes the <i>value</i> to <i>spec</i>.
     * <p>
     * Writes in format: <code><i>directive</i>: <i>value</i></code>
     * </p>
     * 
     * @param directive
     * @param value
     */
    private final void writeNonNullDirective( final String directive, final String value )
    {
        if ( value != null )
        {
            spec.print( directive );
            spec.print( ": " );
            spec.println( value );
        }
    }

    /**
     * Writes a new line for each element in <i>strings</i> to the <i>writer</i> with the <i>prefix</i>.
     * 
     * @param strings <tt>List</tt> of <tt>String</tt>s to write.
     * @param prefix Prefix to write on each line before the string.
     */
    private void writeList( Collection strings, String prefix )
    {
        if ( strings != null )
        {
            for ( Iterator it = strings.iterator(); it.hasNext(); )
            {
                spec.print( prefix );
                spec.println( it.next() );
            }
        }
    }
}
