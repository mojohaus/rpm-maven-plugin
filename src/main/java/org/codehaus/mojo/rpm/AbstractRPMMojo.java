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
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.shared.filtering.MavenFileFilter;
import org.apache.maven.shared.filtering.MavenFilteringException;
import org.apache.maven.shared.filtering.MavenResourcesExecution;
import org.apache.maven.shared.utils.io.FileUtils.FilterWrapper;
import org.codehaus.mojo.rpm.VersionHelper.RPMVersionableMojo;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.Os;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcher;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcherException;

/**
 * Abstract base class for building RPMs.
 *
 * @author Carlos
 * @author Brett Okken, Cerner Corp.
 */
abstract class AbstractRPMMojo
    extends AbstractMojo
    implements RPMVersionableMojo
{

    /**
     * The name portion of the output file name.
     */
    @Parameter( required = true, property = "rpm.name", defaultValue = "${project.artifactId}" )
    private String name;

    /**
     * The version portion of the RPM file name.
     */
    @Parameter( required = true, alias = "version", property = "rpm.version", defaultValue = "${project.version}" )
    private String projversion;

    /**
     * The release portion of the RPM file name.
     * <p>
     * Beginning with 2.0-beta-2, this is an optional parameter. By default, the release will be generated from the
     * modifier portion of the <a href="#projversion">project version</a> using the following rules:
     * <ul>
     * <li>If no modifier exists, the release will be <code>1</code>.</li>
     * <li>If the modifier ends with <i>SNAPSHOT</i>, the timestamp (in UTC) of the build will be appended to end.</li>
     * <li>All instances of <code>'-'</code> in the modifier will be replaced with <code>'_'</code>.</li>
     * <li>If a modifier exists and does not end with <i>SNAPSHOT</i>, <code>"_1"</code> will be appended to end.</li>
     * </ul>
     * </p>
     */
    @Parameter( property = "rpm.release" )
    private String release;

    /**
     * The stage to build. Default to '-bb' but let users specify for instance '-ba' if they want source rpms as well.
     */
    @Parameter( alias = "rpmbuildStage", property = "rpm.rpmbuild.stage", defaultValue = "-bb")
    private String rpmbuildStage;


    /**
     * The target architecture for the rpm. The default value is <i>noarch</i>.
     * <p>
     * For passivity purposes, a value of <code>true</code> or <code>false</code> will indicate whether the <a
     * href="http://plexus.codehaus.org/plexus-utils/apidocs/org/codehaus/plexus/util/Os.html#OS_ARCH">architecture</a>
     * of the build machine will be used. Any other value (such as <tt>x86_64</tt>) will set the architecture of the rpm
     * to <tt>x86_64</tt>.
     * </p>
     * <p>
     * This can also be used in conjunction with <a href="source-params.html#targetArchitecture">Source
     * targetArchitecture</a> to flex the contents of the rpm based on the architecture.
     * </p>
     */

    @Parameter
    private String needarch;

    /**
     * The actual targeted architecture. This will be based on evaluation of {@link #needarch}.
     */
    private String targetArch;

    /**
     * The target os for building the RPM. By default, this will be populated to <a
     * href="http://plexus.codehaus.org/plexus-utils/apidocs/org/codehaus/plexus/util/Os.html#OS_NAME">Os.OS_NAME</a>.
     * <p>
     * This can be used in conjunction with <a href="source-params.html#targetOSName">Source targetOSName</a> to flex
     * the contents of the rpm based on operating system.
     * </p>
     *
     * @since 2.0-beta-3
     */
    @Parameter
    private String targetOS;

    /**
     * The target vendor for building the RPM. By default, this will be populated to the result of <i>rpm -E
     * %{_host_vendor}</i>.
     *
     * @since 2.0-beta-3
     */
    @Parameter
    private String targetVendor;

    /**
     * Set to a key name to sign the package using GPG. If <i>keyPassphrase</i> is not also provided, this will require
     * the input of the passphrase at the terminal.
     */
    @Parameter( property = "gpg.keyname" )
    private String keyname;

    /**
     * The directory from which gpg will load keyrings. If not specified, gpg will use the value configured for its
     * installation, e.g. <code>~/.gnupg</code> or <code>%APPDATA%/gnupg</code>.
     * @since 2.1.2
     */
    @Parameter( property = "gpg.homedir" )
    private File keypath;

    /**
     * The passphrase for the <i>keyname</i> to sign the rpm. This utilizes <a href="http://expect.nist.gov/">expect</a>
     * and requires that {@code expect} be on the PATH.
     * <p>
     * Note that the data type used is <b>NOT</b> {@code String}.
     *
     * <pre>
     * &lt;configuration&gt;
     *     ...
     *     &lt;keyPassphrase&gt;
     *         &lt;passphrase&gt;<i>password</i>&lt;/passphrase&gt;
     *     &lt;/keyPassphrase&gt;
     * &lt;/configuration&gt;
     * </pre>
     *
     * </p>
     * If not given, look up the value under Maven settings using server id at 'keyPassphraseServerId' configuration.
     *
     * @since 2.0-beta-4
     */
    @Parameter
    private Passphrase keyPassphrase;

    /**
     * Server id to lookup the gpg passphrase under Maven settings. The default value intentionally selected to match
     * with maven-gpg-plugin.
     *
     * @Since 2.1.2
     */
    @Parameter( property = "gpg.passphraseServerId", defaultValue = "gpg.passphrase" )
    private String passphraseServerId;

    /**
     * The long description of the package.
     */
    @Parameter( property = "rpm.description", defaultValue = "${project.description}" )
    private String description;

    /**
     * The one-line description of the package.
     */
    @Parameter( property = "rpm.summary", defaultValue = "${project.name}" )
    private String summary;

    /**
     * The one-line license information.
     *
     * @since 2.1-alpha-4
     */
    @Parameter
    private String license;

    /**
     * The distribution containing this package.
     */
    @Parameter
    private String distribution;

    /**
     * The epoch of this package.
     * As defined here: http://www.rpm.org/max-rpm-snapshot/s1-rpm-inside-tags.html
     */
    @Parameter
    private String epoch;

    /**
     * An icon for the package.
     */
    @Parameter
    private File icon;

    /**
     * The vendor supplying the package.
     */
    @Parameter( property = "rpm.vendor", defaultValue = "${project.organization.name}" )
    private String vendor;

    /**
     * A URL for the vendor.
     */
    @Parameter( property = "rpm.url", defaultValue = "${project.organization.url}" )
    private String url;

    /**
     * The package group for the package.
     */
    @Parameter( required = true )
    private String group;

    /**
     * The name of the person or group creating the package.
     */
    @Parameter( property = "rpm.packager", defaultValue = "${project.organization.name}" )
    private String packager;

    /**
     * Automatically add provided shared libraries.
     *
     * @since 2.0-beta-4
     */
    @Parameter( defaultValue = "true" )
    private boolean autoProvides;

    /**
     * Automatically add requirements deduced from included shared libraries.
     *
     * @since 2.0-beta-4
     */
    @Parameter( defaultValue = "true" )
    private boolean autoRequires;

    /**
     * The list of virtual packages provided by this package.
     */
    @Parameter
    private LinkedHashSet<String> provides;

    /**
     * The list of requirements for this package.
     */
    @Parameter
    private LinkedHashSet<String> requires;

    /**
     * The list of build requirements for this package.
     * @since 2.1.6
     */
    @Parameter
    private LinkedHashSet<String> buildRequires;

    /**
     * The list of requirements for running the pre-installation scriptlet.
     * @since 2.1.6
     */
    @Parameter
    private LinkedHashSet<String> requiresPre;

    /**
     * The list of requirements for running the post install scriptlet.
     * @since 2.1.6
     */
    @Parameter
    private LinkedHashSet<String> requiresPost;

    /**
     * The list of requirements for running the pre-removal scriptlet.
     * @since 2.1.6
     */
    @Parameter
    private LinkedHashSet<String> requiresPreun;

    /**
     * The list of prerequisites for this package.
     *
     * @since 2.0-beta-3
     */
    @Parameter
    private LinkedHashSet<String> prereqs;

    /**
     * The list of obsoletes for this package.
     *
     * @since 2.0-beta-3
     */
    @Parameter
    private LinkedHashSet<String> obsoletes;

    /**
     * The list of conflicts for this package.
     */
    @Parameter
    private LinkedHashSet<String> conflicts;

    /**
     * The relocation prefix for this package.
     *
     */
    @Parameter
    private String prefix;

    /**
     * Additional relocation prefixes if needed.
     *
     * @since 2.1-alpha-4
     */
    @Parameter
    private List<String> prefixes;

    /**
     * The area for RPM to use for building the package.<br/>
     * <b>NOTE:</b> The absolute path to the workarea <i>MUST NOT</i> have a space in any of the directory names.
     * <p>
     * Beginning with release 2.0-beta-3, sub-directories will be created within the workarea for each execution of the
     * plugin within a life cycle.<br/>
     * The pattern will be <code>workarea/<i>name[-classifier]</i></code>.<br/>
     * The classifier portion is only applicable for the <a href="attached-rpm-mojo.html">attached-rpm</a> goal.
     * </p>
     */
    @Parameter( defaultValue = "${project.build.directory}/rpm" )
    private File workarea;

    /**
     * The list of file <a href="map-params.html">mappings</a>.
     */
    @Parameter
    private List<Mapping> mappings = Collections.emptyList();

    /**
     * The prepare scriptlet;
     *
     * @since 2.0-beta-4
     */
    @Parameter
    private Scriptlet prepareScriptlet;

    /**
     * The pre-installation scriptlet.
     *
     * @since 2.0-beta-4
     */
    @Parameter
    private Scriptlet preinstallScriptlet;

    /**
     * The post install scriptlet.
     *
     * @since 2.0-beta-4
     */
    @Parameter
    private Scriptlet postinstallScriptlet;

    /**
     * The installation scriptlet.
     *
     * @since 2.0-beta-4
     */
    @Parameter
    private Scriptlet installScriptlet;

    /**
     * The pre-removal scriptlet.
     *
     * @since 2.0-beta-4
     */
    @Parameter
    private Scriptlet preremoveScriptlet;

    /**
     * The post-removal scriptlet.
     *
     * @since 2.0-beta-4
     */
    @Parameter
    private Scriptlet postremoveScriptlet;

    /**
     * The verify scriptlet.
     *
     * @since 2.0-beta-4
     */
    @Parameter
    private Scriptlet verifyScriptlet;

    /**
     * The clean scriptlet.
     *
     * @since 2.0-beta-4
     */
    @Parameter
    private Scriptlet cleanScriptlet;

    /**
     * The pretrans scriptlet.
     *
     * @since 2.0-beta-4
     */
    @Parameter
    private Scriptlet pretransScriptlet;

    /**
     * The posttrans script.
     *
     * @since 2.0-beta-4
     */
    @Parameter
    private Scriptlet posttransScriptlet;

    /**
     * The list of triggers to take place on installation of other packages.
     *
     * <pre>
     *  &lt;triggers>
     *      &lt;installTrigger>
     *          &lt;subpackage>optional&lt;/subpackage>
     *          &lt;program>program to execute (if not shell) optional&lt;/program>
     *          &lt;script>actual contents of script - optional&lt;/script>
     *          &lt;scriptFile>location of file containing script - optional&lt;/script>
     *          &lt;fileEncoding>character encoding for script file - recommended&lt;/fileEncoding>
     *          &lt;triggers>
     *              &lt;trigger>package/version to trigger on (i.e. jre > 1.5)&lt;/trigger>
     *              ...
     *          &lt;/triggers>
     *      &lt;/installTrigger>
     *      &lt;removeTrigger>
     *          ...
     *      &lt;/removeTrigger>
     *      &lt;postRemoveTrigger>
     *          ...
     *      &lt;/postRemoveTrigger>
     *      ...
     *  &lt;/triggers>
     * </pre>
     *
     * @since 2.0-beta-4
     * @see BaseTrigger
     */
    @Parameter
    private List<BaseTrigger> triggers;

    /**
     * Filters (property files) to include during the interpolation of the pom.xml.
     *
     * @since 2.0
     */
    @Parameter
    private List<String> filters;

    /**
     * Expression preceded with the String won't be interpolated \${foo} will be replaced with ${foo}
     *
     * @since 2.0
     */
    @Parameter( property = "maven.rpm.escapeString" )
    private String escapeString;

    /**
     * @since 2.0
     */
    @Parameter( defaultValue = "${session}", readonly = true, required = true )
    private MavenSession session;

    /**
     * @since 2.1-alpha-4
     */
    @Parameter( required = true, readonly = true, property = "project.build.sourceEncoding" )
    private String sourceEncoding;


    /**
     * The primary project artifact.
     */
    @Parameter( required = true, readonly = true, property = "project.artifact" )
    private Artifact artifact;

    /**
     * Auxillary project artifacts.
     */
    @Parameter( required = true, readonly = true, property = "project.attachedArtifacts" )
    private List<Artifact> attachedArtifacts;

    @Parameter( defaultValue = "${project}", readonly = true, required = true )
    protected MavenProject project;

    /**
     * Should <i>brp-repack-jars</i> be used in the RPM build. Defaults to <code>false</code>. If it is
     * <code>false</code> <i>brp-repack-jars</i> will be disabled by:<br/>
     * <code>%define __jar_repack 0</code> This will have no effect on RHEL5 or earlier release.
     *
     * @since 2.1-alpha-4
     */
    @Parameter
    private boolean repackJars = false;

    /**
     * A list of <code>%define</code> arguments
     */
    @Parameter
    private List<String> defineStatements;

    /**
     * The default file mode (octal string) to assign to files when installed. <br/>
     * Only applicable to a <a href="map-params.html">Mapping</a> if <a href="map-params.html#filemode">filemode</a>, <a
     * href="map-params.html#username">username</a>, AND <a href="map-params.html#groupname">groupname</a> are
     * <b>NOT</b> populated.
     *
     * @since 2.0-beta-2
     */
    @Parameter
    private String defaultFilemode;

    /**
     * The default directory mode (octal string) to assign to directories when installed.<br/>
     * Only applicable to a <a href="map-params.html">Mapping</a> if <a href="map-params.html#filemode">filemode</a>, <a
     * href="map-params.html#username">username</a>, AND <a href="map-params.html#groupname">groupname</a> are
     * <b>NOT</b> populated.
     *
     * @since 2.0-beta-2
     */
    @Parameter
    private String defaultDirmode;

    /**
     * The default user name for files when installed.<br/>
     * Only applicable to a <a href="map-params.html">Mapping</a> if <a href="map-params.html#filemode">filemode</a>, <a
     * href="map-params.html#username">username</a>, AND <a href="map-params.html#groupname">groupname</a> are
     * <b>NOT</b> populated.
     *
     * @since 2.0-beta-2
     */
    @Parameter
    private String defaultUsername;

    /**
     * The default group name for files when installed.<br/>
     * Only applicable to a <a href="map-params.html">Mapping</a> if <a href="map-params.html#filemode">filemode</a>, <a
     * href="map-params.html#username">username</a>, AND <a href="map-params.html#groupname">groupname</a> are
     * <b>NOT</b> populated.
     *
     * @since 2.0-beta-2
     */
    @Parameter
    private String defaultGroupname;

    /**
     * Indicates if the execution should be disabled. If <code>true</code>, nothing will occur during execution.
     *
     * @since 2.0
     */
    @Parameter
    private boolean disabled;

    /**
     * The system property to read the calculated version from, normally set by the version mojo.
     *
     * @since 2.1-alpha-2
     */
    @Parameter( required = true, defaultValue = "rpm.version" )
    private String versionProperty;

    /**
     * The system property to read the calculated release from, normally set by the version mojo.
     *
     * @since 2.1-alpha-2
     */
    @Parameter( required = true, defaultValue = "rpm.release" )
    private String releaseProperty;

    /**
     * The changelog file. If the file does not exist, it is ignored.
     *
     * @since 2.0-beta-3
     */
    @Parameter
    private File changelogFile;

    /**
     * Option to copy the created RPM to another location
     * @since 2.1.
     */
    @Parameter(property="rpm.copyTo")
    private File copyTo;

    //////////////////////////////////////////////////////////////////////////

    /**
     * @since 2.0
     */
    @Component( role = org.apache.maven.shared.filtering.MavenFileFilter.class, hint = "default" )
    private MavenFileFilter mavenFileFilter;


    /**
     * Maven Security Dispatcher
     *
     * @since 2.1.2
     */
    @Component( hint = "mng-4384" )
    private SecDispatcher securityDispatcher;

    /**
     * Current user system settings for use in Maven.
     *
     * @since 2.1.2
     */
    @Parameter( defaultValue = "${settings}", readonly = true )
    private Settings settings;

    /**
     * Indicates if the execution should be disabled for POM projects. If <code>true</code>, nothing will happen during
     * execution in projects with packaging "pom".
     * @since 2.1.6
     */
    @Parameter(defaultValue = "false")
    private boolean skipPOMs;

    //////////////////////////////////////////////////////////////////////////

    /**
     * Maintains a mapping of macro keys to their values (either {@link RPMHelper#evaluateMacro(String) evaluated} or
     * set via {@link #defineStatements}.
     *
     * @since 2.1-alpha-1
     */
    private final Map<String, String> macroKeyToValue = new HashMap<String, String>();

    /**
     * The key of the map is the directory where the files should be linked to. The value is the {@code List} of
     * {@link SoftlinkSource}s to be linked to.
     *
     * @since 2.0-beta-3
     */
    private final Map<String, List<SoftlinkSource>> linkTargetToSources =
        new LinkedHashMap<String, List<SoftlinkSource>>();

    /** The root of the build area prior to calling rpmbuild. */
    private File buildroot;

    /** The root of the build area as used by rpmbuild. */
    private File rpmBuildroot;

    /** The changelog string. */
    private String changelog;

    /**
     * This is not set until {@link #execute() is called}.
     *
     * @since 2.1-alpha-1
     */
    private RPMHelper helper;

    /**
     * The {@link FileUtils.FilterWrapper filter wrappers} to use for file filtering.
     *
     * @since 2.0
     * @see #mavenFileFilter
     */
    private List<FilterWrapper> defaultFilterWrappers;


    // // // Mojo methods/////////////////////////////////////////////////////

    /** {@inheritDoc} */
    public final void execute()
        throws MojoExecutionException, MojoFailureException
    {
        if ( skipPOMs && isPOM() )
        {
            getLog().info("skipping because artifact is a pom (skipPOMs)");
            return;
        }

        if ( disabled )
        {
            getLog().info( "MOJO is disabled. Doing nothing." );

            // FIXME, is it a correct way getting install/deploy to ignore the original primary 'rpm' artifact?
            if ( "rpm".equals( this.project.getPackaging() ) )
            {
                this.project.setPackaging( "pom" );
            }

            return;
        }

        if ( this.prefix != null )
        {
            if ( this.prefixes == null )
            {
                this.prefixes = new ArrayList<String>();
            }
            this.prefixes.add(prefix);
        }

        helper = new RPMHelper( this );

        checkParams( helper );

        final String classifier = getClassifier();

        if ( classifier != null )
        {
            workarea = new File( workarea, name + '-' + classifier );
        }
        else
        {
            workarea = new File( workarea, name );
        }

        buildWorkArea();

        // set up the maven file filter and FilteringDirectoryArchiver
        setDefaultWrappers();
        final FilteringDirectoryArchiver copier = new FilteringDirectoryArchiver();
        copier.setMavenFileFilter( mavenFileFilter );
        new FileHelper( this, copier ).installFiles();

        writeSpecFile();

        this.loadGpgPassphrase();

        helper.buildPackage();

        afterExecution();

        if ( this.copyTo != null ) {
        	makeSecondCopy();
        }
    }

    /**
     * @return The Maven project used by this MOJO
     */
    private MavenProject getProject() {
        if (project.getExecutionProject() != null) {
            return project.getExecutionProject();
        }

        return project;
    }

    /**
     * @return Whether the artifact is a POM or not
     */
    private boolean isPOM() {
        return "pom".equalsIgnoreCase(getProject().getArtifact().getType());
    }

    private void makeSecondCopy() throws MojoFailureException {
    	try {
    		this.getLog().info( "Copy " + this.getRPMFile() + " to " + copyTo );
    	    FileUtils.copyFile( this.getRPMFile(), copyTo );
    	}
    	catch ( IOException e ) {
    		throw new MojoFailureException( "Unable to copy file" );
    	}
    }

    /**
     * Will be called on completion of {@link #execute()}. Provides subclasses an opportunity to perform any post
     * execution logic (such as attaching an artifact).
     *
     * @throws MojoExecutionException If an error occurs.
     * @throws MojoFailureException If failure occurs.
     */
    protected void afterExecution()
        throws MojoExecutionException, MojoFailureException
    {

    }

    /**
     * Provides an opportunity for subclasses to provide an additional classifier for the rpm workarea.<br/>
     * By default this implementation returns {@code null}, which indicates that no additional classifier should be
     * used.
     *
     * @return An additional classifier to use for the rpm workarea or {@code null} if no additional classifier should
     *         be used.
     */
    String getClassifier()
    {
        return null;
    }

    /**
     * Returns the generated rpm {@link File}.
     *
     * @return The generated rpm <tt>File</tt>.
     */
    protected File getRPMFile()
    {
        File rpms = new File( workarea, "RPMS" );
        File archDir = new File( rpms, targetArch );

        return new File( archDir, name + '-' + projversion + '-' + release + '.' + targetArch + ".rpm" );
    }

    /**
     * @throws MojoExecutionException
     */
    private void setDefaultWrappers()
        throws MojoExecutionException
    {
        final MavenResourcesExecution mavenResourcesExecution = new MavenResourcesExecution();
        mavenResourcesExecution.setEscapeString( escapeString );

        try
        {
            defaultFilterWrappers =
                mavenFileFilter.getDefaultFilterWrappers( project, filters, false, this.session,
                                                          mavenResourcesExecution );
        }
        catch ( MavenFilteringException e )
        {
            getLog().error( "fail to build filering wrappers " + e.getMessage() );
            throw new MojoExecutionException( e.getMessage(), e );
        }
    }

    /**
     * Build the structure of the work area.
     *
     * @throws MojoFailureException if a directory cannot be built
     * @throws MojoExecutionException if buildroot cannot be cleared (if exists)
     */
    private void buildWorkArea()
        throws MojoFailureException, MojoExecutionException
    {
        final String[] topdirs = { "BUILD", "RPMS", "SOURCES", "SPECS", "SRPMS", "tmp-buildroot", "buildroot" };

        // Build the top directory
        if ( !workarea.exists() )
        {
            getLog().info( "Creating directory " + workarea.getAbsolutePath() );
            if ( !workarea.mkdirs() )
            {
                throw new MojoFailureException( "Unable to create directory " + workarea.getAbsolutePath() );
            }
        }

        validateWorkarea();

        // Build each directory in the top directory
        for ( String topdir : topdirs )
        {
            File dir = new File( workarea, topdir );
            if ( dir.exists() )
            {
                getLog().info( "Directory " + dir.getAbsolutePath() + " already exists. Deleting all contents." );

                try
                {
                    FileUtils.cleanDirectory( dir );
                }
                catch ( IOException e )
                {
                    throw new MojoExecutionException( "Unable to clear directory: " + dir.getName(), e );
                }
            }
            else
            {
                getLog().info( "Creating directory " + dir.getAbsolutePath() );
                if ( !dir.mkdir() )
                {
                    throw new MojoFailureException( "Unable to create directory " + dir.getAbsolutePath() );
                }
            }
        }

        // set build root variable
        buildroot = new File( workarea, "tmp-buildroot" );
        rpmBuildroot = new File( workarea, "buildroot" );
    }

    /**
     * Check the parameters for validity.
     *
     * @throws MojoFailureException if an invalid parameter is found
     * @throws MojoExecutionException if an error occurs reading a script
     */
    private void checkParams( RPMHelper helper )
        throws MojoExecutionException, MojoFailureException
    {
        Log log = getLog();

        // Retrieve any versions set by the VersionMojo
	if ( versionProperty != null ) {
	    String projversion = this.project.getProperties().getProperty( versionProperty );
	    if ( projversion != null )
		{
		    this.projversion = projversion;
		}
	}
	if ( releaseProperty != null ) {
	    String release = this.project.getProperties().getProperty( releaseProperty );
	    if ( release != null )
		{
		    this.release = release;
		}
	}

        // calculate versions if neccessary, check for existing maven modifier and split them accordingly
        if ( this.projversion == null || this.release == null || this.projversion.contains( "-" ) )
        { // including -SNAPSHOT and 1-34
            final VersionHelper.Version version = new VersionHelper( this ).calculateVersion();
            this.projversion = version.version;
            this.release = version.release;
        }

        log.debug( "project version = " + this.projversion );
        log.debug( "project release = " + this.release );

        // evaluate needarch and populate targetArch
        if ( needarch == null || needarch.length() == 0 || "false".equalsIgnoreCase( needarch ) )
        {
            targetArch = "noarch";
        }
        else if ( "true".equalsIgnoreCase( needarch ) )
        {
            targetArch = helper.getArch();
        }
        else
        {
            targetArch = needarch;
        }
        log.debug( "targetArch = " + targetArch );

        // provide default targetOS if value not given
        if ( targetOS == null || targetOS.length() == 0 )
        {
            targetOS = Os.OS_NAME;
        }
        log.debug( "targetOS = " + targetOS );

        if ( targetVendor == null || targetVendor.length() == 0 )
        {
            targetVendor = helper.getHostVendor();
        }
        log.debug( "targetVendor = " + targetVendor );

        // Various checks in the mappings
        for ( Mapping map : mappings )
        {
            if ( map.getDirectory() == null )
            {
                throw new MojoFailureException( "<mapping> element must contain the destination directory" );
            }
            if ( map.getSources() != null )
            {
                for ( Source src : map.getSources() )
                {
                    if ( src.getLocation() == null )
                    {
                        throw new MojoFailureException( "<mapping><source> tag must contain the source directory" );
                    }
                }
            }
        }

        if ( ( changelog == null ) && ( changelogFile != null ) )
        {
            if ( !changelogFile.exists() )
            {
                log.debug( changelogFile.getAbsolutePath() + " does not exist - ignoring" );
            }
            else
            {
                try
                {
                    StringBuilder sb = new StringBuilder();
                    BufferedReader br = new BufferedReader( new FileReader( changelogFile ) );
                    while ( br.ready() )
                    {
                        String line = br.readLine();
                        sb.append( line );
                        sb.append( '\n' );
                    }
                    br.close();
                    changelog = sb.toString();
                }
                catch ( Throwable t )
                {
                    throw new MojoExecutionException( "Unable to read " + changelogFile.getAbsolutePath(), t );
                }
            }
        }

        // generate license text if not set
        if ( license == null )
        {
            license = generateDefaultCopyrightText();
        }

        // if this package obsoletes any packages, make sure those packages are added to the provides list
        if ( obsoletes != null && "true".equals( System.getProperty( "disable.mrpm24" ) ) )
        {
            //this block is incorrectly implemented, however we want to provide capability
            // to enable this if needed in the next few releases, after that remove it completely
            if ( provides == null )
            {
                provides = obsoletes;
            }
            else
            {
                provides.addAll( obsoletes );
            }
        }

        if ( !repackJars )
        {
            if ( defineStatements == null )
            {
                defineStatements = new ArrayList<String>();
            }

            defineStatements.add( "__jar_repack 0" );
        }

        processDefineStatements();
    }

    /**
     * Put all name/value pairs in {@link #defineStatements} in {@link #macroKeyToValue}.
     *
     * @since 2.1-alpha-1
     */
    private void processDefineStatements()
    {
        if ( defineStatements == null )
        {
            return;
        }
        for ( String define : defineStatements )
        {
            String[] parts = define.split( " " );
            if ( parts.length == 2 )
            {
                macroKeyToValue.put( parts[0], parts[1] );
            }
        }
    }

    /**
     * Validate that {@link #workarea} is a {@link File#isDirectory() directory} and that the
     * {@link File#getAbsolutePath()} does not contain any spaces.
     *
     * @throws MojoExecutionException
     */
    private void validateWorkarea()
        throws MojoExecutionException
    {
        if ( !workarea.isDirectory() )
        {
            throw new MojoExecutionException( workarea + " is not a directory" );
        }

        if ( workarea.getAbsolutePath().trim().indexOf( " " ) != -1 )
        {
            throw new MojoExecutionException( workarea + " contains a space in path" );
        }
    }

    /**
     * Determines the actual value for the <i>macro</i>. Will check both {@link #defineStatements} and
     * {@link RPMHelper#evaluateMacro(String)}.
     *
     * @param macro The macro to evaluate.
     * @return The literal value or name of macro if it has no value.
     * @throws MojoExecutionException
     * @since 2.1-alpha-1
     */
    String evaluateMacro( String macro )
        throws MojoExecutionException
    {
        if ( macroKeyToValue.containsKey( macro ) )
        {
            return macroKeyToValue.get( macro );
        }

        final String value = helper.evaluateMacro( macro );
        macroKeyToValue.put( macro, value );

        return value;
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
            PrintWriter spec = new UnixPrintWriter( new FileWriter( specf ) );
            try
            {
                new SpecWriter( this, spec ).writeSpecFile();
            }
            finally
            {
                spec.close();
            }
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Unable to write " + specf.getAbsolutePath(), e );
        }
    }

    /**
     * Generates a default copyright text from {@link MavenProject#getOrganization()} and
     * {@link MavenProject#getInceptionYear()}.
     *
     * @return Generated copyright text from the organization name and inception year.
     */
    private String generateDefaultCopyrightText()
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
            copyrightText = year == null ? organization : year;
        }
        return "(c) " + copyrightText;
    }

    /**
     * @return Returns the {@link #linkTargetToSources}.
     */
    final Map<String, List<SoftlinkSource>> getLinkTargetToSources()
    {
        return this.linkTargetToSources;
    }

    /**
     * @return Returns the {@link #name}.
     */
    final String getName()
    {
        return this.name;
    }

    /**
     * @return Returns the {@link #release}.
     */
    public final String getRelease()
    {
        return this.release;
    }

    public Date getBuildTimestamp()
    {
        return ( session == null ) ? new Date() : session.getStartTime();
    }

    /**
     * @return Returns the {@link #description}.
     */
    final String getDescription()
    {
        return this.description;
    }

    /**
     * @return Returns the {@link #summary}.
     */
    final String getSummary()
    {
        return this.summary;
    }

    /**
     * @return Returns the {@link #license}.
     */
    final String getLicense()
    {
        return this.license;
    }

    /**
     * @return Returns the {@link #epoch}.
     */
    final String getEpoch()
    {
        return this.epoch;
    }


    /**
     * @return Returns the {@link #distribution}.
     */
    final String getDistribution()
    {
        return this.distribution;
    }

    /**
     * @return Returns the {@link #icon}.
     */
    final File getIcon()
    {
        return this.icon;
    }

    /**
     * @return Returns the {@link #vendor}.
     */
    final String getVendor()
    {
        return this.vendor;
    }

    /**
     * @return Returns the {@link #url}.
     */
    final String getUrl()
    {
        return this.url;
    }

    /**
     * @return Returns the {@link #group}.
     */
    final String getGroup()
    {
        return this.group;
    }

    /**
     * @return Returns the {@link #packager}.
     */
    final String getPackager()
    {
        return this.packager;
    }

    /**
     * @return Returns the {@link #autoProvides}.
     */
    final boolean isAutoProvides()
    {
        return this.autoProvides;
    }

    /**
     * @return Returns the {@link #autoRequires}.
     */
    final boolean isAutoRequires()
    {
        return this.autoRequires;
    }

    /**
     * @return Returns the {@link #provides}.
     */
    final LinkedHashSet<String> getProvides()
    {
        return this.provides;
    }

    /**
     * @return Returns the {@link #requires}.
     */
    final LinkedHashSet<String> getRequires()
    {
        return this.requires;
    }

    /**
     * @return Returns the {@link #buildRequires}.
     */
    final LinkedHashSet<String> getBuildRequires()
    {
        return this.buildRequires;
    }
    
    /**
     * @return Returns the {@link #requiresPre}.
     */
    final LinkedHashSet<String> getRequiresPre()
    {
        return this.requiresPre;
    }

    /**
     * @return Returns the {@link #requiresPreun}.
     */
    final LinkedHashSet<String> getRequiresPreun()
    {
        return this.requiresPreun;
    }

    /**
     * @return Returns the {@link #requiresPost}.
     */
    final LinkedHashSet<String> getRequiresPost()
    {
        return this.requiresPost;
    }

    /**
     * @return Returns the {@link #prereqs}.
     */
    final LinkedHashSet<String> getPrereqs()
    {
        return this.prereqs;
    }

    /**
     * @return Returns the {@link #obsoletes}.
     */
    final LinkedHashSet<String> getObsoletes()
    {
        return this.obsoletes;
    }

    /**
     * @return Returns the {@link #conflicts}.
     */
    final LinkedHashSet<String> getConflicts()
    {
        return this.conflicts;
    }

    final List<String> getPrefixes()
    {
        return prefixes;
    }

    /**
     * @return Returns the {@link #mappings}.
     */
    final List<Mapping> getMappings()
    {
        return this.mappings;
    }

    /**
     * @return Returns the {@link #prepareScriptlet}.
     */
    final Scriptlet getPrepareScriptlet()
    {
        return this.prepareScriptlet;
    }

    /**
     * @return Returns the {@link #preinstallScriptlet}.
     */
    final Scriptlet getPreinstallScriptlet()
    {
        return this.preinstallScriptlet;
    }

    /**
     * @return Returns the {@link #postinstallScriptlet}.
     */
    final Scriptlet getPostinstallScriptlet()
    {
        return this.postinstallScriptlet;
    }

    /**
     * @return Returns the {@link #installScriptlet}.
     */
    final Scriptlet getInstallScriptlet()
    {
        return this.installScriptlet;
    }

    /**
     * @return Returns the {@link #preremoveScriptlet}.
     */
    final Scriptlet getPreremoveScriptlet()
    {
        return this.preremoveScriptlet;
    }

    /**
     * @return Returns the {@link #postremoveScriptlet}.
     */
    final Scriptlet getPostremoveScriptlet()
    {
        return this.postremoveScriptlet;
    }

    /**
     * @return Returns the {@link #verifyScriptlet}.
     */
    final Scriptlet getVerifyScriptlet()
    {
        return this.verifyScriptlet;
    }

    /**
     * @return Returns the {@link #cleanScriptlet}.
     */
    final Scriptlet getCleanScriptlet()
    {
        return this.cleanScriptlet;
    }

    /**
     * @return Returns the {@link #pretransScriptlet}.
     */
    final Scriptlet getPretransScriptlet()
    {
        return this.pretransScriptlet;
    }

    /**
     * @return Returns the {@link #posttransScriptlet}.
     */
    final Scriptlet getPosttransScriptlet()
    {
        return this.posttransScriptlet;
    }

    /**
     * @return Returns the {@link #triggers}.
     */
    final List<BaseTrigger> getTriggers()
    {
        return this.triggers;
    }

    /**
     * @return Returns the {@link #defineStatements}.
     */
    final List<String> getDefineStatements()
    {
        return this.defineStatements;
    }

    /**
     * @return Returns the {@link #defaultFilemode}.
     */
    final String getDefaultFilemode()
    {
        return this.defaultFilemode;
    }

    /**
     * @return Returns the {@link #defaultDirmode}.
     */
    final String getDefaultDirmode()
    {
        return this.defaultDirmode;
    }

    /**
     * @return Returns the {@link #defaultUsername}.
     */
    final String getDefaultUsername()
    {
        return this.defaultUsername;
    }

    /**
     * @return Returns the {@link #defaultGroupname}.
     */
    final String getDefaultGroupname()
    {
        return this.defaultGroupname;
    }

    /**
     * @return Returns the {@link #buildroot}.
     */
    final File getBuildroot()
    {
        return this.buildroot;
    }

    /**
     * @return Returns the {@link #rpmBuildroot}.
     */
    final File getRPMBuildroot()
    {
        return this.rpmBuildroot;
    }

    /**
     * @inheritDoc
     */
    public final String getVersion()
    {
        return this.projversion;
    }

    /**
     * @return Returns the {@link #changelog}.
     */
    final String getChangelog()
    {
        return this.changelog;
    }

    /**
     * @return Returns the {@link #targetArch}.
     */
    final String getTargetArch()
    {
        return this.targetArch;
    }

    /**
     * @return Returns the {@link #targetOS}.
     */
    final String getTargetOS()
    {
        return this.targetOS;
    }

    /**
     * @return Returns the {@link #targetVendor}.
     */
    final String getTargetVendor()
    {
        return this.targetVendor;
    }

    /**
     * @return Returns the {@link #keypath}.
     */
    final File getKeypath()
    {
        return this.keypath;
    }
    /**
     * @return Returns the {@link #keyname}.
     */
    final String getKeyname()
    {
        return this.keyname;
    }

    /**
     * @return Returns the {@link #keyPassphrase}.
     */
    final Passphrase getKeyPassphrase()
    {
        return this.keyPassphrase;
    }

    /**
     * @return Returns the {@link #workarea}.
     */
    final File getWorkarea()
    {
        return this.workarea;
    }

    /**
     * @return Returns the {@link #artifact}.
     */
    final Artifact getArtifact()
    {
        return this.artifact;
    }

    /**
     * @return Returns the {@link #attachedArtifacts}.
     */
    final List<Artifact> getAttachedArtifacts()
    {
        return this.attachedArtifacts;
    }

    /**
     * Returns the {@link FileUtils.FilterWrapper wrappers} to use for filtering resources.
     *
     * @return Returns the {@code FilterWrapper}s to use for filtering resources.
     */
    final List<FilterWrapper> getFilterWrappers()
    {
        return this.defaultFilterWrappers;
    }

    /**
     * @return the rpmbuildStage
     */
    final public String getRpmbuildStage()
    {
        return rpmbuildStage;
    }

    /**
     * @param rpmRpmbuildStage the rpmRpmbuildStage to set
     */
    final public void setRpmbuildStage( String rpmbuildStage )
    {
        this.rpmbuildStage = rpmbuildStage;
    }

    /**
     * Load and decrypt gpg passphrase from maven settings if not given from plugin configuration
     *
     * @throws MojoFailureException
     */
    private void loadGpgPassphrase()
        throws MojoFailureException
    {
        if ( this.keyPassphrase == null && passphraseServerId != null )
        {
            Server server = this.settings.getServer( passphraseServerId );

            if ( server != null )
            {
                if ( server.getPassphrase() != null )
                {
                    try
                    {
                        this.keyPassphrase = new Passphrase();
                        this.keyPassphrase.setPassphrase( securityDispatcher.decrypt( server.getPassphrase() ) );
                    }
                    catch ( SecDispatcherException e )
                    {
                        throw new MojoFailureException( "Unable to decrypt gpg password", e );
                    }
                }
            }
        }
    }
}
