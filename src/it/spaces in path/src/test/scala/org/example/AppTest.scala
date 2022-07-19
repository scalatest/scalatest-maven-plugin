package org.example

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class AppTest extends AnyFlatSpec with Matchers {
  "Our example App" should "run" in {
    val app = new App
    app.runApp() shouldBe "It ran!"
  }
}
