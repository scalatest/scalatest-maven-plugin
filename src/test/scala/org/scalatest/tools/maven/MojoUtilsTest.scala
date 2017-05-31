package org.scalatest.tools.maven

import org.junit.{After, Before, Test}

class MojoUtilsTest {
  private var savedJavaHome: Option[String] = _

  @Before
  def save(): Unit = {
    savedJavaHome = Option(System.getProperty("java.home"))
  }

  @After
  def restore(): Unit = {
    savedJavaHome match {
      case None => System.clearProperty("java.home")
      case Some(value) => System.setProperty("java.home", value)
    }
  }

  @Test
  def getJvmHappyPath(): Unit = {
    System.setProperty("java.home", "/test/jvm")
    assert(MojoUtils.getJvm == "/test/jvm/bin/java")
  }

  @Test
  def getJvmWithoutJavaHome(): Unit = {
    System.clearProperty("java.home")
    assert(MojoUtils.getJvm == "java")
  }
}
