// The empty preinstallScriptlet element should not fail build, but incorrect postinstallScriptlet should
File buildLog = new File( basedir, "build.log" )
assert buildLog.text.contains("defined scriptFile does not exist")
