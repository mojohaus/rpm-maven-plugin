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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpecWriterGlobEscapeTest {

    @TempDir
    Path destination;

    private String writeSpec() throws Exception {
        RPMMojo mojo = new RPMMojo(null, null);
        set(mojo, "name", "demo");
        set(mojo, "projversion", "1.0");
        set(mojo, "release", "1");
        set(mojo, "rpmBuildroot", new File("target/buildroot"));
        set(mojo, "buildroot", new File("target/tmp-buildroot"));
        Mapping mapping = new Mapping();
        mapping.setDirectory("/opt/test");
        mapping.setAbsoluteDestination(destination.toFile());
        mapping.setDirectoryIncluded(false);
        mapping.setRecurseDirectories(true);
        set(mojo, "mappings", Collections.singletonList(mapping));
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

    @Test
    void metacharactersInNamesAreEscaped() {
        assertEquals("testing\\[123\\].txt", SpecWriter.escapeGlob("testing[123].txt"));
        assertEquals("a\\*b\\?c", SpecWriter.escapeGlob("a*b?c"));
        assertEquals("100%%", SpecWriter.escapeGlob("100%"));
        assertEquals("plain-name_1.0.txt", SpecWriter.escapeGlob("plain-name_1.0.txt"));
    }

    @Test
    void scannedFilesAndDirectoriesAreEscapedInTheSpec() throws Exception {
        Files.createDirectories(destination.resolve("dir[1]"));
        Files.write(destination.resolve("dir[1]").resolve("a*b.txt"), new byte[0]);
        Files.write(destination.resolve("testing[123].txt"), new byte[0]);
        Files.write(destination.resolve("plain.txt"), new byte[0]);

        String spec = writeSpec();

        assertTrue(spec.contains("\"/opt/test/testing\\[123\\].txt\"\n"), spec);
        assertTrue(spec.contains("\"/opt/test/dir\\[1\\]\"\n"), spec);
        assertTrue(spec.contains("\"/opt/test/dir\\[1\\]/a\\*b.txt\"\n"), spec);
        assertTrue(spec.contains("\"/opt/test/plain.txt\"\n"), spec);
        assertFalse(spec.contains("testing[123]"), spec);
    }
}
