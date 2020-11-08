package org.example

import org.scalatest.{FlatSpec, Matchers}

class AppTest extends FlatSpec with Matchers {
  "Our example App" should "run" in {
    val app = new App
    app.runApp() shouldBe "It ran!"
  }
}
