File rpm = new File(basedir, "target/rpm/rpm-macros/RPMS/noarch/rpm-macros-1.0-1.noarch.rpm")

if (!rpm.exists())
    throw new AssertionError("${rpm.getAbsolutePath()} does not exist");

lines = new File(basedir, "target/rpm/rpm-macros/SPECS/rpm-macros.spec").readLines()
[
        "Name: rpm-macros",
        "Version: 1.0",
        "Release: 1"
].each {
    if (!lines.contains(it))
        throw new AssertionError("Spec file missing \"${it}\"")
}

def proc = "rpm --eval %_datadir".execute()
proc.waitFor()
datadirpath = proc.in.text.trim().replace("/", "\\/")

proc = ["rpm", "-qvlp", rpm.getAbsolutePath()].execute()
proc.waitFor()
lines = proc.in.text.readLines()

[
        /l.*${datadirpath}\/app2 -> (\/usr\/mygrp\/app|\.\.\/mygrp\/app)/,
        /d.*\/usr\/mygrp/,
        /d.*\/usr\/mygrp\/app\/lib/,
        /-.*\/usr\/mygrp\/app\/lib\/grizzly-comet-counter.war/,
        /-.*\/usr\/mygrp\/app\/somefile/
].each {
    if (!lines*.matches(it).contains(true))
        throw new AssertionError("File/dir/link matching ${it.toString()} missing from RPM!")
}

if (lines.size() != 5)
    throw new AssertionError("Expected: 5 file/dir/links but only got: ${lines.size()}")

return true