assert new File(basedir, "target/rpm/rpm-disable-attached-jre5/RPMS/noarch/rpm-disable-1.0-1.noarch.rpm").exists() : "attached-jre5 RPM not created"
assert !new File(basedir, "target/rpm/rpm-disable-attached-jre6/RPMS/noarch/rpm-disable-1.0-1.noarch.rpm").exists() : "attached-jre6 RPM should have been disabled"

return true