// $Id$
import org.codehaus.mojo.unix.rpm.RpmUtil
import org.codehaus.mojo.unix.rpm.RpmUtil.FileInfo
import org.codehaus.mojo.unix.rpm.RpmUtil.SpecFile

import java.util.List
import java.util.Iterator


File rpm = new File((File) basedir, "target/rpm/project-rpm-3/RPMS/somearch/project-rpm-3-1.2-1.somearch.rpm")

if (!rpm.exists())
{
    throw new java.lang.AssertionError(rpm.getAbsolutePath() + " does not exist");
}

SpecFile spec = RpmUtil.getSpecFileFromRpm(rpm)

if (!spec.name.equals("project-rpm-3"))
    throw new java.lang.AssertionError("spec name");
if (!spec.version.equals("1.2"))
    throw new java.lang.AssertionError("spec version");
if (spec.release != 1)
    throw new java.lang.AssertionError("spec release");
if (!spec.license.equals("2009 my org"))
    throw new java.lang.AssertionError("spec license");

List fileInfos = RpmUtil.queryPackageForFileInfo(rpm)

int fileCnt = fileInfos.size()
System.out.println("File Count: " + fileCnt);
System.out.println(fileInfos);
if (fileCnt != 17)
    throw new java.lang.AssertionError("file count");

boolean x86NameScript = false;
boolean linuxNameScript = false;
boolean nameScript = false;
boolean osNameScript = false;
boolean archNameScript = false;
boolean oldNameLink = false;
boolean serviceLink = false;
boolean subdir = false;

for (Iterator i = fileInfos.iterator(); i.hasNext();)
{
    FileInfo fileInfo = (FileInfo) i.next()
        
    if (fileInfo.path.equals("/etc/init.d/myapp"))
    {   
        if (!fileInfo.user.equals("root"))
            throw new java.lang.AssertionError("file user for: " + fileInfo);
        if (!fileInfo.group.equals("root"))
            throw new java.lang.AssertionError("file group for: " + fileInfo);
        serviceLink = true;
    }
    else
    {
        if (!fileInfo.user.equals("myuser"))
            throw new java.lang.AssertionError("file user for: " + fileInfo);
        if (!fileInfo.group.equals("mygroup"))
            throw new java.lang.AssertionError("file group for: " + fileInfo);
        
        //check for executable mode
        if (fileInfo.path.startsWith("/usr/myusr/app/bin/"))
        {
            if (!fileInfo.mode.endsWith("rwxr-xr-x"))
                throw new java.lang.AssertionError("file mode for: " + fileInfo);

            if (fileInfo.path.endsWith("/name.sh"))
            {
                nameScript = true;
            }
            else if (fileInfo.path.endsWith("/name-someOS.sh"))
            {
                osNameScript = true;
            }
            else if (fileInfo.path.endsWith("/name-someArch.sh"))
            {
                archNameScript = true;
            }
            else if (fileInfo.path.endsWith("/name-linux.sh"))
            {
                linuxNameScript = true;
            }
            else if (fileInfo.path.endsWith("/name-x86.sh"))
            {
                x86NameScript = true;
            }
            else if (fileInfo.path.endsWith("/oldname.sh"))
            {
                oldNameLink = true;
            }
            else if (fileInfo.path.endsWith("/subdir"))
            {
                subdir = true;
            }
        }
    }
}

if (!nameScript)
    throw new java.lang.AssertionError("name script not found")

if (!osNameScript)
    throw new java.lang.AssertionError("os name script not found")
    
if (!archNameScript)
    throw new java.lang.AssertionError("arch name script not found")
    
if (!oldNameLink)
    throw new java.lang.AssertionError("old name link not found")
    
if (linuxNameScript)
    throw new java.lang.AssertionError("linux name script found")

if (x86NameScript)
    throw new java.lang.AssertionError("x86 name script found")
    
if (!serviceLink)
    throw new java.lang.AssertionError("service link not found")
    
if (!subdir)
    throw new java.lang.AssertionError("subdir directory not found")

return true
