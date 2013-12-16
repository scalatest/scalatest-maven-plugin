package org.scalatest.tools.maven

import java.io.File
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnit3Suite
import java.util.ArrayList
import org.scalatest.BeforeAndAfterAll

/**
 * @author Jon -Anders Teigen
 */
class PluginTest extends JUnit3Suite with ShouldMatchers with PluginMatchers with BeforeAndAfterAll {
  val tmpDir = new File(System.getProperty("java.io.tmpdir"))
  val reportsDirectory = new File(tmpDir, "reportsDirectory")
  val baseDir = new File(tmpDir, "basedir");
  val testOutputDirectory = new File(reportsDirectory, "testOutputDirectory").getAbsolutePath
  val outputDirectory = new File(reportsDirectory, "outputDirectory").getAbsolutePath

  override def afterAll {
    def delete(it: File) {
      if (it.isFile) {
        it.delete()
      } else {
        for (d <- it.listFiles)
          delete(d)
      }
    }
    delete(reportsDirectory)
    delete(baseDir);
  }

  def jlist(a: String*) = new ArrayList[String]() {for (e <- a) this.add(e)}

  def comma(a: String*) = a mkString ","

  def configure(m: TestMojo => Unit) = {
    val mojo = new TestMojo
    mojo.reportsDirectory = reportsDirectory
    mojo.testOutputDirectory = new File(testOutputDirectory)
    mojo.outputDirectory = new File(outputDirectory)
    m(mojo)
    mojo.configuration
  }

  def testDefault {
    val config = configure(_ => ())
    config should contain("-o")
    config should containCompoundArgs("-R", outputDirectory, testOutputDirectory)
    config should have length (3)
  }

  def testConfigs {
    val config = configure(_.config = comma("foo=bar", "monkey=donkey"))
    config should contain("-Dfoo=bar")
    config should contain("-Dmonkey=donkey")
  }

  def testRunpath {
    configure(_.runpath = comma("http://foo.com/my.jar", "/some/where")) should containCompoundArgs("-R", outputDirectory, testOutputDirectory, "http://foo.com/my.jar", "/some/where")
  }

  def testFilereporters {
    val config = configure(_.filereports = comma("foo.txt", "YZT some.txt"))
    config should containSlice("-f", new File(reportsDirectory, "foo.txt").getAbsolutePath)
    config should containSlice("-fYZT", new File(reportsDirectory, "some.txt").getAbsolutePath)
  }

  def testHtmlreporters {
    val config = configure(_.htmlreporters =
      comma("target/htmldir", "target/myhtmldir src/resources/my.css"))

    config should containSlice("-h", "target/htmldir")
    config should containSlice("-h", "target/myhtmldir",
                               "-Y", "src/resources/my.css")
  }

  def testReporters {
    val config = configure(_.reporters = comma("YZT org.my.reporter", "org.your.reporter"))
    config should containSlice("-CYZT", "org.my.reporter")
    config should containSlice("-C", "org.your.reporter")
  }

  def testJUnitXmlReporters {
    val config = configure(_.junitxml = comma("some/foo.xml", "XYZ other.xml"))
    config should containSlice("-u", new File(reportsDirectory, "some/foo.xml").getAbsolutePath)
    config should containSlice("-uXYZ", new File(reportsDirectory, "other.xml").getAbsolutePath)
  }

  def testStdOutReporter {
    configure(_.stdout = "GUP") should contain("-oGUP")
  }

  def testStdErrReporter {
    configure(_.stderr = "BIS") should contain("-eBIS")
  }

  def testIncludes {
    configure(_.tagsToInclude = comma("a", "b", "c")) should containCompoundArgs("-n", "a", "b", "c")
  }

  def testExcludes {
    configure(_.tagsToExclude = comma("a", "b", "c")) should containCompoundArgs("-l", "a", "b", "c")
  }

  def testConcurrent {
    configure(_.parallel = true) should contain("-c")
    configure(_.parallel = false) should not contain ("-c")
  }

  def testSuites {
    val suites: String = comma(" a ",
                               "b",
                               "foo @bar baz",
                               " zowie\n  zip zap ")

    val config = configure(_.suites = suites)

    config should containSlice ("-s", "a")
    config should containSlice ("-s", "b")
    config should containSlice ("-s", "foo", "-t", "bar baz")
    config should containSlice ("-s", "zowie", "-z", "zip zap")
  }

  def testSuitesAndTests {
    val suites: String = comma(" a ", "b c")
    val tests:  String = comma(" d ", "@e")

    val config = configure(x => {x.suites = suites; x.tests = tests})

    config should containSlice ("-z", "d",
                                "-t", "e",
                                "-s", "a",
                                "-s", "b", "-z", "c")
  }

  def testTests {
    val tests: String= comma(" @a ", " b ", "@c")

    val config = configure(_.tests = tests)

    config should containSlice("-t", "a")
    config should containSlice("-z", "b")
    config should containSlice("-t", "c")
  }

  //
  // Verify that a comma can be escaped with a backslash in order to
  // support a test name that contains a comma.
  //
  def testTestsWithCommas {
    configure(_.tests = comma("a\\, bc", "b", "c")) should containSuiteArgs("-z", "a, bc", "b", "c")
  }

  def testSuffixes {
    configure(_.suffixes = "(?<!Integration)(Test|Spec|Suite)") should containSuiteArgs("-q", "(?<!Integration)(Test|Spec|Suite)")
  }

  def testMembers {
    configure(_.membersOnlySuites = comma("a", "b", "c")) should containSuiteArgs("-m", "a", "b", "c")
  }

  def testWildcards {
    configure(_.wildcardSuites = comma("a", "b", "c")) should containSuiteArgs("-w", "a", "b", "c")
  }

  def testTestNGs {
    configure(_.testNGConfigFiles = comma("a", "b", "c")) should containSuiteArgs("-t", "a", "b", "c")
  }

  def testJUnits {
    configure(_.jUnitClasses = comma("a", "b", "c")) should containSuiteArgs("-j", "a", "b", "c")
  }

  def testMemoryFiles {
    configure(_.memoryFiles = comma("a", "b", "c")) should containSuiteArgs("-M", "a", "b", "c")
  }

  def testTestsFiles {
    configure(_.testsFiles = comma("nonesuch", "pom.xml", "src")) should containSuiteArgs("-A", "pom.xml", "src")
  }

  def testMojoConcat {
    MojoUtils.concat(jlist("a", "b", "c"), jlist("1", "2", "3")) should be(Array("a", "b", "c", "1", "2", "3"))
  }

  def testMojoSuiteArg {
    MojoUtils.suiteArg("-a", comma("a", "b", "c")) should be(jlist("-a", "a", "-a", "b", "-a", "c"))
    MojoUtils.suiteArg("-a", null) should be(jlist())
  }

  def testMojoCompundArg {
    MojoUtils.compoundArg("-a", comma("a", "b", "c")) should be(jlist("-a", "a b c"))
    MojoUtils.compoundArg("-a", null.asInstanceOf[String]) should be(jlist())
  }
}
