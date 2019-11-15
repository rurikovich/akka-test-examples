package com.akka.scraper.core.master.util

import org.specs2.mutable.Specification

class MathHelperSpec extends Specification with MathHelper {

  "MathHelper" should {

    "diff one list from other correctly" in {
      listsDiff(List(""), List("")) must beEqualTo(List.empty)
      listsDiff(List("1", "2", "3"), List("1")) must beEqualTo(List("2", "3"))
      listsDiff(List("1", "1", "1", "2", "3"), List("1")) must beEqualTo(List("2", "3"))
      listsDiff(List("1", "3"), List("4")) must beEqualTo(List("1", "3"))
      listsDiff(List("1", "2", "3"), List("2", "4")) must beEqualTo(List("1", "3"))
    }

    "filterDublicates correctly" in {
      filterDublicates(List("1", "1", "2")) must beEqualTo(List("1", "2"))
    }
  }

}
