File jar = new File(basedir, "rpm-jar/target/rpm-reactor-module-jar-1.0.jar")
if (!jar.exists())
    throw new AssertionError("jar file does not exist: ${jar.getAbsolutePath()}");

File war = new File(basedir, "rpm-war/target/rpm-reactor-module-war-1.0.war")
if (!war.exists())
    throw new AssertionError("war file does not exist: ${war.getAbsolutePath()}");

File rpm = new File(basedir, "rpm-rpm/target/rpm/rpm-reactor-module-rpm/RPMS/noarch/rpm-reactor-module-rpm-1.0-rel.noarch.rpm")
if (!rpm.exists())
    throw new AssertionError("rpm file does not exist: ${rpm.getAbsolutePath()}")

def proc = ["rpm", "-qlp", rpm.getAbsolutePath()].execute()
proc.waitFor()
proc.in.text.eachLine {
    if (!it.equals("/usr/myusr/app/rpm-reactor-module-war-1.0.war"))
        throw new AssertionError("rpm.reactor-module-war-1.0.war missing!")
}

return true
