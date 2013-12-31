import org.codehaus.mojo.unix.rpm.RpmUtil
import org.codehaus.mojo.unix.rpm.RpmUtil.FileInfo
import org.codehaus.mojo.unix.rpm.RpmUtil.SpecFile

import java.util.List
import java.util.Iterator


File rpm = new File((File) localRepositoryPath, "org/codehaus/mojo/rpm/its/rpm-artifact/1.0/rpm-artifact-1.0-rpm.rpm")

if (!rpm.exists())
{
    throw new java.lang.AssertionError(rpm.getAbsolutePath() + " does not exist");
}

SpecFile spec = RpmUtil.getSpecFileFromRpm(rpm)

if (!spec.name.equals("rpm-artifact"))
    throw new java.lang.AssertionError("spec name ('rpm-artifact' != '" + spec.name + "')");
if (!spec.version.equals("1.0"))
    throw new java.lang.AssertionError("spec version ('1.0' != '" + spec.version + "')");
if (spec.release != 1)
    throw new java.lang.AssertionError("spec release ('1' != '" + spec.release +  "')");

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
    
    if ("/usr/myusr/app/lib/rpm-artifact-1.0.jar".equals(fileInfo.path))
    {
        foundLibJar = true;
    }
    else if ("/usr/myusr/app/lib/rpm-artifact-1.0-sources.jar".equals(fileInfo.path))
    {
        foundLibSource = true;
    }
    else if ("/usr/myusr/app/sources/rpm-artifact-1.0-sources.jar".equals(fileInfo.path))
    {
        foundSource = true;
    }
}

if (!foundLibJar)
    throw new java.lang.AssertionError("jar artifact '/usr/myusr/app/lib/rpm-artifact-1.0.jar' not present in lib folder '" + fileInfo.path + "'");

if (!foundLibSource)
    throw new java.lang.AssertionError("sources jar secondary artifact '/usr/myusr/app/lib/rpm-artifact-1.0-sources.jar' not present in lib folder '" + fileInfo.path + "'");

if (!foundSource)
    throw new java.lang.AssertionError("sources jar secondary artifact '/usr/myusr/app/sources/rpm-artifact-1.0-sources.jar' not present in sources folder '" + fileInfo.path + "'");

return true
