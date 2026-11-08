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

import java.util.Date;

import org.apache.maven.plugin.AbstractMojo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VersionHelperTest {

    private static final Date BUILD_TIME = new Date(1_700_000_000_000L); // 2023-11-14T22:13:20Z

    private static final class StubMojo extends AbstractMojo implements VersionHelper.RPMVersionableMojo {
        private final String version;

        private final String release;

        private final boolean numericModifierAsRelease;

        StubMojo(String version, String release, boolean numericModifierAsRelease) {
            this.version = version;
            this.release = release;
            this.numericModifierAsRelease = numericModifierAsRelease;
        }

        public void execute() {}

        public String getVersion() {
            return version;
        }

        public String getRelease() {
            return release;
        }

        public Date getBuildTimestamp() {
            return BUILD_TIME;
        }

        public boolean isNumericModifierAsRelease() {
            return numericModifierAsRelease;
        }
    }

    private static VersionHelper.Version calculate(String version, String release, boolean numericAsRelease) {
        return new VersionHelper(new StubMojo(version, release, numericAsRelease)).calculateVersion();
    }

    private static void assertVersion(String version, String release, VersionHelper.Version actual) {
        assertEquals(version + '-' + release, actual.toString());
    }

    @Test
    void numericModifierGetsSuffixByDefault() {
        assertVersion("1.0.0", "1_1", calculate("1.0.0-1", null, false));
    }

    @Test
    void numericModifierIsTheReleaseWhenEnabled() {
        assertVersion("1.0.0", "1", calculate("1.0.0-1", null, true));
        assertVersion("1.0.0", "12", calculate("1.0.0-12", "", true));
    }

    @Test
    void nonNumericModifierIsUnaffectedByTheSetting() {
        assertVersion("1.0.0", "alpha_1_1", calculate("1.0.0-alpha-1", null, false));
        assertVersion("1.0.0", "alpha_1_1", calculate("1.0.0-alpha-1", null, true));
        assertVersion("1.0.0", "rc1_1", calculate("1.0.0-rc1", null, true));
    }

    @Test
    void snapshotGetsTheTimestampWhateverTheSetting() {
        assertVersion("1.0.0", "SNAPSHOT20231114221320", calculate("1.0.0-SNAPSHOT", null, false));
        assertVersion("1.0.0", "SNAPSHOT20231114221320", calculate("1.0.0-SNAPSHOT", null, true));
    }

    @Test
    void noModifierGivesReleaseOne() {
        assertVersion("1.0.0", "1", calculate("1.0.0", null, false));
        assertVersion("1.0.0", "1", calculate("1.0.0", null, true));
    }

    @Test
    void configuredReleaseWinsOverTheModifier() {
        assertVersion("1.0.0", "7", calculate("1.0.0-1", "7", false));
        assertVersion("1.0.0", "7", calculate("1.0.0-1", "7", true));
    }

    @Test
    void mojosDefaultToTheOldBehaviour() throws Exception {
        assertFalse(new RPMMojo(null, null).isNumericModifierAsRelease());
        assertFalse(new VersionMojo().isNumericModifierAsRelease());

        java.lang.reflect.Field field = VersionMojo.class.getDeclaredField("numericModifierAsRelease");
        field.setAccessible(true);
        VersionMojo mojo = new VersionMojo();
        field.set(mojo, true);
        assertTrue(mojo.isNumericModifierAsRelease());
    }
}
