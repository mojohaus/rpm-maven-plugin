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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.archiver.dir.DirectoryArchiver;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.StreamConsumer;


/**
 * Construct the RPM file
 * @version $Id$
 * @goal rpm
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
    private String version;
    
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
     * The long description of the package.
     * @parameter expression="${project.description}"
     * @readonly
     */
    private String description;
    
    /**
     * The one-line description of the package.
     * @parameter expression="${project.name}"
     * @readonly
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
     * @readonly
     */
    private String vendor;
    
    /**
     * A URL for the vendor.
     * @parameter expression="${project.organization.url}"
     * @readonly
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
    private File buildroot;
    
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
     * @component role="org.codehaus.plexus.archiver.Archiver"
     *            roleHint="dir"
     */
    private DirectoryArchiver copier;
    
    // // //  Consumers for rpmbuild output
    
    private class StdoutConsumer  implements StreamConsumer
    {
        private Log logger;
        
        public StdoutConsumer(Log logger)
        {
            this.logger = logger;
        }
        
        public void consumeLine(String string)
        {
            logger.info(string);
        }
    }
    
    private class StderrConsumer  implements StreamConsumer
    {
        private Log logger;
        
        public StderrConsumer(Log logger)
        {
            this.logger = logger;
        }
        
        public void consumeLine(String string)
        {
            logger.warn(string);
        }
    }
    
    // // //  Mojo methods
    
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        checkParams();
        buildWorkArea();
        writeSpecFile();
        installFiles();
        buildPackage();
    }
    
    // // //  Internal methods
    
    private void buildPackage() throws MojoExecutionException, MojoFailureException
    {
        File f = new File(workarea, "SPECS");
        
        Commandline cl = new Commandline();
        cl.setExecutable("rpmbuild");
        cl.setWorkingDirectory(f.getAbsolutePath());
        cl.createArgument().setValue("-bb");
        cl.createArgument().setValue("--define");
        cl.createArgument().setValue("_topdir " + workarea.getAbsolutePath());
        if (!needarch)
        {
            cl.createArgument().setValue("--target");
            cl.createArgument().setValue("noarch");
        }
        cl.createArgument().setValue(name + ".spec");
        
        StreamConsumer stdout = new StdoutConsumer(getLog());
        StreamConsumer stderr = new StderrConsumer(getLog());
        try
        {
            int result = CommandLineUtils.executeCommandLine(cl, stdout, stderr);
            if ( result != 0 )
            {
                throw new MojoExecutionException( "RPM build execution returned: \'" + result + "\'." );
            }
        }
        catch ( CommandLineException e )
        {
            throw new MojoExecutionException( "Unable to build the RPM", e );
        }
    }
    
    private void buildWorkArea() throws MojoExecutionException, MojoFailureException
    {
        final String[] topdirs = { "BUILD", "RPMS", "SOURCES", "SPECS", "SRPMS" };
        
        // Build the top directory
        if (!workarea.exists())
        {
            getLog().info("Creating directory " + workarea.getAbsolutePath());
            if (!workarea.mkdirs())
            {
                throw new MojoFailureException("Unable to create directory " + workarea.getAbsolutePath());
            }
        }
        
        // Build each directory in the top directory
        for (int i = 0; i < topdirs.length; i++)
        {
            File d = new File(workarea, topdirs[i]);
            if (!d.exists())
            {
                getLog().info("Creating directory " + d.getAbsolutePath());
                if (!d.mkdir())
                {
                    throw new MojoFailureException("Unable to create directory " + d.getAbsolutePath());
                }
            }
        }
        
        // Build the build root
        buildroot = new File(workarea, "buildroot");
        if (!buildroot.exists())
        {
            getLog().info("Creating directory " + buildroot.getAbsolutePath());
            if (!buildroot.mkdir())
            {
                throw new MojoFailureException("Unable to create directory " + buildroot.getAbsolutePath());
            }
        }
    }
    
    private void checkParams() throws MojoExecutionException, MojoFailureException
    {
        // Check the version string
        if (projversion.indexOf("-") == -1)
        {
            version = projversion;
        }
        else
        {
            version = projversion.substring(0, projversion.indexOf("-"));
            getLog().warn("Version string truncated to " + version);
        }
        
        // Various checks in the mappings
        for (Iterator it = mappings.iterator(); it.hasNext();)
        {
            Mapping map = (Mapping) it.next();
            if (map.getDirectory() == null)
            {
                throw new MojoFailureException("<mapping> element must contain the destination directory");
            }
            if (map.getSources() != null)
            {
                for (Iterator sit = map.getSources().iterator(); sit.hasNext();)
                {
                    Source src = (Source) sit.next();
                    if (src.getLocation() == null)
                    {
                        throw new MojoFailureException("<mapping><source> element must contain the source directory");
                    }
                }
            }
        }
        
        // Collect the scripts, if necessary
        if ((preinstall == null) && (preinstallScript != null))
        {
            preinstall = readFile(preinstallScript);
        }
        if ((postinstall == null) && (postinstallScript != null))
        {
            postinstall = readFile(postinstallScript);
        }
        if ((preremove == null) && (preremoveScript != null))
        {
            preremove = readFile(preremoveScript);
        }
        if ((postremove == null) && (postremoveScript != null))
        {
            postremove = readFile(postremoveScript);
        }
        if ((verify == null) && (verifyScript != null))
        {
            verify = readFile(verifyScript);
        }
    }
    
    private void copySource(File src, File dest, List incl, List excl) throws MojoExecutionException
    {
        try
        {
            // Set the destination
            copier.setDestFile(dest);

            // Set the source
            if (src.isDirectory())
            {
                String[] ia = null;
                if (incl != null)
                {
                    ia = (String[])incl.toArray(new String[0]);
                }
                
                String[] ea = null;
                if (excl != null)
                {
                    ea = (String[])excl.toArray(new String[0]);
                }
                
                copier.addDirectory(src, "", ia, ea);
            }
            else
            {
                copier.addFile(src, src.getName());
            }

            // Perform the copy
            copier.createArchive();

            // Clear the list for the next mapping
            copier.resetArchiver();
        }
        catch (Throwable t)
        {
            throw new MojoExecutionException("Unable to copy files for packaging: " + t.getMessage(), t);
        }
    }
    
    private void installFiles() throws MojoExecutionException
    {
        // Copy icon, if specified
        if (icon != null)
        {
            File icondest = new File(workarea, "SOURCES");
            copySource(icon, icondest, null, null);
        }
        
        // Process each mapping
        for (Iterator it = mappings.iterator(); it.hasNext();)
        {
            Mapping map = (Mapping) it.next();
            File dest = new File(buildroot + map.getDestination());
            
            List srcs = map.getSources();
            if ((srcs == null) || (srcs.size() == 0))
            {
                // Build the output directory
                if (!dest.mkdirs())
                {
                    throw new MojoExecutionException("Unable to create " + dest.getAbsolutePath());
                }
            }
            else
            {
                for (Iterator sit = srcs.iterator(); sit.hasNext();)
                {
                    Source src = (Source) sit.next();
                    if (src.getLocation().exists())
                    {
                        copySource(src.getLocation(), dest, src.getIncludes(), src.getExcludes());
                    }
                    else
                    {
                        throw new MojoExecutionException("Source location " + src.getLocation() + " does not exist");
                    }
                }
            }
        }
    }
    
    private String readFile(File in) throws MojoExecutionException
    {
        try
        {
            StringBuffer sb = new StringBuffer();
            BufferedReader br = new BufferedReader(new FileReader(in));
            while (br.ready())
            {
                String line = br.readLine();
                sb.append(line + "\n");
            }
            br.close();
            return sb.toString();
        }
        catch (Throwable t)
        {
            throw new MojoExecutionException("Unable to read " + in.getAbsolutePath(), t);
        }
    }
    
    private void writeSpecFile() throws MojoExecutionException
    {
        File f = new File(workarea, "SPECS");
        File specf = new File(f, name + ".spec");
        try
        {
            getLog().info("Creating spec file " + specf.getAbsolutePath());
            PrintWriter spec = new PrintWriter(new FileWriter(specf));
            
            spec.println("Name: " + name);
            spec.println("Version: " + version);
            spec.println("Release: " + release);
            if (summary != null)
            {
                spec.println("Summary: " + summary);
            }
            if (copyright != null)
            {
                spec.println("License: " + copyright);
            }
            if (distribution != null)
            {
                spec.println("Distribution: " + distribution);
            }
            if (icon != null)
            {
                spec.println("Icon: " + icon.getName());
            }
            if (vendor != null)
            {
                spec.println("Vendor: " + vendor);
            }
            if (url != null)
            {
                spec.println("URL: " + url);
            }
            if (group != null)
            {
                spec.println("Group: " + group);
            }
            if (packager != null)
            {
                spec.println("Packager: " + packager);
            }
            if (provides != null)
            {
                for (Iterator it = provides.iterator(); it.hasNext();)
                {
                    spec.println("Provides: " + it.next());
                }
            }
            if (requires != null)
            {
                for (Iterator it = requires.iterator(); it.hasNext();)
                {
                    spec.println("Requires: " + it.next());
                }
            }
            if (conflicts != null)
            {
                for (Iterator it = conflicts.iterator(); it.hasNext();)
                {
                    spec.println("Conflicts: " + it.next());
                }
            }
            if (prefix != null)
            {
                spec.println("Prefix: " + prefix);
            }
            spec.println("BuildRoot: " + buildroot.getAbsolutePath());
            spec.println();
            spec.println("%description");
            if (description != null)
            {
                spec.println(description);
            }
            
            spec.println();
            spec.println("%files");
            for (Iterator it = mappings.iterator(); it.hasNext();)
            {
                Mapping map = (Mapping) it.next();
                spec.println(map.getAttrString() + " " + map.getDestination());
            }
            
            if (preinstall != null)
            {
                spec.println();
                spec.println("%pre");
                spec.println(preinstall);
            }
            if (postinstall != null)
            {
                spec.println();
                spec.println("%post");
                spec.println(postinstall);
            }
            if (preremove != null)
            {
                spec.println();
                spec.println("%preun");
                spec.println(preremove);
            }
            if (postremove != null)
            {
                spec.println();
                spec.println("%postun");
                spec.println(postremove);
            }
            if (verify != null)
            {
                spec.println();
                spec.println("%verifyscript");
                spec.println(verify);
            }
            
            spec.close();
        }
        catch (Throwable t)
        {
            throw new MojoExecutionException("Unable to write " + specf.getAbsolutePath(), t);
        }
    }
}
