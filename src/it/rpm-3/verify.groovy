File rpm = new File(localRepositoryPath, "org/codehaus/mojo/rpm/its/rpm-3/1.0/rpm-3-1.0.rpm")

if (!rpm.exists())
    throw new AssertionError("${rpm.getAbsolutePath()} does not exist");

lines = new File(basedir, "target/rpm/rpm-3/SPECS/rpm-3.spec").readLines()
[
        "Name: rpm-3",
        "Version: 1.0",
        "Release: 1",
        "License: (c) me and myself 1945"
].each {
    if (!lines.contains(it))
        throw new AssertionError("Spec file missing \"${it}\"")
}

proc = ["rpm", "-qvlp", rpm.getAbsolutePath()].execute()
proc.waitFor()
lines = proc.in.text.readLines()

[
        /l.*\sroot\s+root\s.*\s\/etc\/init.d\/myapp -> \/usr\/myusr\/app\/bin\/start.sh/,
        /-rwxr-xr-x\s.*myuser\s+mygroup\s.*\s\/usr\/myusr\/app\/bin\/name-someOS.sh/,
        /-rwxr-xr-x\s.*myuser\s+mygroup\s.*\s\/usr\/myusr\/app\/bin\/name-somearch.sh/,
        /-rwxr-xr-x\s.*myuser\s+mygroup\s.*\s\/usr\/myusr\/app\/bin\/name.sh/,
        /lrwxr-xr-x\s.*myuser\s+mygroup\s.*\s\/usr\/myusr\/app\/bin\/oldname.sh -> \/usr\/myusr\/app\/bin\/name.sh/,
        /-rwxr-xr-x\s.*myuser\s+mygroup\s.*\s\/usr\/myusr\/app\/bin\/start.sh/,
        /drwxr-xr-x\s.*myuser\s+mygroup\s.*\s\/usr\/myusr\/app\/bin\/subdir/
].each {
    if (!lines*.matches(it).contains(true))
        throw new AssertionError("File/dir/link matching ${it.toString()} missing from RPM!")
}

[
        /.*\s\/usr\/myusr\/app\/bin\/name-linux.sh/,
        /.*\s\/usr\/myusr\/app\/bin\/name-x86.sh/
].each {
    if (lines*.matches(it).contains(true))
        throw new AssertionError("File matching ${it.toString()} should NOT be in the RPM!")
}

if (lines.size() != 17)
    throw new AssertionError("Expected: 17 file/dir/links but only got: ${lines.size()}")

return true