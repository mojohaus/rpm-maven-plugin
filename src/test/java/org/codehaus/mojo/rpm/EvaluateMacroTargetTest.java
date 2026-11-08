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

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EvaluateMacroTargetTest {

    private static List<String> arguments(String arch, String vendor, String os) throws Exception {
        RPMMojo mojo = new RPMMojo(null, null);
        set(mojo, "targetArch", arch);
        set(mojo, "targetVendor", vendor);
        set(mojo, "targetOS", os);
        return Arrays.asList(new RPMHelper(mojo).evalCommandline("_libdir").getArguments());
    }

    private static void set(Object target, String field, Object value) throws Exception {
        Field f = AbstractRPMMojo.class.getDeclaredField(field);
        f.setAccessible(true);
        f.set(target, value);
    }

    @Test
    void evalUsesTheResolvedTarget() throws Exception {
        assertEquals(
                Arrays.asList("--target", "noarch-pc-linux", "--eval", "%_libdir"), arguments("noarch", "pc", "linux"));
    }

    @Test
    void evalWithoutAResolvedTargetIsUnchanged() throws Exception {
        assertEquals(Arrays.asList("--eval", "%_libdir"), arguments(null, null, null));
        assertEquals(Arrays.asList("--eval", "%_libdir"), arguments("noarch", null, "linux"));
    }
}
