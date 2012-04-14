// $Id$


File attachedRpm5 = new File((File) basedir, "target/rpm/rpm-disable-attached-jre5/RPMS/noarch/rpm-disable-1.0-1.noarch.rpm")
if (!attachedRpm5.exists())
{
    throw new java.lang.AssertionError("attached-jre5 RPM not created");
}

File attachedRpm6 = new File((File) basedir, "target/rpm/rpm-disable-attached-jre6/RPMS/noarch/rpm-disable-1.0-1.noarch.rpm")
if (!attachedRpm6.exists())
{
    throw new java.lang.AssertionError("attached-jre6 RPM should have been disabled");
}

return true;
