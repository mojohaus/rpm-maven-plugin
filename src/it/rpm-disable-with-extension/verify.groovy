File dir = new File(localRepositoryPath, "org/codehaus/mojo/rpm/its/disabled-with-extension/1.0-SNAPSHOT")

assert new File(dir, "disabled-with-extension-1.0-SNAPSHOT.pom").exists() : "POM should have been installed"
assert !new File(dir, "disabled-with-extension-1.0-SNAPSHOT.rpm").exists() : "RPM should not have been installed"
assert !new File(basedir, "target/rpm").exists() : "RPM build should have been disabled"

return true
