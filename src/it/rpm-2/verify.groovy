def checkRpm(rpm, files, excludedFiles) {

    proc = ["rpm", "-qvlp", rpm.getAbsolutePath()].execute()
    proc.waitFor()
    lines = proc.in.text.readLines()

    files.each {
        if (!lines*.matches(it).contains(true))
            throw new AssertionError("File/dir/link matching ${it.toString()} missing from RPM!")
    }

    excludedFiles.each {
        if (lines*.matches(it).contains(true))
            throw new AssertionError("File/dir/link matching ${it.toString()} should NOT be in RPM!")
    }
}

File attachedRpm5 = new File(localRepositoryPath, "org/codehaus/mojo/rpm/its/rpm-2/1.0/rpm-2-1.0-attached-jre5.rpm")
assert attachedRpm5.exists()
checkRpm(attachedRpm5, [/-.*\/usr\/myusr\/app\/lib\/activation-1.1.jar/, /-.*\/usr\/myusr\/app\/lib\/mail-1.4.1.jar/], [])

File attachedRpm6 = new File(localRepositoryPath, "org/codehaus/mojo/rpm/its/rpm-2/1.0/rpm-2-1.0-attached-jre6.rpm")
assert attachedRpm6.exists()
checkRpm(attachedRpm6, [/-.*\/usr\/myusr\/app\/lib\/mail-1.4.1.jar/], [/-.*\/usr\/myusr\/app\/lib\/activation-1.1.jar/])
File primary = new File(localRepositoryPath, "org/codehaus/mojo/rpm/its/rpm-2/1.0/rpm-2-1.0.rpm")
assert !primary.exists(): "RPM incorrectly added as primary artifact: ${primary.getAbsolutePath()}"

File rpm5 = new File(basedir, "target/rpm/jre5/RPMS/noarch/jre5-1.0-1.noarch.rpm")
assert rpm5.exists()
checkRpm(rpm5, [/-.*\/usr\/myusr\/app\/lib\/activation-1.1.jar/, /-.*\/usr\/myusr\/app\/lib\/mail-1.4.1.jar/], [])

File rpm6 = new File(basedir, "target/rpm/jre6/RPMS/noarch/jre6-1.0-1.noarch.rpm")
assert rpm6.exists()
checkRpm(rpm6, [/-.*\/usr\/myusr\/app\/lib\/mail-1.4.1.jar/], [/-.*\/usr\/myusr\/app\/lib\/activation-1.1.jar/])

return true