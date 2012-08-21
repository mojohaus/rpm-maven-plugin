// $Id$
import org.codehaus.mojo.unix.rpm.RpmUtil
import org.codehaus.mojo.unix.rpm.RpmUtil.FileInfo
import org.codehaus.mojo.unix.rpm.RpmUtil.SpecFile

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


File attachedRpm5 = new File(localRepositoryPath, "org/codehaus/mojo/rpm/its/rpm-2/1.0/rpm-2-1.0-attached-jre5.rpm")
assert attachedRpm5.exists()

def fileInfos = RpmUtil.queryPackageForFileInfo(attachedRpm5)

def result = checkForActivationAndMailArtifact (fileInfos );
assert result['activation'] : "Activation artifact missing in attached JRE5 RPM"
assert result['mail']  : "Mail artifact missing in attached JRE5 RPM"


File attachedRpm6 = new File(localRepositoryPath, "org/codehaus/mojo/rpm/its/rpm-2/1.0/rpm-2-1.0-attached-jre6.rpm")
assert attachedRpm6.exists()

fileInfos = RpmUtil.queryPackageForFileInfo(attachedRpm6)

result = checkForActivationAndMailArtifact (fileInfos );
assert !result['activation'] : "Activation artifact shouldn't exist in attached JRE6 RPM"
assert result['mail']  : "Mail artifact missing in attached JRE6 RPM"


File primary = new File((File) localRepositoryPath, "org/codehaus/mojo/rpm/its/rpm-2/1.0/rpm-2-1.0.rpm")
assert !primary.exists() : "RPM incorrectly added as primary artifact: " + primary.getAbsolutePath()


File rpm5 = new File((File) basedir, "target/rpm/jre5/RPMS/noarch/jre5-1.0-1.noarch.rpm")
assert rpm5.exists()

fileInfos = RpmUtil.queryPackageForFileInfo(rpm5)

result = checkForActivationAndMailArtifact (fileInfos );
assert result['activation'] : "Activation artifact missing in built JRE5 RPM"
assert result['mail']  : "Mail artifact missing in built JRE5 RPM"


File rpm6 = new File((File) basedir, "target/rpm/jre6/RPMS/noarch/jre6-1.0-1.noarch.rpm")
assert rpm6.exists()

fileInfos = RpmUtil.queryPackageForFileInfo(rpm6)

result = checkForActivationAndMailArtifact (fileInfos );
assert !result['activation'] : "Activation artifact shouldn't exist in built JRE6 RPM"
assert result['mail']  : "Mail artifact missing in built JRE6 RPM"
