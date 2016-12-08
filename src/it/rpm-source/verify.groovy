File rpm = new File(basedir, "target/rpm/rpm-source/RPMS/noarch/rpm-source-1.0-1.noarch.rpm")

if (!rpm.exists())
    throw new AssertionError("${rpm.getAbsolutePath()} does not exist");

File srpm = new File(basedir, "target/rpm/rpm-source/SRPMS/rpm-source-1.0-1.src.rpm")

if (!srpm.exists())
    throw new AssertionError("${srpm.getAbsolutePath()} does not exist");

return true
