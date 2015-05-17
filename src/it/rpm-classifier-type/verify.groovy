File rpm = new File(basedir, "target/rpm/rpm-classifier/RPMS/noarch/rpm-classifier-1.0-1.noarch.rpm")
if (!rpm.exists())
    throw new AssertionError("rpm file does not exist: ${rpm.getAbsolutePath()}")

def proc = ["rpm", "-qlp", rpm.getAbsolutePath()].execute()
proc.waitFor()
def expected = ["/usr/local/lib", "/usr/local/lib/signpost-commonshttp4-1.2.1.2-javadoc.jar", "/usr/local/lib/signpost-commonshttp4-1.2.1.2.jar"]
def i = 0;
proc.in.text.eachLine {
    if(!it.equals(expected[i]))
        throw new AssertionError(expected[i] + " not found!");
    i++;
}

return true
