import org.codehaus.mojo.unix.rpm.RpmUtil
import org.codehaus.mojo.unix.rpm.RpmUtil.FileInfo
import org.codehaus.mojo.unix.rpm.RpmUtil.SpecFile

import java.util.List
import java.util.Iterator

boolean success = true

File rpm = new File((File) basedir, "target/rpm/RPMS/noarch/project-rpm-1-1.1-1.noarch.rpm")

success &= rpm.exists()

SpecFile spec = RpmUtil.getSpecFileFromRpm(rpm)

success &= spec.name.equals("project-rpm-1")
success &= spec.version.equals("1.1")
success &= spec.release == 1
success &= spec.license.equals("2009 my org")

List fileInfos = RpmUtil.queryPackageForFileInfo(rpm)

int fileCnt = fileInfos.size()
success &= fileCnt == 8

for (Iterator i = fileInfos.iterator(); i.hasNext();)
{
    FileInfo fileInfo = (FileInfo) i.next()
    success &= fileInfo.user.equals("myuser")
    success &= fileInfo.group.equals("mygroup")
    
    //check for executable mode
    if (fileInfo.path.startsWith("/usr/myusr/app/bin/"))
    {
        success &= fileInfo.mode.equals("-rwxr-xr-x")
    }
}

return success
