def rpm = new File(localRepositoryPath, "org/codehaus/mojo/rpm/its/rpm-1/1.0/rpm-1-1.0.rpm")
if (!rpm.exists())
    throw new AssertionError("RPM artifact not created: ${rpm.getAbsolutePath()}")

// Test that we actually filtered the files.
// This requires cpio version >= 2.10, which is missing in Mac OS X 10.8
proc = ["cpio", "--version"].execute()
proc.waitFor()
cpioVersion = (proc.in.text =~ /(\d+\.\d+)/)[0][1]

if (cpioVersion ==~ /2.1\d/ || cpioVersion ==~ /2.\d\d/) {

    proc = ["sh", "-c", "rpm2cpio ${rpm.getAbsolutePath()} | cpio -iv --to-stdout '.*filter.txt'"].execute()
    proc.waitFor()
    content = proc.in.text

    if (!"org.codehaus.mojo.rpm.its".equals(content))
        throw new java.lang.AssertionError("contents of filter.txt expected[org.codehaus.mojo.rpm.its] actual[${content}]");

    proc = ["sh", "-c", "rpm2cpio ${rpm.getAbsolutePath()} | cpio -iv --to-stdout '.*filter-version.txt'"].execute()
    proc.waitFor()
    content = proc.in.text

    if (!"1.0-1".equals(content))
        throw new java.lang.AssertionError("contents of filter-version.txt expected[1.0-1] actual[${content}]");
} else
    println "WARNING: cpio version < 2.10, skipping filter test. (Mac OS X?)"


lines = new File(basedir, "target/rpm/rpm-1/SPECS/rpm-1.spec").readLines()
[
    "Name: rpm-1",
    "Version: 1.0",
    "Release: 1",
    "License: (c) My self - 2013"
].each {
    if (!lines.contains(it))
        throw new AssertionError("Spec file missing \"${it}\"")
}

proc = ["rpm", "-qvlp", rpm.getAbsolutePath()].execute()
proc.waitFor()
lines = proc.in.text.readLines()

if (lines.size() != 15)
    throw new AssertionError("Incorrect file count: 15 != ${lines.size()}")

[
    /-rwxr-xr-x\s.*\smyuser\s+mygroup\s.*\s\/usr\/myusr\/app\/bin\/filter-version.txt/,
    /-rwxr-xr-x\s.*\smyuser\s+mygroup\s.*\s\/usr\/myusr\/app\/bin\/filter.txt/,
    /-rwxr-xr-x\s.*\smyuser\s+mygroup\s.*\s\/usr\/myusr\/app\/bin\/name-${System.getProperty("os.name")}.sh/,
    /-rwxr-xr-x\s.*\smyuser\s+mygroup\s.*\s\/usr\/myusr\/app\/bin\/name.sh/,
    /lrwxr.*\s.*\smyuser\s+mygroup\s.*\s\/usr\/myusr\/app\/bin\/oldname.sh -> name.sh/,
    /-rwxr-xr-x\s.*\smyuser\s+mygroup\s.*\s\/usr\/myusr\/app\/bin\/start.sh/,
    /l.*\s\/tmp\/myapp\/somefile2 -> \/tmp\/myapp\/somefile/,
    /-.*\s\/usr\/myusr\/app\/lib\/grizzly-comet-counter.war/
].each {
    if (!lines*.matches(it).contains(true))
        throw new AssertionError("File/dir/link matching ${it.toString()} missing from RPM!")
}

return true