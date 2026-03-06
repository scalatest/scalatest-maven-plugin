package org.example

import java.io.File
import java.io.FileWriter
import java.lang.management.ManagementFactory

import org.scalatest.funsuite.AnyFunSuite

class SecondSuite extends AnyFunSuite {

  test("records second suite pid") {
    appendPid("second")
  }

  private def appendPid(label: String): Unit = {
    val targetDir = new File(System.getProperty("basedir"), "target")
    targetDir.mkdirs()
    val pidFile = new File(targetDir, "fork-always-pids.txt")
    val writer = new FileWriter(pidFile, true)
    try {
      writer.write(label + ":" + ManagementFactory.getRuntimeMXBean.getName.split("@")(0) + System.lineSeparator())
    } finally {
      writer.close()
    }
  }
}
