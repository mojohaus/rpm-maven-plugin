File buildLog = new File( basedir, "build.log" )
assert buildLog.text.contains("keyname = KEY-1234")
assert buildLog.text.contains("passphraseServerKey = KEY-1234")
