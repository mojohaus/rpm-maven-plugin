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
import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpecWriterPreambleLinesTest {

    private static String writeSpec(Object preambleLines) throws Exception {
        RPMMojo mojo = new RPMMojo(null, null);
        set(mojo, "name", "demo");
        set(mojo, "projversion", "1.0");
        set(mojo, "release", "1");
        set(mojo, "rpmBuildroot", new File("target/buildroot"));
        set(mojo, "buildroot", new File("target/tmp-buildroot"));
        if (preambleLines != null) {
            set(mojo, "preambleLines", preambleLines);
        }
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
    void linesAreWrittenVerbatimBeforeDescription() throws Exception {
        String spec = writeSpec(Arrays.asList("%{?systemd_requires}", "BuildRequires: systemd"));

        int buildRoot = spec.indexOf("BuildRoot: ");
        int first = spec.indexOf("\n%{?systemd_requires}\n");
        int second = spec.indexOf("\nBuildRequires: systemd\n");
        int description = spec.indexOf("\n%description\n");
        assertTrue(buildRoot >= 0 && buildRoot < first, spec);
        assertTrue(first < second, spec);
        assertTrue(second < description, spec);
    }

    @Test
    void defaultOutputIsUnchanged() throws Exception {
        assertEquals(writeSpec(null), writeSpec(Collections.<String>emptyList()));
        assertTrue(writeSpec(null).contains("\nBuildRoot: "));
        assertTrue(writeSpec(null).contains("\n\n%description\n"));
    }
}
