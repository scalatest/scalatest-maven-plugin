package demo.helloworld

import _root_.java.io.File

/**
  * Unit test for simple App.
  */
class AppTest extends FunSuite {

  /**
    * Rigourous Tests :-)
    */
  test("OK") {
    assert(true === true)
  }

  ignore("KO") {
    assert(true === false)
  }

  /**
    * Tests to make sure the project's XML files are well-formed.
    *
    * Finds every *.html and *.xml file in src/main/webapp (and its
    * subdirectories) and tests to make sure they are well-formed.
    */
  test("xml") {
    var failed: List[File] = Nil

    def handledXml(file: String) =
      file.endsWith(".xml")

    def handledXHtml(file: String) =
      file.endsWith(".html") || file.endsWith(".htm") || file.endsWith(".xhtml")

    def wellFormed(file: File) {
      if (file.isDirectory)
        for (f <- file.listFiles) wellFormed(f)

      if (file.isFile && handledXml(file.getName)) {
        try {
          XML.loadFile(file)
        } catch {
          case e: _root_.org.xml.sax.SAXParseException => failed = file :: failed
        }
      }
      if (file.isFile && handledXHtml(file.getName)) {
        PCDataXmlParser(new java.io.FileInputStream(file.getAbsolutePath)) match {
          case Full(_) => // file is ok
          case _ => failed = file :: failed
        }
      }
    }

    wellFormed(new File("src/main/webapp"))

    val numFails = failed.size
    if (numFails > 0) {
      val fileStr = if (numFails == 1) "file" else "files"
      val msg = "Malformed XML in " + numFails + " " + fileStr + ": " + failed.mkString(", ")
      println(msg)
      fail(msg)
    }
  }
}
