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
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.CommandLineUtils.StringStreamConsumer;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.StreamConsumer;

/**
 * Utility to interact with rpm and rpmbuild commands.
 *
 * @author Brett Okken
 * @since 2.0
 */
final class RPMHelper {
    private final AbstractRPMMojo mojo;

    /**
     * @param mojo
     */
    public RPMHelper(AbstractRPMMojo mojo) {
        super();
        this.mojo = mojo;
    }

    /**
     * Gets the default host vendor for system by executing <i>rpm -E %{_host_vendor}</i>.
     */
    public String getHostVendor() throws MojoExecutionException {
        final Log log = mojo.getLog();

        final Commandline cl = new Commandline();
        cl.setExecutable("rpm");
        cl.addArguments(new String[] {"-E", "%{_host_vendor}"});

        final StringStreamConsumer stdout = new StringStreamConsumer();
        final StreamConsumer stderr = new LogStreamConsumer(LogStreamConsumer.INFO, log);
        try {
            if (log.isDebugEnabled()) {
                log.debug("About to execute \'" + cl.toString() + "\'");
            }

            int result = CommandLineUtils.executeCommandLine(cl, stdout, stderr);
            if (result != 0) {
                throw new MojoExecutionException("RPM query for default vendor returned: \'" + result
                        + "\' executing \'" + cl.toString() + "\'");
            }
        } catch (CommandLineException e) {
            throw new MojoExecutionException("Unable to query for default vendor from RPM", e);
        }

        return stdout.getOutput().trim();
    }

    /**
     * Run the external command to build the package.
     * <p>
     * Uses the following attributes from the {@link #mojo}:
     * <ul>
     * <li>{@link AbstractRPMMojo#getBuildroot() build root}</li>
     * <li>{@link AbstractRPMMojo#getKeyname() key name}</li>
     * <li>{@link AbstractRPMMojo#getKeyPassphrase() key passphrase}</li>
     * <li>{@link AbstractRPMMojo#getName() name}</li>
     * <li>{@link AbstractRPMMojo#getRPMFile() rpm file}</li>
     * <li>{@link AbstractRPMMojo#getTargetArch() target arch}</li>
     * <li>{@link AbstractRPMMojo#getTargetOS() target OS}</li>
     * <li>{@link AbstractRPMMojo#getTargetVendor() target vendor}</li>
     * <li>{@link AbstractRPMMojo#getWorkarea() workarea}</li>
     * </ul>
     * </p>
     *
     * @throws MojoExecutionException if an error occurs
     */
    public void buildPackage() throws MojoExecutionException {
        final Commandline cl = buildCommandline();

        final Log log = mojo.getLog();

        final StreamConsumer stdout = new LogStreamConsumer(LogStreamConsumer.INFO, log);
        final StreamConsumer stderr = new LogStreamConsumer(LogStreamConsumer.INFO, log);
        try {
            if (log.isDebugEnabled()) {
                log.debug("About to execute \'" + cl.toString() + "\'");
            }

            int result = CommandLineUtils.executeCommandLine(cl, stdout, stderr);
            if (result != 0) {
                throw new MojoExecutionException(
                        "RPM build execution returned: \'" + result + "\' executing \'" + cl.toString() + "\'");
            }
        } catch (CommandLineException e) {
            throw new MojoExecutionException("Unable to build the RPM", e);
        }

        // if the keyname has been provided, try to sign
        final String keyname = mojo.getKeyname();
        final File keypath = mojo.getKeypath();
        final Passphrase keyPassphrase = mojo.getKeyPassphrase();

        if (keyname != null) {
            RPMSigner signer =
                    new RPMSigner(keypath, keyname, keyPassphrase != null ? keyPassphrase.toCharArray() : null, log);

            try {
                signer.sign(mojo.getRPMFile());
            } catch (Exception e) {
                throw new MojoExecutionException("Unable to sign RPM", e);
            }
        }
    }

    /**
     * Creates the <code>rpm --eval</code> command line for the <i>macro</i>. It passes the target of the mojo, because
     * macros such as <code>%{_libdir}</code> differ between targets. Without a resolved target the option is left out.
     */
    Commandline evalCommandline(String macro) {
        final Commandline cl = new Commandline();
        cl.setExecutable("rpm");
        final String arch = mojo.getTargetArch();
        final String vendor = mojo.getTargetVendor();
        final String os = mojo.getTargetOS();
        if (arch != null && vendor != null && os != null) {
            cl.createArg().setValue("--target");
            cl.createArg().setValue(arch + '-' + vendor + '-' + os);
        }
        cl.createArg().setValue("--eval");
        cl.createArg().setValue('%' + macro);
        return cl;
    }

    /**
     * Creates the <i>rpmbuild</i> command line, with the {@link AbstractRPMMojo#getRpmbuildArgs() additional
     * arguments} just before the spec file name.
     */
    Commandline buildCommandline() {
        final File workarea = mojo.getWorkarea();
        final File f = new File(workarea, "SPECS");

        final Commandline cl = new Commandline();
        cl.setExecutable("rpmbuild");
        cl.setWorkingDirectory(f.getAbsolutePath());
        cl.createArg().setValue(mojo.getRpmbuildStage());
        cl.createArg().setValue("--target");
        cl.createArg().setValue(mojo.getTargetArch() + '-' + mojo.getTargetVendor() + '-' + mojo.getTargetOS());
        cl.createArg().setValue("--buildroot");
        cl.createArg().setValue(FileHelper.toUnixPath(mojo.getRPMBuildroot()));
        cl.createArg().setValue("--define");
        cl.createArg().setValue("_topdir " + FileHelper.toUnixPath(workarea));

        // Don't assume default values for rpm path macros
        cl.createArg().setValue("--define");
        cl.createArg().setValue("_build_name_fmt %%{ARCH}/%%{NAME}-%%{VERSION}-%%{RELEASE}.%%{ARCH}.rpm");
        cl.createArg().setValue("--define");
        cl.createArg().setValue("_builddir %{_topdir}/BUILD");
        cl.createArg().setValue("--define");
        cl.createArg().setValue("_rpmdir %{_topdir}/RPMS");
        cl.createArg().setValue("--define");
        cl.createArg().setValue("_sourcedir %{_topdir}/SOURCES");
        cl.createArg().setValue("--define");
        cl.createArg().setValue("_specdir %{_topdir}/SPECS");
        cl.createArg().setValue("--define");
        cl.createArg().setValue("_srcrpmdir %{_topdir}/SRPMS");

        final List<String> extraArgs = mojo.getRpmbuildArgs();
        if (extraArgs != null) {
            for (String arg : extraArgs) {
                cl.createArg().setValue(arg);
            }
        }

        cl.createArg().setValue(mojo.getName() + ".spec");

        return cl;
    }

    /**
     * Evaluates the <i>macro</i> by executing <code>rpm --target <i>arch-vendor-os</i> --eval %<i>macro</i></code>.
     *
     * /**
     * Evaluates the <i>macro</i> by executing <code>rpm --eval %<i>macro</i></code>.
     *
     * @param macro The macro to evaluate.
     * @return The result of rpm --eval.
     * @throws MojoExecutionException
     * @since 2.1-alpha-1
     */
    public String evaluateMacro(String macro) throws MojoExecutionException {
        final Commandline cl = evalCommandline(macro);

        final Log log = mojo.getLog();

        final StringStreamConsumer stdout = new StringStreamConsumer();
        final StreamConsumer stderr = new LogStreamConsumer(LogStreamConsumer.INFO, log);
        try {
            if (log.isDebugEnabled()) {
                log.debug("About to execute \'" + cl.toString() + "\'");
            }

            int result = CommandLineUtils.executeCommandLine(cl, stdout, stderr);
            if (result != 0) {
                throw new MojoExecutionException(
                        "rpm --eval returned: \'" + result + "\' executing \'" + cl.toString() + "\'");
            }
        } catch (CommandLineException e) {
            throw new MojoExecutionException("Unable to evaluate macro: " + macro, e);
        }

        return stdout.getOutput().trim();
    }

    /**
     * Gets the architecure for system by executing <i>rpm -E %{_arch}</i>.
     */
    public String getArch() throws MojoExecutionException {
        final Commandline cl = new Commandline();
        cl.setExecutable("rpm");
        cl.addArguments(new String[] {"-E", "%{_arch}"});

        final StringStreamConsumer stdConsumer = new StringStreamConsumer();
        final StreamConsumer errConsumer = new LogStreamConsumer(LogStreamConsumer.INFO, mojo.getLog());
        try {
            if (mojo.getLog().isDebugEnabled()) mojo.getLog().debug("About to execute \'" + cl.toString() + "\'");
            final int result = CommandLineUtils.executeCommandLine(cl, stdConsumer, errConsumer);
            if (result != 0)
                throw new MojoExecutionException(
                        "rpm -E %{_arch} returned: \'" + result + "\' executing \'" + cl.toString() + "\'");
        } catch (CommandLineException e) {
            throw new MojoExecutionException("Unable get system architecture", e);
        }

        return stdConsumer.getOutput().trim();
    }

    /**
     * Creates the command line that queries one attribute of an rpm file. It runs in the C locale, like the signing
     * commands, so that the output does not depend on the user's language.
     *
     * @param rpmFile The rpm file to query.
     * @param attribute The attribute to query, such as <code>NAME</code>.
     * @return The command line.
     */
    Commandline rpmQueryCommandline(File rpmFile, String attribute) {
        final Commandline cl = new Commandline();
        cl.setExecutable("rpm");
        cl.createArg().setValue("--query");
        cl.createArg().setValue("--package");
        cl.createArg().setValue(rpmFile.getAbsolutePath());
        cl.createArg().setValue("--queryformat");
        cl.createArg().setValue("%{" + attribute + "}");
        cl.addEnvironment("LC_ALL", "C");
        cl.addEnvironment("LANG", "C");
        return cl;
    }

    /**
     * Gets the value of the RPM attribute for the given RPM file..
     * @param rpmFile The full path of the RPM file.
     * @param attribute The attribute to get the value for.
     * @return A String containing the value of the requested attribute.
     * @throws MojoExecutionException if the attribute could not be read.
     */
    public String getRpmAttribute(File rpmFile, String attribute) throws MojoExecutionException {
        final Commandline cl = rpmQueryCommandline(rpmFile, attribute);

        final Log log = mojo.getLog();

        final StringStreamConsumer stdout = new StringStreamConsumer();
        final StreamConsumer stderr = new LogStreamConsumer(LogStreamConsumer.INFO, log);
        try {
            if (log.isDebugEnabled()) {
                log.debug("About to execute \'" + cl.toString() + "\'");
            }

            int result = CommandLineUtils.executeCommandLine(cl, stdout, stderr);
            if (result != 0) {
                throw new MojoExecutionException(
                        "rpm -query returned: \'" + result + "\' executing \'" + cl.toString() + "\'");
            }
        } catch (CommandLineException e) {
            throw new MojoExecutionException("Unable to get " + attribute + " from rpm", e);
        }

        return stdout.getOutput().trim();
    }
}
