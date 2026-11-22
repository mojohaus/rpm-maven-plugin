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
import java.util.Collections;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The relocation prefixes of the former {@code rpm-relocate} integration test.
 */
class SpecWriterPrefixTest {

    private static void set(Object target, String field, Object value) throws Exception {
        Field f = AbstractRPMMojo.class.getDeclaredField(field);
        f.setAccessible(true);
        f.set(target, value);
    }

    private static String writeSpec() throws Exception {
        RPMMojo mojo = new RPMMojo(null, null);
        set(mojo, "name", "rpm-relocate");
        set(mojo, "projversion", "1.0");
        set(mojo, "release", "1");
        set(mojo, "rpmBuildroot", new File("target/buildroot"));
        set(mojo, "buildroot", new File("target/tmp-buildroot"));
        set(mojo, "prefix", "/tmp");
        set(mojo, "prefixes", new java.util.ArrayList<>(Collections.singletonList("/opt")));
        mojo.addPrefixToPrefixes();
        StringWriter out = new StringWriter();
        try (PrintWriter spec = new PrintWriter(out)) {
            new SpecWriter(mojo, spec).writeSpecFile();
        }
        return out.toString();
    }

    @Test
    void prefixAndPrefixesAreBothWrittenToTheSpec() throws Exception {
        String spec = writeSpec();

        assertTrue(spec.contains("\nPrefix: /opt\n"), spec);
        assertTrue(spec.contains("\nPrefix: /tmp\n"), spec);
    }

    @Test
    void prefixAloneIsWrittenWithoutPrefixes() throws Exception {
        RPMMojo mojo = new RPMMojo(null, null);
        set(mojo, "prefix", "/usr");
        mojo.addPrefixToPrefixes();

        assertEquals(Collections.singletonList("/usr"), mojo.getPrefixes());
    }
}
