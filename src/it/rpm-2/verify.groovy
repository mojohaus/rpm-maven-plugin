// $Id$
import org.codehaus.mojo.unix.rpm.RpmUtil
import org.codehaus.mojo.unix.rpm.RpmUtil.FileInfo
import org.codehaus.mojo.unix.rpm.RpmUtil.SpecFile

import java.util.List
import java.util.Iterator

boolean success = true

File attachedRpm5 = new File((File) localRepositoryPath, "org/codehaus/mojo/rpm/its/rpm-2/1.0/rpm-2-1.0-attached-jre5.rpm")

success &= attachedRpm5.exists()

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

success &= activation;
success &= mail;


File attachedRpm6 = new File((File) localRepositoryPath, "org/codehaus/mojo/rpm/its/rpm-2/1.0/rpm-2-1.0-attached-jre6.rpm")

success &= attachedRpm6.exists()

fileInfos = RpmUtil.queryPackageForFileInfo(attachedRpm6)

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

success &= !activation;
success &= mail;


File primary = new File((File) localRepositoryPath, "org/codehaus/mojo/rpm/its/rpm-2/1.0/rpm-2-1.0.rpm")

// no primary rpm artifact should have been added
if (primary.exists())
{
    throw new java.lang.AssertionError("RPM incorrectly added as primary artifact");
}


File rpm5 = new File((File) basedir, "target/rpm/jre5/RPMS/noarch/jre5-1.0-1.noarch.rpm")

success &= rpm5.exists()

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

success &= activation;
success &= mail;


File rpm6 = new File((File) basedir, "target/rpm/jre6/RPMS/noarch/jre6-1.0-1.noarch.rpm")

success &= rpm6.exists()

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

success &= !activation;
success &= mail;

return success
