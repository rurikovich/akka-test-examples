package scrapers

import net.ruippeixotog.scalascraper.model.Document
import org.bson.codecs.configuration.CodecRegistries.fromProviders
import org.mongodb.scala.bson.codecs.Macros._

case class TestPageData2(page: String)

case class TestPageScraper2() extends PageScraper[TestPageData2] {

  override def entryPointLinks: List[String] = List("/test/apps")

  override def BASE_URL: String = "http://localhost"

  override def parseLinks(doc: Document): List[String] = List.empty[String]

  override def parsePageData(pageLink: String, doc: Document): Option[TestPageData2] = Some(TestPageData2(pageLink))

}

case class TestScraperFactory2(mongoDbUri: String, dbName: String) extends SiteScraperFactory[TestPageData2](
  site = "testSite2",
  page = "testPage2",
  pageDataRegistry = fromProviders(classOf[TestPageData2]),
  pageScraper = TestPageScraper2(),
  mongoDbUri = mongoDbUri,
  dbName = dbName
)

