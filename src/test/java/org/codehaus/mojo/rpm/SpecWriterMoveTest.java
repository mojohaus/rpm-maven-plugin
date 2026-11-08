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
import java.util.Collections;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpecWriterMoveTest {

    private static String writeSpec(File buildroot) throws Exception {
        RPMMojo mojo = new RPMMojo(null, null);
        set(mojo, "name", "demo");
        set(mojo, "projversion", "1.0");
        set(mojo, "release", "1");
        set(mojo, "rpmBuildroot", new File("target/buildroot"));
        set(mojo, "buildroot", buildroot);
        Mapping mapping = new Mapping();
        mapping.setDirectory("/var/lib/demo");
        mapping.setAbsoluteDestination(new File("target"));
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
    void moveCopiesDotfilesAndQuotesEveryPath() throws Exception {
        String spec = writeSpec(new File("/work/tmp-buildroot"));

        assertTrue(spec.contains("\nTMP_BUILD_ROOT=\"/work/tmp-buildroot\"\n"), spec);
        assertTrue(spec.contains("\nmkdir -p \"$RPM_BUILD_ROOT\"\n"), spec);
        assertTrue(
                spec.contains("\ncp -a \"$TMP_BUILD_ROOT\"/. \"$RPM_BUILD_ROOT\"/ && rm -rf \"$TMP_BUILD_ROOT\"\n"),
                spec);
        assertFalse(spec.contains("/* "), spec);
        assertFalse(spec.contains(" $RPM_BUILD_ROOT\n"), spec);
    }

    @Test
    void shellMetacharactersInTheBuildrootStayInsideTheQuotes() throws Exception {
        String spec = writeSpec(new File("/home/tmp/bad&&dir with $HOME/tmp-buildroot"));

        assertTrue(spec.contains("\nTMP_BUILD_ROOT=\"/home/tmp/bad&&dir with \\$HOME/tmp-buildroot\"\n"), spec);
    }
}
