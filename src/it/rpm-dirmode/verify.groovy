File rpm = new File(localRepositoryPath, "org/codehaus/mojo/rpm/its/rpm-dirmode/0.0.1-SNAPSHOT/rpm-dirmode-0.0.1-SNAPSHOT.rpm")

if (!rpm.exists())
    throw new AssertionError("${rpm.getAbsolutePath()} does not exist");

lines = new File(basedir, "target/rpm/rpm-dirmode/SPECS/rpm-dirmode.spec").readLines()
[
        "Name: rpm-dirmode",
        "Version: 0.0.1"
].each {
    if (!lines.contains(it))
        throw new AssertionError("Spec file missing \"${it}\"")
}

proc = ["rpm", "-qvlp", rpm.getAbsolutePath()].execute()
proc.waitFor()
lines = proc.in.text.readLines()

[
        // Explicitly specified directory
        /drwxrwxr-x\s.+tu01\s+tg01\s.*\s\/opt\/dirmode$/,
        // Files and directories mapped into the directory above via a separate directoryIncluded=false mapping
        /-rw-rw-r--\s.+tu01\s+tg01\s.*\s\/opt\/dirmode\/top_level.txt$/,
        /drwxrwxr-x\s.+tu01\s+tg01\s.*\s\/opt\/dirmode\/subdir\/deepdir\/finaldir$/,
        /-rw-rw-r--\s.+tu01\s+tg01\s.*\s\/opt\/dirmode\/subdir\/deepdir\/finaldir\/last_file.txt$/,
        // directory mapping without a dirmode (fallback to filemode)
        /drwxrwx---\s.+tu01\s+tg01\s.*\s\/opt\/dirmode\/t1$/,
        // directory mapping that uses default ownership/permission
        /drwxr-x-wx\s.+du02\s+dg02\s.*\s\/opt\/dirmode\/t2$/,
        // mapping where the directory is included (no separate directory-only mapping)
        /drwxrwxr-x\s.+tu01\s+tg01\s.*\s\/var\/opt\/dirmode$/,
        /drwxrwxr-x\s.+tu01\s+tg01\s.*\s\/var\/opt\/dirmode\/subdir\/deepdir\/finaldir$/,
        /-rw-rw-r--\s.+tu01\s+tg01\s.*\s\/var\/opt\/dirmode\/subdir\/deepdir\/finaldir\/last_file.txt$/,
        // For this variant, the 'simpleDir' optimization gets applied.
        // The spec-file only contains a single entry for the top dir (instead of one entry per file/directory).
        // The directory permissions for that case are NOT CORRECT. Not sure how to fix.
        /drw-rw-r--\s.+tu01\s+tg01\s.*\s\/var\/tmp\/dirmode$/,
        /drw-rw-r--\s.+tu01\s+tg01\s.*\s\/var\/tmp\/dirmode\/subdir\/deepdir\/finaldir$/,
        /-rw-rw-r--\s.+tu01\s+tg01\s.*\s\/var\/tmp\/dirmode\/subdir\/deepdir\/finaldir\/last_file.txt$/
].each {
    if (!lines*.matches(it).contains(true))
        throw new AssertionError("File/dir/link matching ${it.toString()} missing from RPM!")
}

[
        // Ensure that we don't have duplicates of the directory that uses default ownership/permission
        /d.........\s.+tu01\s+tg01\s.*\s\/opt\/dirmode\/t2/,

        // Ensure that we don't have duplicates of certain directories without the executable bit
        /d..-..-.--\s.+\s\/opt\/dirmode$/,
        /d..-..-.--\s.+\s\/opt\/dirmode\/subdir\/deepdir\/finaldir$/,
].each {
    if (lines*.matches(it).contains(true))
        throw new AssertionError("File matching ${it.toString()} should NOT be in the RPM!")
}

if (lines.size() != 38)
    throw new AssertionError("Expected: 38 file/dir/links but got: ${lines.size()}")

return true