File buildLog = new File( basedir, "target/rpm/rpm-relocate/SPECS/rpm-relocate.spec" )
assert buildLog.text.contains("Prefix: /opt")
assert buildLog.text.contains("Prefix: /tmp")
