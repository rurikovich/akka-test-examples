package com.akka.scraper.core.webScraper

import net.ruippeixotog.scalascraper.model.Document

trait PageScraper[PageData] extends Serializable {

  def entryPointLinks: List[String]

  def BASE_URL: String

  def parseLinks(doc: Document): List[String]

  def parsePageData(pageLink: String, doc: Document): Option[PageData]

}
