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
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs the file installation and the spec writer of an {@code rpm} goal on a project directory, without
 * {@code rpmbuild}, and checks the spec file. These cases used to be integration tests that read the package with
 * {@code rpm -qvlp}.
 */
class SpecWriterMappingsTest {

    @TempDir
    Path tmp;

    private RPMMojo mojo;

    private Path basedir;

    private MavenProject project;

    private RPMMojo newMojo(String name, String version) throws Exception {
        basedir = Files.createDirectories(tmp.resolve("project"));
        project = new MavenProject();
        project.setFile(basedir.resolve("pom.xml").toFile());
        Path workarea = Files.createDirectories(tmp.resolve("rpm").resolve(name));
        mojo = new RPMMojo(null, null);
        set("name", name);
        set("projversion", version);
        set("release", "1");
        set("project", project);
        set("workarea", workarea.toFile());
        set(
                "buildroot",
                Files.createDirectories(workarea.resolve("tmp-buildroot")).toFile());
        set("rpmBuildroot", workarea.resolve("buildroot").toFile());
        set("targetArch", "noarch");
        set("targetOS", "linux");
        set("attachedArtifacts", new ArrayList<Artifact>());
        return mojo;
    }

    private void set(String field, Object value) throws Exception {
        Field f = AbstractRPMMojo.class.getDeclaredField(field);
        f.setAccessible(true);
        f.set(mojo, value);
    }

    @SuppressWarnings("unchecked")
    private void defineMacro(String name, String value) throws Exception {
        Field f = AbstractRPMMojo.class.getDeclaredField("macroKeyToValue");
        f.setAccessible(true);
        ((Map<String, String>) f.get(mojo)).put(name, value);
    }

    private String install(Mapping... mappings) throws Exception {
        set("mappings", Arrays.asList(mappings));
        new FileHelper(mojo, new FilteringDirectoryArchiver()).installFiles();
        StringWriter out = new StringWriter();
        try (PrintWriter spec = new PrintWriter(out)) {
            new SpecWriter(mojo, spec).writeSpecFile();
        }
        return out.toString();
    }

    private Path file(String path) throws Exception {
        Path file = basedir.resolve(path);
        Files.createDirectories(file.getParent());
        return Files.write(file, new byte[] {'x'});
    }

    private Artifact artifact(String groupId, String artifactId, String version, String type, String classifier)
            throws Exception {
        Artifact artifact = new DefaultArtifact(
                groupId,
                artifactId,
                VersionRange.createFromVersion(version),
                "compile",
                type,
                classifier,
                new DefaultArtifactHandler(type));
        Path repo = Files.createDirectories(tmp.resolve("repo"));
        String fileName = artifactId + '-' + version + (classifier == null ? "" : '-' + classifier) + '.' + type;
        artifact.setFile(Files.write(repo.resolve(fileName), new byte[] {'x'}).toFile());
        return artifact;
    }

    private static Mapping mapping(String directory) {
        Mapping mapping = new Mapping();
        mapping.setDirectory(directory);
        return mapping;
    }

    private static Source source(String location, String destination) {
        Source source = new Source();
        source.setLocation(location);
        source.setDestination(destination);
        return source;
    }

    private static SoftlinkSource link(String location, String destination) {
        SoftlinkSource link = new SoftlinkSource();
        link.setLocation(location);
        link.setDestination(destination);
        return link;
    }

    /** The entries of the %files section, sorted because the scanner does not promise an order. */
    private static List<String> filesSection(String spec) {
        List<String> lines = Arrays.asList(spec.split("\n"));
        List<String> files = new ArrayList<>();
        for (String line : lines.subList(lines.indexOf("%files") + 2, lines.size())) {
            if (!line.trim().isEmpty()) {
                files.add(line);
            }
        }
        Collections.sort(files);
        return files;
    }

    /** The installed files, relative to the build root, as the spec file will find them. */
    private List<String> installedFiles() throws Exception {
        Path root = mojo.getBuildroot().toPath();
        try (Stream<Path> walk = Files.walk(root)) {
            return walk.filter(Files::isRegularFile)
                    .map(f -> root.relativize(f).toString().replace(File.separatorChar, '/'))
                    .sorted()
                    .collect(Collectors.toList());
        }
    }

    private static List<String> sorted(String... lines) {
        List<String> list = new ArrayList<>(Arrays.asList(lines));
        Collections.sort(list);
        return list;
    }

    private static Dependency dependency(boolean stripVersion, String... includes) throws Exception {
        Dependency dependency = new Dependency();
        dependency.setStripVersion(stripVersion);
        if (includes.length > 0) {
            dependency.setIncludes(Arrays.asList(includes));
        }
        return dependency;
    }

    private static Source filtered(Source source, String targetOSName, String targetArchitecture) {
        source.setTargetOSName(targetOSName);
        source.setTargetArchitecture(targetArchitecture);
        return source;
    }

    /** The project of the former {@code rpm-3} integration test. */
    private String installApplication() throws Exception {
        newMojo("rpm-3", "1.0");
        set("targetArch", "somearch");
        set("targetOS", "someOS");
        set("defaultUsername", "myuser");
        set("defaultGroupname", "mygroup");
        set("license", "(c) me and myself 1945");
        file("src/main/bin/start.sh");
        file("src/main/bin/subdir/sub.file");
        file("src/main/bin2/rename.sh");
        Set<Artifact> artifacts = new LinkedHashSet<>();
        artifacts.add(artifact("org.apache.tomcat", "juli", "6.0.16", "jar", null));
        artifacts.add(artifact("org.apache.tomcat", "servlet-api", "6.0.16", "jar", null));
        artifacts.add(artifact("com.sun.grizzly.samples", "grizzly-comet-counter", "2.2.5", "war", null));
        project.setArtifacts(artifacts);

        Mapping app = mapping("/usr/myusr/app");
        Mapping app2 = mapping("/usr/myusr/app2");
        app2.setSources(Collections.singletonList(link("/usr/myusr/app", null)));
        Mapping myusr = mapping("/usr/myusr");
        myusr.setSources(Collections.singletonList(link("/usr/myusr/app", "app3")));
        Mapping initd = mapping("/etc/init.d");
        initd.setDirectoryIncluded(false);
        initd.setUsername("root");
        initd.setGroupname("root");
        initd.setSources(Collections.singletonList(link("/usr/myusr/app/bin/start.sh", "myapp")));
        Mapping lib = mapping("/usr/myusr/app/lib");
        lib.setDependency(dependency(false, "com.sun.grizzly.samples:grizzly-comet-counter"));
        Mapping lib2 = mapping("/usr/myusr/app/lib2");
        lib2.setDependency(dependency(false, "org.apache.tomcat:*"));
        Mapping bin = mapping("/usr/myusr/app/bin");
        bin.setFilemode("755");
        bin.setRecurseDirectories(true);
        Source start = new Source();
        start.setLocation("src/main/bin");
        start.setExcludes(Collections.singletonList("*install.sh"));
        bin.setSources(Arrays.asList(
                source("src/main/bin2/rename.sh", "name.sh"),
                start,
                filtered(source("src/main/bin2/rename.sh", "name-someOS.sh"), ".*OS", null),
                filtered(source("src/main/bin2/rename.sh", "name-linux.sh"), "linux", null),
                filtered(source("src/main/bin2/rename.sh", "name-somearch.sh"), null, ".*arch"),
                filtered(source("src/main/bin2/rename.sh", "name-x86.sh"), null, "i386"),
                link("/usr/myusr/app/bin/name.sh", "oldname.sh")));
        return install(app, app2, myusr, initd, lib, lib2, bin);
    }

    @Test
    void applicationSpecHasTheHeaderOfTheProject() throws Exception {
        List<String> spec = Arrays.asList(installApplication().split("\n"));

        for (String line : new String[] {
            "Name: rpm-3", "Version: 1.0", "Release: 1", "License: (c) me and myself 1945",
        }) {
            assertTrue(spec.contains(line), "Spec file missing \"" + line + "\": " + spec);
        }
    }

    @Test
    void applicationLinksAreCreatedInTheInstallSection() throws Exception {
        String spec = installApplication();

        for (String link : new String[] {
            "ln -s /usr/myusr/app/bin/start.sh $RPM_BUILD_ROOT/etc/init.d/myapp",
            "ln -s /usr/myusr/app/bin/name.sh $RPM_BUILD_ROOT/usr/myusr/app/bin/oldname.sh",
            "ln -s /usr/myusr/app $RPM_BUILD_ROOT/usr/myusr/app2",
            "ln -s /usr/myusr/app $RPM_BUILD_ROOT/usr/myusr/app3",
        }) {
            assertTrue(spec.contains("\n" + link + "\n"), "missing: " + link + "\n" + spec);
        }
    }

    @Test
    void applicationFilesSectionOwnsEverythingWithTheMappingAttributes() throws Exception {
        String spec = installApplication();

        assertEquals(
                sorted(
                        "%dir  \"/usr/myusr/app\"",
                        " \"/usr/myusr/app2\"",
                        "  \"/usr/myusr/app3\"",
                        "%attr(-,root,root)  \"/etc/init.d/myapp\"",
                        " \"/usr/myusr/app/lib\"",
                        " \"/usr/myusr/app/lib2\"",
                        "%dir %attr(755,myuser,mygroup)  \"/usr/myusr/app/bin/\"",
                        "%dir %attr(755,myuser,mygroup)  \"/usr/myusr/app/bin/subdir\"",
                        "%attr(755,myuser,mygroup)  \"/usr/myusr/app/bin/name.sh\"",
                        "%attr(755,myuser,mygroup)  \"/usr/myusr/app/bin/name-someOS.sh\"",
                        "%attr(755,myuser,mygroup)  \"/usr/myusr/app/bin/name-somearch.sh\"",
                        "%attr(755,myuser,mygroup)  \"/usr/myusr/app/bin/start.sh\"",
                        "%attr(755,myuser,mygroup)  \"/usr/myusr/app/bin/subdir/sub.file\"",
                        "%attr(755,myuser,mygroup)  \"/usr/myusr/app/bin/oldname.sh\""),
                filesSection(spec),
                spec);
        assertTrue(spec.contains("%defattr(-,myuser,mygroup,-)\n"), spec);
    }

    @Test
    void applicationSourcesAreFilteredByTargetOsAndArchitecture() throws Exception {
        installApplication();

        // name-linux.sh and name-x86.sh do not match target OS someOS and architecture somearch
        assertEquals(
                sorted(
                        "usr/myusr/app/bin/name-someOS.sh",
                        "usr/myusr/app/bin/name-somearch.sh",
                        "usr/myusr/app/bin/name.sh",
                        "usr/myusr/app/bin/start.sh",
                        "usr/myusr/app/bin/subdir/sub.file",
                        "usr/myusr/app/lib/grizzly-comet-counter-2.2.5.war",
                        "usr/myusr/app/lib2/juli-6.0.16.jar",
                        "usr/myusr/app/lib2/servlet-api-6.0.16.jar"),
                installedFiles());
    }

    /** The project of the former {@code rpm-artifact} integration test. */
    private String installArtifacts() throws Exception {
        newMojo("rpm-artifact", "1.0");
        set("defaultUsername", "myuser");
        set("defaultGroupname", "mygroup");
        set("artifact", artifact("org.codehaus.mojo.rpm.its", "rpm-artifact", "1.0", "jar", null));
        set(
                "attachedArtifacts",
                new ArrayList<>(Collections.singletonList(
                        artifact("org.codehaus.mojo.rpm.its", "rpm-artifact", "1.0", "jar", "sources"))));

        Mapping lib = mapping("/usr/myusr/app/lib");
        lib.setDirectoryIncluded(false);
        lib.setArtifact(new ArtifactMap());
        Mapping sources = mapping("/usr/myusr/app/sources");
        sources.setDirectoryIncluded(false);
        ArtifactMap sourcesOnly = new ArtifactMap();
        sourcesOnly.setClassifiers(Collections.singletonList("sources"));
        sources.setArtifact(sourcesOnly);
        return install(lib, sources);
    }

    @Test
    void artifactMappingWithoutClassifiersTakesTheMainAndAllAttachedArtifacts() throws Exception {
        String spec = installArtifacts();

        assertEquals(
                sorted(
                        "  \"/usr/myusr/app/lib/rpm-artifact-1.0.jar\"",
                        "  \"/usr/myusr/app/lib/rpm-artifact-1.0-sources.jar\"",
                        "  \"/usr/myusr/app/sources/rpm-artifact-1.0-sources.jar\""),
                filesSection(spec),
                spec);
        assertEquals(
                sorted(
                        "usr/myusr/app/lib/rpm-artifact-1.0.jar",
                        "usr/myusr/app/lib/rpm-artifact-1.0-sources.jar",
                        "usr/myusr/app/sources/rpm-artifact-1.0-sources.jar"),
                installedFiles());
    }

    @Test
    void artifactSpecHasTheHeaderOfTheProject() throws Exception {
        List<String> spec = Arrays.asList(installArtifacts().split("\n"));

        for (String line : new String[] {"Name: rpm-artifact", "Version: 1.0", "Release: 1"}) {
            assertTrue(spec.contains(line), "Spec file missing \"" + line + "\": " + spec);
        }
    }

    /** The project of the former {@code rpm-macros} integration test. */
    private String installWithMacros() throws Exception {
        newMojo("rpm-macros", "1.0");
        set(
                "defineStatements",
                new ArrayList<>(Arrays.asList("_unpackaged_files_terminate_build 0", "_some_cool_dir /usr/mygrp")));
        Method process = AbstractRPMMojo.class.getDeclaredMethod("processDefineStatements");
        process.setAccessible(true);
        process.invoke(mojo);
        // rpm evaluates the macros that the project does not define; the stand-in value is the one of Linux
        defineMacro("_datadir", "/usr/share");
        set("defaultUsername", "myuser");
        set("defaultGroupname", "mygroup");
        file("src/main/bin/start.sh");
        Set<Artifact> artifacts = new LinkedHashSet<>();
        artifacts.add(artifact("com.sun.grizzly.samples", "grizzly-comet-counter", "2.2.5", "war", null));
        project.setArtifacts(artifacts);

        Mapping app = mapping("%{_some_cool_dir}/app");
        app.setSources(Collections.singletonList(source("src/main/bin/start.sh", "somefile")));
        Mapping lib = mapping("%{_some_cool_dir}/app/lib");
        lib.setDependency(dependency(true));
        Mapping share = mapping("%{_datadir}/app2");
        share.setSources(Collections.singletonList(link("%{_some_cool_dir}/app", null)));
        return install(mapping("%{_some_cool_dir}"), app, lib, share);
    }

    @Test
    void macroDirectoriesStayMacrosInTheSpecAndAreEvaluatedForTheFiles() throws Exception {
        String spec = installWithMacros();

        assertTrue(spec.contains("\n%define _some_cool_dir /usr/mygrp\n"), spec);
        assertEquals(
                sorted(
                        "%dir  \"%{_some_cool_dir}\"",
                        "  \"%{_some_cool_dir}/app/somefile\"",
                        " \"%{_some_cool_dir}/app/lib\"",
                        " \"%{_datadir}/app2\""),
                filesSection(spec),
                spec);
        assertTrue(spec.contains("\nln -s %{_some_cool_dir}/app $RPM_BUILD_ROOT/%{_datadir}/app2\n"), spec);
        assertEquals(sorted("usr/mygrp/app/lib/grizzly-comet-counter.war", "usr/mygrp/app/somefile"), installedFiles());
    }

    @Test
    void macroSpecHasTheHeaderOfTheProject() throws Exception {
        List<String> spec = Arrays.asList(installWithMacros().split("\n"));

        for (String line : new String[] {"Name: rpm-macros", "Version: 1.0", "Release: 1"}) {
            assertTrue(spec.contains(line), "Spec file missing \"" + line + "\": " + spec);
        }
    }
}
