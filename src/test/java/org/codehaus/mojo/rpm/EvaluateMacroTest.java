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
import java.lang.reflect.Method;
import java.util.Arrays;

import org.apache.maven.plugin.MojoExecutionException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EvaluateMacroTest {

    private static RPMMojo mojoWith(String... defines) throws Exception {
        RPMMojo mojo = new RPMMojo(null, null);
        Field f = AbstractRPMMojo.class.getDeclaredField("defineStatements");
        f.setAccessible(true);
        f.set(mojo, Arrays.asList(defines));
        Method process = AbstractRPMMojo.class.getDeclaredMethod("processDefineStatements");
        process.setAccessible(true);
        process.invoke(mojo);
        return mojo;
    }

    @Test
    void plainValueIsReturnedAsDefined() throws Exception {
        assertEquals("/etc/my-app", mojoWith("conf /etc/my-app").evaluateMacro("conf"));
    }

    @Test
    void macroInTheValueIsExpandedInBothForms() throws Exception {
        RPMMojo mojo = mojoWith("base /etc", "myappsysconf %{base}/my-app", "other %base/other");

        assertEquals("/etc/my-app", mojo.evaluateMacro("myappsysconf"));
        assertEquals("/etc/other", mojo.evaluateMacro("other"));
    }

    @Test
    void chainsOfMacrosAreExpandedRecursively() throws Exception {
        RPMMojo mojo = mojoWith("a /root", "b %{a}/b", "c %{b}/c");

        assertEquals("/root/b/c", mojo.evaluateMacro("c"));
    }

    @Test
    void escapedPercentIsLeftAlone() throws Exception {
        assertEquals("100%%{x}", mojoWith("pct 100%%{x}").evaluateMacro("pct"));
    }

    @Test
    void selfReferenceIsReportedWithTheChain() throws Exception {
        RPMMojo mojo = mojoWith("a %{b}", "b %{a}");

        MojoExecutionException e = assertThrows(MojoExecutionException.class, () -> mojo.evaluateMacro("a"));
        assertTrue(e.getMessage().contains("a -> b -> a"), e.getMessage());

        MojoExecutionException direct = assertThrows(
                MojoExecutionException.class, () -> mojoWith("x %{x}").evaluateMacro("x"));
        assertTrue(direct.getMessage().contains("x -> x"), direct.getMessage());
    }

    @Test
    void tooDeepChainIsRejected() throws Exception {
        String[] defines = new String[20];
        for (int i = 0; i < 20; i++) {
            defines[i] = "m" + i + " %{m" + (i + 1) + "}";
        }
        RPMMojo mojo = mojoWith(defines);

        MojoExecutionException e = assertThrows(MojoExecutionException.class, () -> mojo.evaluateMacro("m0"));
        assertTrue(e.getMessage().contains("levels deep"), e.getMessage());
    }
}
