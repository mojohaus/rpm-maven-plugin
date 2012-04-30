// $Id$
import org.codehaus.mojo.unix.rpm.RpmUtil
import org.codehaus.mojo.unix.rpm.RpmUtil.FileInfo
import org.codehaus.mojo.unix.rpm.RpmUtil.SpecFile

def success = true

def checkForActivationAndMailArtifact(fileInfos) {
    def result = ['activation':false, 'mail':false]
    fileInfos.each { fileInfo ->
        //check for executable mode
        //Why should we check a jar file for executable mode?
        if (fileInfo.path.endsWith ("activation-1.1.jar")) {
            result['activation'] = true
        } else if (fileInfo.path.endsWith ("mail-1.4.1.jar")) {
            result['mail'] = true
        }
    }
    return result;
}

def attachedRpm5 = new File(localRepositoryPath, "org/codehaus/mojo/rpm/its/rpm-2/1.0/rpm-2-1.0-attached-jre5.rpm")

success &= attachedRpm5.exists()

def fileInfos = RpmUtil.queryPackageForFileInfo(attachedRpm5)

def result = checkForActivationAndMailArtifact (fileInfos );

success &= result['activation'];
success &= result['mail'];

def attachedRpm6 = new File(localRepositoryPath, "org/codehaus/mojo/rpm/its/rpm-2/1.0/rpm-2-1.0-attached-jre6.rpm")

success &= attachedRpm6.exists()

fileInfos = RpmUtil.queryPackageForFileInfo(attachedRpm6)

result = checkForActivationAndMailArtifact (fileInfos );

success &= !result['activation'];
success &= result['mail'];

File primary = new File((File) localRepositoryPath, "org/codehaus/mojo/rpm/its/rpm-2/1.0/rpm-2-1.0.rpm")

// no primary rpm artifact should have been added
if (primary.exists())
{
    throw new java.lang.AssertionError("RPM incorrectly added as primary artifact");
}


File rpm5 = new File((File) basedir, "target/rpm/jre5/RPMS/noarch/jre5-1.0-1.noarch.rpm")

success &= rpm5.exists()

fileInfos = RpmUtil.queryPackageForFileInfo(rpm5)

result = checkForActivationAndMailArtifact (fileInfos );

success &= result['activation'];
success &= result['mail'];


File rpm6 = new File((File) basedir, "target/rpm/jre6/RPMS/noarch/jre6-1.0-1.noarch.rpm")

success &= rpm6.exists()

fileInfos = RpmUtil.queryPackageForFileInfo(rpm6)

result = checkForActivationAndMailArtifact (fileInfos );

success &= !result['activation'];
success &= result['mail'];

return success
