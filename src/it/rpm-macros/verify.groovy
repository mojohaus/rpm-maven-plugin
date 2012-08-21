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
    throw new java.lang.AssertionError("spec name: 'rpm-macros' != '" + spec.name + "'");
if (!spec.version.equals("1.0"))
    throw new java.lang.AssertionError("spec version: '1.0' != '" + spec.version + "'");
if (spec.release != 1)
    throw new java.lang.AssertionError("spec release: '1' != '" + spec.release + "'");

List fileInfos = RpmUtil.queryPackageForFileInfo(rpm)

int fileCnt = fileInfos.size()
if (fileCnt != 5)
    throw new java.lang.AssertionError("file count: 5 != " + fileCnt);
    
for (Iterator i = fileInfos.iterator(); i.hasNext();)
{
    FileInfo fileInfo = (FileInfo) i.next()
    
    //TODO
}
    
return true
