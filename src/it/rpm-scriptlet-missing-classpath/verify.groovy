File buildLog = new File( basedir, "build.log" )

// The message names the value as configured, including the classpath: prefix
assert buildLog.text.contains("defined scriptFile does not exist: classpath:no/such/script.sh")

return true
