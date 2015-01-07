File buildLog = new File( basedir, "build.log" )
assert buildLog.text.contains("keyname = Maven GPG Plugin (TESTING KEY) <dev@maven.apache.org>")
assert buildLog.text.contains("passphraseServerKey = Maven GPG Plugin (TESTING KEY) <dev@maven.apache.org>")
