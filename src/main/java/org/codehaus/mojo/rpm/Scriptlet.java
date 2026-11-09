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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;

import org.apache.maven.shared.filtering.FilterWrapper;

/**
 * Defines a scriptlet including the optinal {@link #getSubpackage()} and {@link #getProgram()}. The (optional) contents
 * can be provided by either {@link #getScript()} or {@link #getScriptFile()}.
 *
 * @author Brett Okken, Cerner Corp.
 * @since 2.0-beta-4
 */
public class Scriptlet {
    private static final String CLASSPATH_PREFIX = "classpath:";

    /**
     * Optional subpackage.
     *
     * @see #getSubpackage()
     */
    private String subpackage;

    /**
     * Program to execute script.
     *
     * @see #getProgram()
     */
    private String program;

    /**
     * Contents of the script. Mutually exclusive with {@link #scriptFile}.
     *
     * @see #getScript()
     */
    private String script;

    /**
     * Script to execute. Mutually exclusive with {@link #script}.
     *
     * @see #getScriptFile()
     */
    private String scriptFile;

    /**
     * Encoding of {@link #scriptFile}.
     *
     * @see #getFileEncoding().
     */
    private String fileEncoding;

    /**
     * The default encoding of the project, used if {@link #fileEncoding} is not set.
     *
     * @see #getFileEncoding().
     */
    private String sourceEncoding;

    /**
     * Switch to filter the scriptlet
     *
     * @see #isFilter().
     */
    private boolean filter;

    /**
     * The optional subpackage. This is passed as a <i>-n</i> argument to the scriptlet directive.
     *
     * @return Returns the {@link #subpackage}.
     */
    public String getSubpackage() {
        return this.subpackage;
    }

    /**
     * @param subpackage The {@link #subpackage} to set.
     */
    public void setSubpackage(String subpackage) {
        this.subpackage = subpackage;
    }

    /**
     * The program to use to execute the script. This is passed as a <i>-p</i> argument to the scriptlet directive.
     *
     * @return Returns the {@link #program}.
     */
    public String getProgram() {
        return this.program;
    }

    /**
     * @param program The {@link #program} to set.
     */
    public void setProgram(String program) {
        this.program = program;
    }

    /**
     * The contents of the script as a {@code String}. This value will override anything at {@link #getScriptFile()}.
     *
     * @return Returns the {@link #script}.
     */
    public String getScript() {
        return this.script;
    }

    /**
     * @param script The {@link #script} to set.
     */
    public void setScript(String script) {
        this.script = script;
    }

    /**
     * The location of the script: a file path (absolute, or relative to the project base directory) or, with the prefix
     * {@code classpath:}, a resource on the plugin class path, for example a script shipped in a jar that is added to
     * the plugin's {@code dependencies}. This is ignored if {@link #getScript()} is populated.
     *
     * @return Returns the {@link #scriptFile}.
     * @since 2.5.0 the {@code classpath:} prefix, and the return type {@code String} instead of {@code File}
     */
    public String getScriptFile() {
        return this.scriptFile;
    }

    /**
     * @param scriptFile The {@link #scriptFile} to set.
     */
    public void setScriptFile(String scriptFile) {
        this.scriptFile = scriptFile;
    }

    /**
     * The encoding to use to read {@link #getScriptFile()}. If {@code null}, the default character encoding for th
     * platform will be used.
     *
     * @return Returns the {@link #fileEncoding}.
     */
    public String getFileEncoding() {
        if (fileEncoding != null && !"".equals(fileEncoding)) {
            return this.fileEncoding;
        }
        return this.sourceEncoding;
    }

    /**
     * @param fileEncoding The {@link #fileEncoding} to set.
     */
    public void setFileEncoding(String fileEncoding) {
        this.fileEncoding = fileEncoding;
    }

    /**
     * This is the maven property: project.build.sourceEncoding
     *
     * @param sourceEncoding The {@link #sourceEncoding} to set.
     */
    public void setSourceEncoding(String sourceEncoding) {
        this.sourceEncoding = sourceEncoding;
    }

    /**
     * @return Returns the {@link #filter}.
     */
    public boolean isFilter() {
        return this.filter;
    }

    /**
     * @param filter The {@link #filter} to set.
     */
    public void setFilter(boolean filter) {
        this.filter = filter;
    }

    /**
     * Writes the scriptlet.
     *
     * @param writer {@code PrintWriter} to write content to.
     * @param directive The directive for the scriptlet.
     * @param mojo the {@code AbstractRPMMojo} which contains any resources needed when writing the scriptlet.
     * @throws IOException if an I/IO error occurs
     */
    protected final void write(final PrintWriter writer, final String directive, final AbstractRPMMojo mojo)
            throws IOException {
        if (script != null || scriptFile != null || program != null) {
            writer.println();
            writer.println(buildScriptletLine(directive));

            writeContent(writer, mojo);
        }
    }

    /**
     * Builds the scriptlet line including any optional args.
     *
     * @param directive The directive for the scriptlet.
     * @return The scriptlet line - this does not include the script.
     */
    protected String buildScriptletLine(final String directive) {
        final StringBuilder builder = new StringBuilder();

        builder.append(directive);
        if (subpackage != null) {
            builder.append(" -n ");
            builder.append(subpackage);
        }

        if (program != null) {
            builder.append(" -p ");
            builder.append(program);
        }

        return builder.toString();
    }

    /**
     * Opens {@link #getScriptFile()}: a {@code classpath:} value is read from the plugin class loader, anything else
     * is a file path that is resolved against the project base directory, as Maven does for a {@code File} parameter.
     */
    private Reader getScriptReader(AbstractRPMMojo mojo) throws IOException {
        final InputStream in;
        if (scriptFile.startsWith(CLASSPATH_PREFIX)) {
            in = openClasspathResource(scriptFile.substring(CLASSPATH_PREFIX.length()));
        } else {
            File file = new File(scriptFile);
            if (!file.isAbsolute()) {
                // what Maven does for a File parameter: relative paths are based on the project directory
                file = new File(mojo.getProject().getBasedir(), scriptFile);
            }
            if (!file.isFile()) {
                throw missingScriptFile();
            }
            in = new FileInputStream(file);
        }
        try {
            return fileEncoding != null ? new InputStreamReader(in, fileEncoding) : new InputStreamReader(in);
        } catch (IOException | RuntimeException e) {
            in.close();
            throw e;
        }
    }

    private InputStream openClasspathResource(String name) {
        while (name.startsWith("/")) {
            name = name.substring(1);
        }
        InputStream in = Scriptlet.class.getClassLoader().getResourceAsStream(name);
        if (in == null && Thread.currentThread().getContextClassLoader() != null) {
            in = Thread.currentThread().getContextClassLoader().getResourceAsStream(name);
        }
        if (in == null) {
            throw missingScriptFile();
        }
        return in;
    }

    private RuntimeException missingScriptFile() {
        return new RuntimeException(
                "Invalid scriptlet declaration found - defined scriptFile does not exist: " + scriptFile);
    }

    /**
     * Writes the content (either {@link #getScript()} or {@link #getScriptFile()}) to <i>writer</i>.
     *
     * @param writer {@code PrintWriter} to write content to.
     * @param mojo the {@code AbstractRPMMojo} which contains any resources needed when writing the scriptlet.
     * @throws IOException if an I/IO error occurs
     */
    protected final void writeContent(PrintWriter writer, final AbstractRPMMojo mojo) throws IOException {
        if (script != null) {
            writer.println(script);
        } else if (scriptFile != null) {
            Reader reader = getScriptReader(mojo);
            if (filter) {
                for (FilterWrapper filterWrapper : mojo.getFilterWrappers()) {
                    reader = filterWrapper.getReader(reader);
                }
            }
            try (BufferedReader bufferedReader = new BufferedReader(reader)) {
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    writer.println(line);
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        StringBuilder buffer = new StringBuilder(64);
        buffer.append("Scriptlet[");
        buffer.append("subpackage=");
        buffer.append(subpackage);
        buffer.append(",program=");
        buffer.append(program);
        buffer.append(",script=");
        buffer.append(script);
        buffer.append(",scriptFile=");
        buffer.append(scriptFile);
        buffer.append(",fileEncoding=");
        buffer.append(fileEncoding);
        buffer.append("]");
        return buffer.toString();
    }
}
