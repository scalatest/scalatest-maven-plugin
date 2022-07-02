
def logsFile = new File(basedir, "build.log")

if (!logsFile.exists()) {
  throw new Exception("Could not find build.log.  Searched: " + logsFile)
}

def result = logsFile.readLines().findAll() { 
  it.contains("Skipped because ScalaTest not available on classpath.") 
}
if (result.size == 0) {
  throw new Exception("Could not find skipped because ScalaTest not available on classpath message in build.log")
}

return true
