package org.scalatest.tools.maven

import org.scalatest.matchers.{MatchResult, Matcher}

/**
  * @author Jon-Anders Teigen
  */
trait PluginMatchers {

  def containSlice(slice: String*) = new Matcher[Array[String]] {
    def apply(seq: Array[String]) = {
      MatchResult(
        seq containsSlice slice,
        "The " + seq + " did not contain the slice " + slice,
        "The " + seq + " did contain the slice " + slice
      )
    }
  }

  def containCompoundArgs(name: String, values: String*) = new Matcher[Array[String]] {
    def apply(seq: Array[String]) = {
      MatchResult(
        seq containsSlice List(name, values.mkString(" ")),
        "The compoundArgs for " + name + " and " + values.mkString(" ") + " where not found in " + seq,
        "The compoundArgs for " + name + " and " + values.mkString(" ") + " where found in " + seq
      )
    }
  }

  def containSuiteArgs(name: String, values: String*) = new Matcher[Array[String]] {
    def apply(seq: Array[String]) = {
      MatchResult(
        values.forall(v => seq.containsSlice(List(name, v))),
        "The suiteArgs " + name + " " + values.mkString(" ") + " were not all found in " + seq,
        "The suiteArgs for " + name + " " + values.mkString(" ") + " were all found in " + seq
      )
    }
  }
}