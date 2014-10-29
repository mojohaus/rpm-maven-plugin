File rpm = new File(localRepositoryPath, "org/codehaus/mojo/rpm/its/rpm-artifact/1.0/rpm-artifact-1.0-rpm.rpm")

if (!rpm.exists())
    throw new java.lang.AssertionError("${rpm.getAbsolutePath()} does not exist");

def lines = new File(basedir, "target/rpm/rpm-artifact/SPECS/rpm-artifact.spec").readLines()
[
        "Name: rpm-artifact",
        "Version: 1.0",
        "Release: 1"
].each {
    if (!lines.contains(it))
        throw new AssertionError("Spec file missing \"${it}\"")
}

def proc = ["rpm", "-qvlp", rpm.getAbsolutePath()].execute()
proc.waitFor()
lines = proc.in.text.readLines()

[
        /-.*\/usr\/myusr\/app\/lib\/rpm-artifact-1.0.jar/,
        /-.*\/usr\/myusr\/app\/lib\/rpm-artifact-1.0-sources.jar/,
        /-.*\/usr\/myusr\/app\/sources\/rpm-artifact-1.0-sources.jar/
].each {
    if (!lines*.matches(it).contains(true))
        throw new AssertionError("File/dir/link matching ${it.toString()} missing from RPM!")
}

if (lines.size() != 3)
    throw new AssertionError("Expected: 3 file/dir/links but only got: ${lines.size()}")

return true