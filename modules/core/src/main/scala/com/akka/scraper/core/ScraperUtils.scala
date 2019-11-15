package com.akka.scraper.core

object ScraperUtils {

  def scraperNameTemplate(site: String, page: String) = s"$site-$page"
}
