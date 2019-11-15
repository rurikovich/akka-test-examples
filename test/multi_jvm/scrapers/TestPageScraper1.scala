package scrapers

import net.ruippeixotog.scalascraper.model.Document
import org.bson.codecs.configuration.CodecRegistries.fromProviders
import org.mongodb.scala.bson.codecs.Macros._

case class TestPageData1(page: String)

case class TestPageScraper1() extends PageScraper[TestPageData1] {

  override def entryPointLinks: List[String] = List("/test/apps")

  override def BASE_URL: String = "http://localhost"

  override def parseLinks(doc: Document): List[String] = List.empty[String]

  override def parsePageData(pageLink: String, doc: Document): Option[TestPageData1] = Some(TestPageData1(pageLink))

}

case class TestScraperFactory1(mongoDbUri: String, dbName: String) extends SiteScraperFactory[TestPageData1](
  site = "testSite1",
  page = "testPage1",
  pageDataRegistry = fromProviders(classOf[TestPageData1]),
  pageScraper = TestPageScraper1(),
  mongoDbUri = mongoDbUri,
  dbName = dbName
)

