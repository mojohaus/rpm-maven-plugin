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
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpecWriterGhostTest {

    private static String writeSpec(Mapping... mappings) throws Exception {
        RPMMojo mojo = new RPMMojo(null, null);
        set(mojo, "name", "demo");
        set(mojo, "projversion", "1.0");
        set(mojo, "release", "1");
        set(mojo, "rpmBuildroot", new File("target/buildroot"));
        set(mojo, "buildroot", new File("target/tmp-buildroot"));
        set(mojo, "mappings", java.util.Arrays.asList(mappings));
        StringWriter out = new StringWriter();
        try (PrintWriter spec = new PrintWriter(out)) {
            new SpecWriter(mojo, spec).writeSpecFile();
        }
        return out.toString();
    }

    private static void set(Object target, String field, Object value) throws Exception {
        Field f = AbstractRPMMojo.class.getDeclaredField(field);
        f.setAccessible(true);
        f.set(target, value);
    }

    private static Mapping mapping(String directory) {
        Mapping mapping = new Mapping();
        mapping.setDirectory(directory);
        return mapping;
    }

    @Test
    void ghostMappingNeedsNoSources() throws Exception {
        Mapping ghost = mapping("/var/log/demo.log");
        ghost.setGhost(true);

        String spec = writeSpec(ghost);

        assertTrue(spec.contains("\n%ghost \"/var/log/demo.log\"\n"), spec);
        assertFalse(spec.contains("%dir"), spec);
    }

    @Test
    void ghostMappingCarriesAttributes() throws Exception {
        Mapping ghost = mapping("/var/run/demo.pid");
        ghost.setGhost(true);
        ghost.setFilemode("0644");
        ghost.setUsername("demo");
        ghost.setGroupname("demo");

        String spec = writeSpec(ghost);

        assertTrue(spec.contains("\n%ghost %attr(0644,demo,demo) \"/var/run/demo.pid\"\n"), spec);
    }

    @Test
    void ghostMappingCombinesWithConfiguration() throws Exception {
        Mapping ghost = mapping("/etc/demo/generated.conf");
        ghost.setGhost(true);
        ghost.setConfiguration("noreplace");

        String spec = writeSpec(ghost);

        assertTrue(spec.contains("\n%ghost %config(noreplace) \"/etc/demo/generated.conf\"\n"), spec);
    }

    @Test
    void mappingWithoutGhostStaysADirectory() throws Exception {
        Mapping plain = mapping("/var/lib/demo");
        plain.setAbsoluteDestination(new File("target"));

        String spec = writeSpec(plain);

        assertTrue(spec.contains("\n%dir  \"/var/lib/demo\"\n"), spec);
        assertFalse(spec.contains("%ghost"), spec);
        assertFalse(plain.isGhost());
    }
}
