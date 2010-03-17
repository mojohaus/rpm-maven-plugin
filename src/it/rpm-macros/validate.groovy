// $Id$
import org.codehaus.mojo.unix.rpm.RpmUtil
import org.codehaus.mojo.unix.rpm.RpmUtil.FileInfo
import org.codehaus.mojo.unix.rpm.RpmUtil.SpecFile

import java.io.*
import java.util.List
import java.util.Iterator

boolean success = true

File rpm = new File((File) basedir, "target/rpm/rpm-macros/RPMS/noarch/rpm-macros-1.0-1.noarch.rpm")

if (!rpm.exists())
{
    throw new java.lang.AssertionError(rpm.getAbsolutePath() + " does not exist");
}

SpecFile spec = RpmUtil.getSpecFileFromRpm(rpm)

if (!spec.name.equals("rpm-macros"))
    throw new java.lang.AssertionError("spec name");
if (!spec.version.equals("1.0"))
    throw new java.lang.AssertionError("spec version");
if (spec.release != 1)
    throw new java.lang.AssertionError("spec release");

List fileInfos = RpmUtil.queryPackageForFileInfo(rpm)

int fileCnt = fileInfos.size()
System.out.println("File Count: " + fileCnt);
System.out.println(fileInfos);
if (fileCnt != 5)
    throw new java.lang.AssertionError("file count");
    

for (Iterator i = fileInfos.iterator(); i.hasNext();)
{
    FileInfo fileInfo = (FileInfo) i.next()
        
}
    
return true
