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
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import org.codehaus.plexus.util.cli.Commandline;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RpmbuildArgsTest {

    private static List<String> arguments(Object rpmbuildArgs) throws Exception {
        RPMMojo mojo = new RPMMojo(null, null);
        set(mojo, "name", "demo");
        set(mojo, "workarea", new File("target/rpm/demo"));
        set(mojo, "rpmBuildroot", new File("target/rpm/demo/buildroot"));
        set(mojo, "rpmbuildStage", "-bb");
        set(mojo, "targetArch", "noarch");
        set(mojo, "targetVendor", "pc");
        set(mojo, "targetOS", "linux");
        if (rpmbuildArgs != null) {
            set(mojo, "rpmbuildArgs", rpmbuildArgs);
        }
        Commandline cl = new RPMHelper(mojo).buildCommandline();
        return Arrays.asList(cl.getArguments());
    }

    private static void set(Object target, String field, Object value) throws Exception {
        Field f = AbstractRPMMojo.class.getDeclaredField(field);
        f.setAccessible(true);
        f.set(target, value);
    }

    @Test
    void argumentsComeJustBeforeTheSpecName() throws Exception {
        List<String> args = arguments(Arrays.asList("--rcfile", "/etc/rpm/custom.rc"));

        int last = args.size() - 1;
        assertEquals("demo.spec", args.get(last));
        assertEquals("/etc/rpm/custom.rc", args.get(last - 1));
        assertEquals("--rcfile", args.get(last - 2));
        assertEquals("_srcrpmdir %{_topdir}/SRPMS", args.get(last - 3));
    }

    @Test
    void defaultCommandLineIsUnchanged() throws Exception {
        List<String> args = arguments(null);

        assertEquals("demo.spec", args.get(args.size() - 1));
        assertEquals("_srcrpmdir %{_topdir}/SRPMS", args.get(args.size() - 2));
        assertEquals(args, arguments(java.util.Collections.<String>emptyList()));
    }
}
