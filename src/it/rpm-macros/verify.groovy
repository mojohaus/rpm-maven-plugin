import org.codehaus.mojo.unix.rpm.RpmUtil
import org.codehaus.mojo.unix.rpm.RpmUtil.FileInfo
import org.codehaus.mojo.unix.rpm.RpmUtil.SpecFile

boolean success = true

File rpm = new File((File) basedir, "target/rpm/rpm-macros/RPMS/noarch/rpm-macros-1.0-1.noarch.rpm")

if (!rpm.exists())
{
    throw new AssertionError("${rpm.getAbsolutePath()} does not exist");
}

SpecFile spec = RpmUtil.getSpecFileFromRpm(rpm)

if (!spec.name.equals("rpm-macros"))
{
    throw new AssertionError("spec name: 'rpm-macros' != '${spec.name}'");
}
if (!spec.version.equals("1.0"))
{
    throw new AssertionError("spec version: '1.0' != '${spec.version}'");
}
if (spec.release != 1)
{
    throw new AssertionError("spec release: '1' != '${spec.release}'");
}

List fileInfos = RpmUtil.queryPackageForFileInfo(rpm)

int fileCnt = fileInfos.size()
if (fileCnt != 5)
{
    throw new AssertionError("file count: 5 != ${fileCnt}");
}
    
def proc = "rpm --eval %_datadir".execute()
proc.waitFor()
datadirpath = proc.in.text.trim()

expectedFiles = ["/usr/mygrp", "/usr/mygrp/app/lib", "/usr/mygrp/app/lib/grizzly-comet-counter.war",
                 "/usr/mygrp/app/somefile", datadirpath + "/app2"]
expectedDirs = ["/usr/mygrp", "/usr/mygrp/app/lib"]

for (Iterator i = fileInfos.iterator(); i.hasNext();)
{
    FileInfo fileInfo = (FileInfo) i.next()
    
    if (!expectedFiles.contains(fileInfo.path))
    {
        throw new AssertionError("Found unexpected file in RPM: ${fileInfo.path}");
    }

    if (expectedDirs.contains(fileInfo.path) && !fileInfo.mode.startsWith("d"))
    {
        throw new AssertionError("Expected: ${fileInfo.path} to be a directory, but has mode: ${fileInfo.mode}.")
    }

    if ("${datadirpath}/app2".equals(fileInfo.path) && !fileInfo.mode.startsWith("l"))
    {
        throw new AssertionError("Expected: ${fileInfo.path} to be a symbolic link, but has mode: ${fileInfo.mode}.")
    }
}

return true
