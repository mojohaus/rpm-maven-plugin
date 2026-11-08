// MRPM-150: a softlinkSource may name a file or a directory; both must end up as links in the package
def rpms = []
new File(basedir, 'target/rpm').eachFileRecurse { if (it.name.endsWith('.rpm')) rpms << it }
assert rpms.size() == 1 : "expected one built rpm under target/rpm, found ${rpms}"

def proc = ['rpm', '-qvlp', rpms[0].absolutePath].execute()
def lines = proc.in.text.readLines()
proc.waitFor()

assert lines.any { it =~ /^l.*\s\/usr\/myapp\/hosts -> \/etc\/hosts$/ } : "no link to the file /etc/hosts in:\n${lines.join('\n')}"
assert lines.any { it =~ /^l.*\s\/usr\/myapp\/etc -> \/etc$/ } : "no link to the directory /etc in:\n${lines.join('\n')}"

return true
