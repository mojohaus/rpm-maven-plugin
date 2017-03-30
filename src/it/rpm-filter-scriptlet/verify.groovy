File specFile = new File( basedir, "target/rpm/rpm-filter-scriptlet/SPECS/rpm-filter-scriptlet.spec" )

// Test if the pre install inline <script> was filtered
assert specFile.text.contains("echo \"installing rpm-filter-scriptlet\"")

// Test if the post install <scriptFile> was filtered
assert specFile.text.contains("PROJECT_NAME=rpm-filter-scriptlet")

// Test if the pre remove <scriptFile> was filtered
assert specFile.text.contains("echo \"Erasing version 1.0\"")

// Test if the post remove <scriptFile> was found and filtered (located in a relative path)
assert specFile.text.contains("echo \"Done erasing version 1.0\"")

// Test if the trigger script was filtered
assert specFile.text.contains("echo \"a filtered install trigger for rpm-filter-scriptlet\"")

return true