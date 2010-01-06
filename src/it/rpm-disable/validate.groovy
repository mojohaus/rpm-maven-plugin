// $Id$
import org.codehaus.mojo.unix.rpm.RpmUtil
import org.codehaus.mojo.unix.rpm.RpmUtil.FileInfo
import org.codehaus.mojo.unix.rpm.RpmUtil.SpecFile

import java.util.List
import java.util.Iterator


//ATTACHED RPM5
File attachedRpm5 = new File((File) basedir, "target/rpm/project-rpm-1-attached-jre5/RPMS/noarch/project-rpm-1-1.1-1.noarch.rpm")

if (!attachedRpm5.exists())
    throw new java.lang.AssertionError("attachedRPM5 does not exist");

List fileInfos = RpmUtil.queryPackageForFileInfo(attachedRpm5)

boolean activation = false;
boolean mail = false;

for (Iterator i = fileInfos.iterator(); i.hasNext(); )
{
    FileInfo fileInfo = (FileInfo) i.next()
    
    //check for executable mode
    if (fileInfo.path.endsWith("activation-1.1.jar"))
    {
        activation = true;
    }
    else if (fileInfo.path.endsWith("mail-1.4.1.jar"))
    {
        mail = true;
    }
}

if (!activation)
    throw new java.lang.AssertionError("attachedRPM5 does not contain activation");

if (!mail)
    throw new java.lang.AssertionError("attachedRPM5 does not contain main");


//ATTACHED RPM6
File attachedRpm6 = new File((File) basedir, "target/rpm/project-rpm-1-attached-jre6/RPMS/noarch/project-rpm-1-1.1-1.noarch.rpm")

if (attachedRpm6.exists())
    throw new java.lang.AssertionError("attachedRPM6 should have been disabled");


//RPM5
File rpm5 = new File((File) basedir, "target/rpm/jre5/RPMS/noarch/jre5-1.1-1.noarch.rpm")

if (!rpm5.exists())
    throw new java.lang.AssertionError("rpm5 does not exist");

fileInfos = RpmUtil.queryPackageForFileInfo(rpm5)

activation = false;
mail = false;

for (Iterator i = fileInfos.iterator(); i.hasNext(); )
{
    FileInfo fileInfo = (FileInfo) i.next()
    
    //check for executable mode
    if (fileInfo.path.endsWith("activation-1.1.jar"))
    {
        activation = true;
    }
    else if (fileInfo.path.endsWith("mail-1.4.1.jar"))
    {
        mail = true;
    }
}

if (!activation)
    throw new java.lang.AssertionError("RPM5 does not contain activation");

if (!mail)
    throw new java.lang.AssertionError("RPM5 does not contain main");


//RPM6
File rpm6 = new File((File) basedir, "target/rpm/jre6/RPMS/noarch/jre6-1.1-1.noarch.rpm")

if (!rpm6.exists())
    throw new java.lang.AssertionError("rpm6 does not exist");

fileInfos = RpmUtil.queryPackageForFileInfo(rpm6)

activation = false;
mail = false;

for (Iterator i = fileInfos.iterator(); i.hasNext(); )
{
    FileInfo fileInfo = (FileInfo) i.next()
    
    //check for executable mode
    if (fileInfo.path.endsWith("activation-1.1.jar"))
    {
        activation = true;
    }
    else if (fileInfo.path.endsWith("mail-1.4.1.jar"))
    {
        mail = true;
    }
}


if (activation)
    throw new java.lang.AssertionError("rpm6 does contain activation");

if (!mail)
    throw new java.lang.AssertionError("rpm6 does not contain main");

return true;
