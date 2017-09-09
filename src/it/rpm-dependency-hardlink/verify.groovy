File rpm = new File(basedir, "target/rpm/rpm-dependency-hardlink-module-rpm/RPMS/noarch/rpm-dependency-hardlink-module-rpm-1.0-rel.noarch.rpm")
if (!rpm.exists())
    throw new AssertionError("rpm file does not exist: ${rpm.getAbsolutePath()}")

def proc = ["rpm", "-qlp", rpm.getAbsolutePath()].execute()
proc.waitFor()
proc.in.text.eachLine {
    if (!it.equals("/usr/myusr/app/log4j-1.2.14.jar"))
        throw new AssertionError("rpm.dependency-hardlink log4j-1.2.14.jar missing!")
}

return true
