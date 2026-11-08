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
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AttachSpecTest {

    private final List<Object[]> attachments = new ArrayList<>();

    private final MavenProject project = new MavenProject();

    private RPMMojo mojo(Boolean attachSpec) throws Exception {
        MavenProjectHelper helper = (MavenProjectHelper) Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class<?>[] {MavenProjectHelper.class}, (proxy, method, args) -> {
                    if (method.getName().equals("attachArtifact")) {
                        attachments.add(args);
                    }
                    return null;
                });

        RPMMojo mojo = new RPMMojo(null, null);
        set(mojo, "name", "demo");
        set(mojo, "workarea", new File("target/rpm/demo"));
        set(mojo, "project", project);
        set(mojo, "mavenProjectHelper", helper);
        if (attachSpec != null) {
            set(mojo, "attachSpec", attachSpec);
        }
        return mojo;
    }

    private static void set(Object target, String field, Object value) throws Exception {
        Field f = AbstractRPMMojo.class.getDeclaredField(field);
        f.setAccessible(true);
        f.set(target, value);
    }

    @Test
    void attachesTheSpecFileWithTypeSpecAndClassifierRpmSpec() throws Exception {
        mojo(true).attachSpecFile();

        assertEquals(1, attachments.size());
        Object[] args = attachments.get(0);
        assertSame(project, args[0]);
        assertEquals("spec", args[1]);
        assertEquals("rpm-spec", args[2]);
        assertEquals(new File(new File("target/rpm/demo", "SPECS"), "demo.spec"), args[3], Arrays.toString(args));
    }

    @Test
    void attachesNothingByDefault() throws Exception {
        mojo(null).attachSpecFile();

        assertTrue(attachments.isEmpty());
    }

    @Test
    void attachesNothingWhenDisabled() throws Exception {
        mojo(false).attachSpecFile();

        assertTrue(attachments.isEmpty());
    }
}
