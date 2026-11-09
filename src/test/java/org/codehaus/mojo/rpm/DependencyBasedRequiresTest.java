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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.model.Dependency;
import org.codehaus.plexus.util.cli.Commandline;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DependencyBasedRequiresTest {

    private static Artifact artifact(String artifactId, String type) {
        return new DefaultArtifact(
                "org.example", artifactId, "1.0", "runtime", type, null, new DefaultArtifactHandler(type));
    }

    private static Dependency dependency(String artifactId, String type) {
        Dependency d = new Dependency();
        d.setGroupId("org.example");
        d.setArtifactId(artifactId);
        d.setVersion("1.0");
        d.setType(type);
        return d;
    }

    @Test
    void requireHasNameVersionAndRelease() {
        assertEquals("base-lib >= 1.2.3-4", AbstractRPMMojo.dependencyRequire("base-lib", "1.2.3", "4"));
    }

    @Test
    void onlyDirectRpmDependenciesAreSelected() {
        Artifact direct = artifact("direct-rpm", "rpm");
        Artifact transitive = artifact("transitive-rpm", "rpm");
        Artifact jar = artifact("a-jar", "jar");

        List<Artifact> selected = AbstractRPMMojo.selectRpmDependencies(
                Arrays.asList(jar, transitive, direct),
                Arrays.asList(dependency("a-jar", "jar"), dependency("direct-rpm", "rpm")));

        assertEquals(Collections.singletonList(direct), selected);
    }

    @Test
    void noRpmDependenciesSelectsNothing() {
        assertTrue(AbstractRPMMojo.selectRpmDependencies(
                        Collections.singletonList(artifact("a-jar", "jar")),
                        Collections.singletonList(dependency("a-jar", "jar")))
                .isEmpty());
    }

    @Test
    void rpmQueryRunsInTheCLocale() throws Exception {
        RPMMojo mojo = new RPMMojo(null, null);
        Commandline cl = new RPMHelper(mojo).rpmQueryCommandline(new File("dep.rpm"), "NAME");

        assertTrue(cl.getExecutable().contains("rpm"), cl.getExecutable());
        String[] args = cl.getArguments();
        assertEquals("--queryformat", args[args.length - 2]);
        assertEquals("%{NAME}", args[args.length - 1]);
        List<String> env = Arrays.asList(cl.getEnvironmentVariables());
        assertTrue(env.contains("LC_ALL=C"), env.toString());
        assertTrue(env.contains("LANG=C"), env.toString());
    }
}
