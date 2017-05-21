
def logsFile = new File(basedir, "build.log")

if (!logsFile.exists()) {
  throw new Exception("Could not find build.log.  Searched: " + logsFile)
}

def testSummaryLines = []

logsFile.filterLine { 
  it ==~ /.*Tests: succeeded [0-9]*, failed [0-9]*, canceled [0-9]*, ignored [0-9]*, pending [0-9]*.*/
}.each { line -> testSummaryLines << "" + line }

if (testSummaryLines.size == 0) {
  throw new Exception("Could not find scalatest's summary line in build.log")
} else if (testSummaryLines.size > 1) {
  throw new Exception("Found more than one scalatest summary line in build.log")
}

if (testSummaryLines[0].contains("Tests: succeeded 0, failed 0, canceled 0, ignored 0, pending 0")) {
  throw new Exception("No tests were run by scalatest!")
}
