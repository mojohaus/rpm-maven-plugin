// $Id$
import org.codehaus.mojo.unix.rpm.RpmUtil
import org.codehaus.mojo.unix.rpm.RpmUtil.FileInfo
import org.codehaus.mojo.unix.rpm.RpmUtil.SpecFile

import java.io.*
import java.util.List
import java.util.Iterator

boolean success = true

File rpm = new File((File) basedir, "target/rpm/project-rpm-1/RPMS/noarch/project-rpm-1-1.1-1.noarch.rpm")

success &= rpm.exists()

SpecFile spec = RpmUtil.getSpecFileFromRpm(rpm)

success &= spec.name.equals("project-rpm-1")
success &= spec.version.equals("1.1")
success &= spec.release == 1
success &= spec.license.equals("2009 my org")

List fileInfos = RpmUtil.queryPackageForFileInfo(rpm)


int fileCnt = fileInfos.size()
System.out.println("File Count: " + fileCnt);
System.out.println(fileInfos);
success &= fileCnt == 15

boolean nameScript = false;
boolean osNameScript = false;
String expectedOsNameScript = "name-" + System.getProperty("os.name") + ".sh";
boolean oldNameLink = false;
boolean tempLink = false;
boolean unversionedHudsonWar = false;

for (Iterator i = fileInfos.iterator(); i.hasNext();)
{
    FileInfo fileInfo = (FileInfo) i.next()
    success &= fileInfo.user.equals("myuser")
    success &= fileInfo.group.equals("mygroup")
    
    //check for executable mode
    if (fileInfo.path.startsWith("/usr/myusr/app/bin/"))
    {
        //oldname.sh is a link, so the filemode is different
        if (fileInfo.path.endsWith("/oldname.sh"))
        {
            oldNameLink = true;
            success &= fileInfo.mode.equals("lrwxr-xr-x")
        }
        else
        {
            success &= fileInfo.mode.equals("-rwxr-xr-x")
        
            if (fileInfo.path.endsWith("/name.sh"))
            {
                nameScript = true;
            }
            else if (fileInfo.path.endsWith(expectedOsNameScript))
            {
                osNameScript = true;
            }
            
        }
    }
    else if (fileInfo.path.equals("/tmp/myapp/somefile2"))
    {
        tempLink = true;
        if (!fileInfo.mode.startsWith("l"))
        {
            throw new java.lang.AssertionError("temp link mode");
        }
    }
    else if (fileInfo.path.endsWith("hudson-war.war"))
    {
        unversionedHudsonWar = true;
    }
}

if (!tempLink)
    throw new java.lang.AssertionError("temp link");

success &= nameScript;
success &= osNameScript;
success &= oldNameLink;
success &= unversionedHudsonWar;

//now test that we actually filtered the file
File filteredFile = new File((File) basedir, "target/rpm/project-rpm-1/buildroot/usr/myusr/app/bin/filter.txt")

if (!filteredFile.exists())
    throw new java.lang.AssertionError("/usr/myusr/app/bin/filter.txt does not exist");
    
BufferedReader reader = new BufferedReader(new FileReader(filteredFile));
try
{
    String line = reader.readLine();
    if (!"bar".equals(line))
        throw new java.lang.AssertionError("contents of filter.txt expected[bar] actual[" + line + "]");
}
finally
{
    reader.close()
}

File filteredVersionFile = new File((File) basedir, "target/rpm/project-rpm-1/buildroot/usr/myusr/app/bin/filter-version.txt")

if (!filteredVersionFile.exists())
    throw new java.lang.AssertionError("/usr/myusr/app/bin/filter-version.txt does not exist");
    
BufferedReader reader2 = new BufferedReader(new FileReader(filteredVersionFile));
try
{
    String line = reader2.readLine();
    if (!"1.1-1".equals(line))
        throw new java.lang.AssertionError("contents of filter-version.txt expected[1.1-1] actual[" + line + "]");
}
finally
{
    reader2.close()
}

return success
