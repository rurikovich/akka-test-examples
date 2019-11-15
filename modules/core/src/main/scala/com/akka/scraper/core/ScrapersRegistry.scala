package com.akka.scraper.core

trait ScrapersRegistry {

  def mongoDbUri: String

  def dbName: String

  def scrapersList: List[ScraperFactory]

}
