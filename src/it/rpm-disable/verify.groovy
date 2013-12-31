def attachedRpm5 = new File(basedir, "target/rpm/rpm-disable-attached-jre5/RPMS/noarch/rpm-disable-1.0-1.noarch.rpm")
if (!attachedRpm5.exists())
{
    throw new java.lang.AssertionError("attached-jre5 RPM not created");
}

def attachedRpm6 = new File(basedir, "target/rpm/rpm-disable-attached-jre6/RPMS/noarch/rpm-disable-1.0-1.noarch.rpm")
if (attachedRpm6.exists())
{
    throw new java.lang.AssertionError("attached-jre6 RPM should have been disabled");
}

return true;
