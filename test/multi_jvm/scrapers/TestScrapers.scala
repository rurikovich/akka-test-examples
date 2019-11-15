package scrapers



trait TestScrapers extends ScrapersRegistry {
  val scrapersList: List[ScraperFactory] = List(TestScraperFactory1(mongoDbUri, dbName), TestScraperFactory2(mongoDbUri, dbName))
}
