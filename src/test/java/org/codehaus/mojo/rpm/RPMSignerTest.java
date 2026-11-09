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
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Properties;

import org.apache.maven.plugin.logging.SystemStreamLog;
import org.codehaus.plexus.util.cli.Commandline;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RPMSignerTest {

    private final File rpm = new File("target/rpm/RPMS/noarch/demo-1.0-1.noarch.rpm").getAbsoluteFile();

    private RPMSigner signer(char[] passphrase) {
        return new RPMSigner(new File("target/gnupg"), "Demo Key", passphrase, new SystemStreamLog(), "-1", null);
    }

    private static Properties environment(Commandline cl) throws Exception {
        Properties env = new Properties();
        for (String entry : cl.getEnvironmentVariables()) {
            int eq = entry.indexOf('=');
            env.setProperty(entry.substring(0, eq), entry.substring(eq + 1));
        }
        return env;
    }

    @Test
    void expectRunsInTheCLocaleAndReadsTheScriptFromStdin() throws Exception {
        Commandline cl = signer("secret".toCharArray()).expectCommandline(rpm);

        assertTrue(cl.getExecutable().contains("expect"), cl.getExecutable());
        assertEquals(Arrays.asList("-"), Arrays.asList(cl.getArguments()));
        Properties env = environment(cl);
        assertEquals("C", env.getProperty("LC_ALL"));
        assertEquals("C", env.getProperty("LANG"));
    }

    @Test
    void rpmsignRunsInTheCLocale() throws Exception {
        Commandline cl = signer(null).rpmsignCommandline(rpm);

        assertTrue(cl.getExecutable().contains("rpmsign"), cl.getExecutable());
        assertTrue(Arrays.asList(cl.getArguments()).contains("--addsign"));
        Properties env = environment(cl);
        assertEquals("C", env.getProperty("LC_ALL"));
        assertEquals("C", env.getProperty("LANG"));
    }

    @Test
    void scriptStartsWithTheSleep() throws Exception {
        String script = new String(signer("secret".toCharArray()).writeExpectScriptFile(rpm), StandardCharsets.UTF_8);

        assertTrue(script.startsWith("sleep 1\n"), script);
        assertTrue(script.contains("\nset timeout -1\n"), script);
        assertTrue(script.contains("spawn rpm --define \"_gpg_name Demo Key\""), script);
        assertFalse(script.contains("-c"), script);
    }
}
