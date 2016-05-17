File rpm = new File(localRepositoryPath, "org/codehaus/mojo/rpm/its/rpm-skip-poms/1.0/rpm-skip-poms-1.0.rpm")

if (rpm.exists())
    throw new java.lang.AssertionError("${rpm.getAbsolutePath()} should not exist");

return true