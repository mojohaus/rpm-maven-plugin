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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OptionalGroupTest {

    private static String writeSpec(String group) throws Exception {
        RPMMojo mojo = new RPMMojo(null, null);
        set(mojo, "name", "demo");
        set(mojo, "projversion", "1.0");
        set(mojo, "release", "1");
        set(mojo, "rpmBuildroot", new File("target/buildroot"));
        set(mojo, "buildroot", new File("target/tmp-buildroot"));
        if (group != null) {
            set(mojo, "group", group);
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
    void groupParameterIsNotRequired() throws Exception {
        // @Parameter has class retention, so read the descriptor that plugin:descriptor generated from it
        String descriptor = new String(
                Files.readAllBytes(Paths.get("target/classes/META-INF/maven/plugin.xml")), StandardCharsets.UTF_8);
        Matcher parameter = Pattern.compile("<parameter>\\s*<name>group</name>.*?</parameter>", Pattern.DOTALL)
                .matcher(descriptor);

        assertTrue(parameter.find(), "no group parameter in the plugin descriptor");
        assertTrue(parameter.group().contains("<required>false</required>"), parameter.group());
    }

    @Test
    void noGroupLineWithoutAGroup() throws Exception {
        assertFalse(writeSpec(null).contains("Group:"));
    }

    @Test
    void groupLineIsWrittenWhenGiven() throws Exception {
        String spec = writeSpec("Applications/System");

        assertTrue(spec.contains("\nGroup: Applications/System\n"), spec);
        assertEquals(1, spec.split("Group:", -1).length - 1);
    }
}
