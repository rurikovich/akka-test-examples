package com.akka.scraper.core.webScraper

import akka.actor.{ActorRef, ActorSelection, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.browser.JsoupBrowser.JsoupDocument
import net.ruippeixotog.scalascraper.model.Document
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import org.scalatestplus.mockito.MockitoSugar

import scala.concurrent.duration._

class WebScraperActorSpec extends TestKit(ActorSystem("MySpec"))
  with ImplicitSender
  with WordSpecLike
  with Matchers
  with MockitoSugar
  with BeforeAndAfterAll {

  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "An WebScraperActor actor" should {

    "received ParsePageCmd and send back PageFetchResult" in {

      val pageLink = "/link1"
      val newLinks = List("/link2", "/link3")
      val testPageData = TestPageData(name = "test")

      object TestPageScraper extends PageScraper[TestPageData] {

        def entryPointLinks: List[String] = List("/store/apps")

        def BASE_URL: String = "https://test.com"

        def parseLinks(doc: Document): List[String] = newLinks

        def parsePageData(page: String, doc: Document): Option[TestPageData] = Some(testPageData)
      }

      val dataManagerActorProbe = TestProbe()

      val webScraperActor = system.actorOf(
        Props(new WebScraperActor[TestPageData](scraperName = "testScraper", 30 seconds, TestPageScraper) {

          val browser: JsoupBrowser = mock[JsoupBrowser]

          override def downloadPage(pageLink: String): JsoupDocument = JsoupDocument(new org.jsoup.nodes.Document(""))

          override def getDataManagerProxy(system: ActorSystem, scraperName: String): ActorSelection = system.actorSelection(dataManagerActorProbe.ref.path)

        })
      )

      webScraperActor ! ParsePageCmd(pageLink)

      dataManagerActorProbe.expectMsg(SavePageFetchResultCmd[TestPageData](pageLink, newLinks, Some(testPageData)))


    }

    "received not empty list of links to process and send ParsePageCmd to self and send other links to self for future processing" in {

      val dataManagerActorProbe = TestProbe()
      val selfTestProbe = TestProbe()

      val site: String = "play.google.com"
      val page: String = "app"
      val links = List("/link2", "/link3")

      val linksButchSize = 10

      val webScraperActor = system.actorOf(
        Props(new WebScraperActor[TestPageData](scraperName = "testScraper", 1 second, TestPageScraper) {

          val browser: JsoupBrowser = mock[JsoupBrowser]

          override val selfRef = selfTestProbe.ref

          override val newLinksButchSize = linksButchSize

          override def downloadPage(pageLink: String): JsoupDocument = JsoupDocument(new org.jsoup.nodes.Document(""))

          override def getDataManagerProxy(system: ActorSystem, scraperName: String): ActorSelection = system.actorSelection(dataManagerActorProbe.ref.path)

          override def stopMyself(): Unit = selfTestProbe.ref ! StopTaskManagerActorTestCmd

        })
      )

      //check#1 при приеме StartCmd webScraperActor должен запросить пачку ссылок для обработки
      webScraperActor ! StartCmd()
      dataManagerActorProbe.expectMsg(AllocateLinksCmd(linksButchSize))

      //check#2 когда webScraperActor присылают пачку ссылок для обработку он должен первую отправить себе на скачивание,
      // а остальные отправить себе (с заданной задержкой. но это тут не проверяется)
      webScraperActor ! ProcessLinksCmd(links)
      selfTestProbe.expectMsg(ParsePageCmd(links.head))
      selfTestProbe.expectMsg(ProcessLinksCmd(links.tail))


      //check#3 при приеме StopCmd webScraperActor должен послать себе PoisonPill
      webScraperActor ! StopCmd()
      selfTestProbe.expectMsg(StopTaskManagerActorTestCmd)
    }

    "received list with 1 link to process, send ParsePageCmd to WebScraperActor and send AllocateLinksCmd to LinksManagerActor" in {

      val dataManagerActorProbe = TestProbe()
      val selfTestProbe = TestProbe()

      val site: String = "play.google.com"
      val page: String = "app"

      val linksButchSize = 10

      val webScraperActor = system.actorOf(
        Props(new WebScraperActor[TestPageData](scraperName = "testScraper", 1 seconds, TestPageScraper) {

          val browser: JsoupBrowser = mock[JsoupBrowser]

          override val selfRef: ActorRef = selfTestProbe.ref

          override val newLinksButchSize: Int = linksButchSize

          override def downloadPage(pageLink: String): JsoupDocument = JsoupDocument(new org.jsoup.nodes.Document(""))

          override def getDataManagerProxy(system: ActorSystem, scraperName: String): ActorSelection = system.actorSelection(dataManagerActorProbe.ref.path)

          override def stopMyself(): Unit = selfTestProbe.ref ! StopTaskManagerActorTestCmd

        })
      )
      val links = List("/link2")

      //check#1 когда webScraperActor присылают пачку ссылок для обработку он должен первую отправить себе для скачивания,
      // и если список состоит только из 1 ссылки то потом он должен запросить еще ссылок на обработку
      webScraperActor ! ProcessLinksCmd(links)
      selfTestProbe.expectMsg(ParsePageCmd(links.head))
      dataManagerActorProbe.expectMsg(AllocateLinksCmd(linksButchSize))

    }
  }

  case class StopTaskManagerActorTestCmd()


}
