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
import java.util.List;

import org.apache.maven.shared.filtering.FilterWrapper;
import org.apache.maven.shared.filtering.MavenFileFilter;
import org.apache.maven.shared.filtering.MavenFilteringException;
import org.codehaus.plexus.archiver.ArchiveEntry;
import org.codehaus.plexus.archiver.dir.DirectoryArchiver;
import org.codehaus.plexus.archiver.util.ArchiveEntryUtils;
import org.codehaus.plexus.components.io.resources.PlexusIoFileResource;

/**
 * Extends the DirectoryArchiver and adds the ability to filter each file that is
 * {@link #copyFile(ArchiveEntry, String) copied}.
 *
 * @author Brett Okken
 * @since 2.0
 */
final class FilteringDirectoryArchiver
    extends DirectoryArchiver
{
    private MavenFileFilter mavenFileFilter;

    private List<FilterWrapper> filterWrappers;

    private boolean filter;

    /**
     * @return Returns the {@link #mavenFileFilter}.
     */
    public MavenFileFilter getMavenFileFilter()
    {
        return this.mavenFileFilter;
    }

    /**
     * @param mavenFileFilter The {@link #mavenFileFilter} to set.
     */
    public void setMavenFileFilter( MavenFileFilter mavenFileFilter )
    {
        this.mavenFileFilter = mavenFileFilter;
    }

    /**
     * @return Returns the {@link #filterWrappers}.
     */
    public List<FilterWrapper> getFilterWrappers()
    {
        return this.filterWrappers;
    }

    /**
     * @param filterWrappers The {@link #filterWrappers} to set.
     */
    public void setFilterWrappers( List<FilterWrapper> filterWrappers )
    {
        this.filterWrappers = filterWrappers;
    }

    /**
     * @return Returns the {@link #filter}.
     */
    public boolean isFilter()
    {
        return this.filter;
    }

    /**
     * @param filter The {@link #filter} to set.
     */
    public void setFilter( boolean filter )
    {
        this.filter = filter;
    }

    /**
     * {@inheritDoc}
     *
     * @throws IOException
     */
    public void resetArchiver()
        throws IOException
    {
        filterWrappers = null;
        filter = false;

        super.resetArchiver();
    }

    /**
     * {@inheritDoc}
     */
    protected void copyFile( ArchiveEntry entry, String vPath )
        throws IOException
    {
        if ( !filter || mavenFileFilter == null )
        {
            super.copyFile( entry, vPath );
        }
        else
        {
            // don't add "" to the archive
            if ( vPath.length() <= 0 )
            {
                return;
            }

            File inFile = ( (PlexusIoFileResource) entry.getResource() ).getFile();
            File outFile = new File( vPath );

            if ( !inFile.isDirectory() )
            {
                try
                {
                    mavenFileFilter.copyFile( inFile, outFile, true, filterWrappers, null );
                    if ( !isIgnorePermissions() )
                    {
                        ArchiveEntryUtils.chmod( outFile, entry.getMode(), getLogger(), isUseJvmChmod() );
                    }
                }
                catch ( MavenFilteringException e )
                {
                    final IOException ioe = new IOException();
                    ioe.initCause( e );

                    throw ioe;
                }
            }
            else
            {
                super.copyFile( entry, vPath );
            }
        }
    }
}
