package org.codehaus.mojo.rpm;

/*
 * Copyright MojoHaus and Contributors
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.nio.file.Path;

import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The scriptlet files that do not exist, as the former {@code MRPM-117} and
 * {@code rpm-scriptlet-missing-classpath} integration tests checked them in the build log.
 */
class SpecWriterScriptletFileTest {

    @TempDir
    Path basedir;

    private static void set(Object target, String field, Object value) throws Exception {
        Field f = AbstractRPMMojo.class.getDeclaredField(field);
        f.setAccessible(true);
        f.set(target, value);
    }

    private static Scriptlet scriptFile(String scriptFile) {
        Scriptlet scriptlet = new Scriptlet();
        scriptlet.setScriptFile(scriptFile);
        return scriptlet;
    }

    private String writeSpec(Scriptlet preinstall, Scriptlet postinstall) throws Exception {
        MavenProject project = new MavenProject();
        project.setFile(new File(basedir.toFile(), "pom.xml"));
        RPMMojo mojo = new RPMMojo(null, null);
        set(mojo, "name", "demo");
        set(mojo, "projversion", "1.0");
        set(mojo, "release", "1");
        set(mojo, "project", project);
        set(mojo, "rpmBuildroot", new File("target/buildroot"));
        set(mojo, "buildroot", new File("target/tmp-buildroot"));
        set(mojo, "preinstallScriptlet", preinstall);
        set(mojo, "postinstallScriptlet", postinstall);
        StringWriter out = new StringWriter();
        try (PrintWriter spec = new PrintWriter(out)) {
            new SpecWriter(mojo, spec).writeSpecFile();
        }
        return out.toString();
    }

    @Test
    void missingScriptFileIsReportedWithItsPathAndAnEmptyScriptletIsIgnored() {
        // MRPM-117: the empty preinstallScriptlet must not fail the build, the postinstallScriptlet must
        RuntimeException e = assertThrows(
                RuntimeException.class, () -> writeSpec(new Scriptlet(), scriptFile("src/main/bin/doesnotexist.sh")));

        assertEquals(
                "Invalid scriptlet declaration found - defined scriptFile does not exist: "
                        + "src/main/bin/doesnotexist.sh",
                e.getMessage());
    }

    @Test
    void emptyScriptletWritesNothing() throws Exception {
        assertFalse(writeSpec(new Scriptlet(), null).contains("%pre"));
    }

    @Test
    void missingClasspathScriptIsReportedWithTheClasspathPrefix() {
        RuntimeException e =
                assertThrows(RuntimeException.class, () -> writeSpec(scriptFile("classpath:no/such/script.sh"), null));

        assertEquals(
                "Invalid scriptlet declaration found - defined scriptFile does not exist: "
                        + "classpath:no/such/script.sh",
                e.getMessage());
    }
}
