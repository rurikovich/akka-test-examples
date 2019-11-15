package com.akka.test.scraper


import org.specs2.mutable._
import net.ruippeixotog.scalascraper.browser.HtmlUnitBrowser
import net.ruippeixotog.scalascraper.browser.HtmlUnitBrowser._

class TestWebScraperUtilsSpec extends Specification {

  val typedBrowser: HtmlUnitBrowser = HtmlUnitBrowser.typed()
  val doc: HtmlUnitDocument = typedBrowser.parseFile("test/resources/testPage.html")

  "Document" should {

    "contains author" in {
      TestPageScraper().parseAuthor(doc) must beSome("Test Inc.")
    }

    "contains email" in {
      TestPageScraper().parseEmail(doc) must beSome("test@support.test.com")
    }

    "contains website" in {
      TestPageScraper().parseWebSite(doc) must beSome("http://www.test.com/")
    }

    "contains Instals" in {
      TestPageScraper().parseInstalls(doc) must beSome("1,000,000,000+")
    }

    "contains new links" in {
      val links = TestPageScraper().parseLinks(doc)
      links must length(7)

      links must beEqualTo(
        List(
          "/test/apps/details?id=com.test.w4b",
          "/test/apps/details?id=com.test.lite",
          "/test/apps/details?id=com.imo.test.imoim",
          "/test/apps/details?id=com.test.mlite",
          "/test/apps/details?id=com.test.sav.test",
          "/test/apps/details?id=com.test.w4b",
          "/test/apps/details?id=com.test.wallpaper"
        )
      )
    }

  }
}
