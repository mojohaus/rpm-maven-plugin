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
import java.util.LinkedHashSet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpecWriterWeakDependenciesTest {

    private static String writeSpec(String field, String... values) throws Exception {
        RPMMojo mojo = new RPMMojo(null, null);
        set(mojo, "name", "demo");
        set(mojo, "projversion", "1.0");
        set(mojo, "release", "1");
        set(mojo, "rpmBuildroot", new File("target/buildroot"));
        set(mojo, "buildroot", new File("target/tmp-buildroot"));
        set(mojo, "conflicts", new LinkedHashSet<String>(Arrays.asList("incinerator")));
        if (field != null) {
            set(mojo, field, new LinkedHashSet<String>(Arrays.asList(values)));
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
    void eachWeakDependencyIsWrittenWithItsTag() throws Exception {
        assertTrue(writeSpec("recommends", "a", "b").contains("\nRecommends: a\nRecommends: b\n"));
        assertTrue(writeSpec("suggests", "a").contains("\nSuggests: a\n"));
        assertTrue(writeSpec("supplements", "a").contains("\nSupplements: a\n"));
        assertTrue(writeSpec("enhances", "a >= 1.0").contains("\nEnhances: a >= 1.0\n"));
    }

    @Test
    void weakDependenciesFollowConflicts() throws Exception {
        String spec = writeSpec("recommends", "a");

        assertTrue(spec.indexOf("Conflicts: incinerator") < spec.indexOf("Recommends: a"), spec);
        assertTrue(spec.indexOf("Recommends: a") < spec.indexOf("autoprov:"), spec);
    }

    @Test
    void nothingIsWrittenByDefault() throws Exception {
        String spec = writeSpec(null);

        for (String tag : new String[] {"Recommends:", "Suggests:", "Supplements:", "Enhances:"}) {
            assertFalse(spec.contains(tag), spec);
        }
        assertEquals(1, spec.split("Conflicts:", -1).length - 1);
    }
}
