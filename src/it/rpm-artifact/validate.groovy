// $Id$
import org.codehaus.mojo.unix.rpm.RpmUtil
import org.codehaus.mojo.unix.rpm.RpmUtil.FileInfo
import org.codehaus.mojo.unix.rpm.RpmUtil.SpecFile

import java.util.List
import java.util.Iterator


File rpm = new File((File) basedir, "target/rpm/mojo-rpm-it-artifact/RPMS/noarch/mojo-rpm-it-artifact-1.0-1.noarch.rpm")

if (!rpm.exists())
{
    throw new java.lang.AssertionError(rpm.getAbsolutePath() + " does not exist");
}

SpecFile spec = RpmUtil.getSpecFileFromRpm(rpm)

if (!spec.name.equals("mojo-rpm-it-artifact"))
    throw new java.lang.AssertionError("spec name");
if (!spec.version.equals("1.0"))
    throw new java.lang.AssertionError("spec version");
if (spec.release != 1)
    throw new java.lang.AssertionError("spec release");

List fileInfos = RpmUtil.queryPackageForFileInfo(rpm)

int fileCnt = fileInfos.size()
System.out.println("File Count: " + fileCnt);
System.out.println(fileInfos);
if (fileCnt != 3)
    throw new java.lang.AssertionError("number of files in rpm: " + fileCnt + "does not match expected: 3");

boolean foundLibJar = false;
boolean foundLibSource = false;
boolean foundSource = false;

for (Iterator i = fileInfos.iterator(); i.hasNext();)
{
    FileInfo fileInfo = (FileInfo) i.next()
    
    if ("/usr/myusr/app/lib/mojo-rpm-it-artifact-1.0.jar".equals(fileInfo.path))
    {
        foundLibJar = true;
    }
    else if ("/usr/myusr/app/lib/mojo-rpm-it-artifact-1.0-sources.jar".equals(fileInfo.path))
    {
        foundLibSource = true;
    }
    else if ("/usr/myusr/app/sources/mojo-rpm-it-artifact-1.0-sources.jar".equals(fileInfo.path))
    {
        foundSource = true;
    }
}

if (!foundLibJar)
    throw new java.lang.AssertionError("jar artifact not present in lib folder");

if (!foundLibSource)
    throw new java.lang.AssertionError("sources jar secondary artifact not present in lib folder");

if (!foundSource)
    throw new java.lang.AssertionError("sources jar secondary artifact not present in sources folder");

return true
