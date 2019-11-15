package com.akka.scraper.core.scrapers


import net.ruippeixotog.scalascraper.model.Document

case class TestPageData(name: String)

object TestPageScraper extends PageScraper[TestPageData] {
  def BASE_URL: String = "https://test.com"

  def parseLinks(doc: Document): List[String] = List()

  def parsePageData(pageLink: String, doc: Document): Option[TestPageData] = None

  def entryPointLinks: List[String] = List("/store/apps")
}