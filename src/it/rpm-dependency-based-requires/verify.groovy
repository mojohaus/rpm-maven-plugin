def specFile = new File(basedir, "app/target/rpm/rpm-dependency-based-requires-app/SPECS/rpm-dependency-based-requires-app.spec")
assert specFile.exists() : "spec file does not exist: ${specFile}"
def lines = specFile.readLines()

// the dependency-based require is added next to the explicitly configured one
assert lines.contains("Requires: rpm-dependency-based-requires-base >= 1.0-rel") : "dependency-based Requires missing in ${lines}"
assert lines.contains("Requires: explicit-package") : "explicit Requires missing in ${lines}"

// and survives into the built package
File rpm = new File(basedir, "app/target/rpm/rpm-dependency-based-requires-app/RPMS/noarch/rpm-dependency-based-requires-app-1.0-rel.noarch.rpm")
assert rpm.exists() : "rpm file does not exist: ${rpm.getAbsolutePath()}"
def proc = ["rpm", "-qRp", rpm.getAbsolutePath()].execute()
def requires = proc.in.text
proc.waitFor()
assert requires.contains("rpm-dependency-based-requires-base >= 1.0-rel") : "rpm requires: ${requires}"

return true
