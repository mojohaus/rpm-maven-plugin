File rpm = new File(basedir, "rpm-rpm/target/rpm/rpm-dependency-based-requires-rpm-rpm/RPMS/noarch/rpm-dependency-based-requires-rpm-rpm-1.0-rel.noarch.rpm")
if (!rpm.exists())
    throw new AssertionError("rpm file does not exist: ${rpm.getAbsolutePath()}")

def proc = ["rpm", "-qpR", rpm.getAbsolutePath()].execute()
proc.waitFor()
if (!proc.in.text.contains("rpm-dependency-based-requires-rpm-dep >= 8.1-rel"))
    throw new AssertionError("Dependency to rpm-dependency-based-requires-rpm-dep missing!")

return true
