package com.akka.scraper.core.master

import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import com.akka.scraper.core.model.PageLink
import com.mongodb.client.result.UpdateResult
import org.mockito.ArgumentMatchers.{any, anyInt}
import org.mockito.Mockito._
import org.mongodb.scala.Completed
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import org.scalatestplus.mockito.MockitoSugar

import scala.concurrent.Future
import scala.concurrent.duration._

class DataManagerActorSpec extends TestKit(ActorSystem("MySpec"))
  with ImplicitSender
  with WordSpecLike
  with Matchers
  with MockitoSugar
  with BeforeAndAfterAll {

  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "An DataManagerActor actor" should {

    "received PageFetchResult and save appInfo" in {
      val pageLink = "/link1"
      val newLinks = List("/link2", "/link3")
      val pageData = TestPageData(name = "test1")

      case object SaveNewLinksMethodCall
      case object MarkLinkAsVisitedMethodCall
      case object SavePageDataMethodCall

      val probe = TestProbe()

      val pageLinkDaoMock: PageLinkDao = mock[PageLinkDao]
      when(pageLinkDaoMock.saveNewLinks(any[List[String]]())).thenReturn {
        probe.ref ! SaveNewLinksMethodCall
        Future.successful(Completed())
      }

      when(pageLinkDaoMock.findExistingLinks(any[List[String]]())).thenReturn {
        Future.successful(List.empty[PageLink])
      }

      when(pageLinkDaoMock.markLinkAsVisited(any[String]())).thenReturn {
        probe.ref ! MarkLinkAsVisitedMethodCall
        Future.successful(PageLink(pageLink))
      }


      val pageDataDaoMock: PageDataDao[TestPageData] = mock[PageDataDao[TestPageData]]
      when(pageDataDaoMock.save(any[TestPageData]())).thenReturn {
        probe.ref ! SavePageDataMethodCall
        Future.successful(Completed())
      }

      val dataManagerActor = system.actorOf(
        Props(
          classOf[DataManagerActor[TestPageData]], "site1", "page1", 30 seconds, pageLinkDaoMock, pageDataDaoMock, TestPageScraper
        )
      )
      dataManagerActor ! SavePageFetchResultCmd(pageLink, newLinks, Some(pageData))

      probe.expectMsg(SaveNewLinksMethodCall)
      probe.expectMsg(MarkLinkAsVisitedMethodCall)
      probe.expectMsg(SavePageDataMethodCall)

    }


    "received AllocateLinksCmd and send back generated EntryPointLinks if there is no links in db" in {
      val pageLinkDaoMock = mock[PageLinkDao]
      when(pageLinkDaoMock.findUnseenLinks(anyInt())).thenReturn(Future.successful(List.empty))
      when(pageLinkDaoMock.markLinksAsAllocated(any[List[String]])).thenReturn(Future.successful(UpdateResult.unacknowledged()))

      val pageDataDaoMock: PageDataDao[TestPageData] = mock[PageDataDao[TestPageData]]

      val dataManagerActor = system.actorOf(
        Props(
          classOf[DataManagerActor[TestPageData]], "site1", "page1", 30 seconds, pageLinkDaoMock, pageDataDaoMock, TestPageScraper
        )
      )

      dataManagerActor ! AllocateLinksCmd(1)

      expectMsg(ProcessLinksCmd(TestPageScraper.entryPointLinks))

    }

    "received AllocateLinksCmd and send back links if there are links in db" in {
      val pageLinkDaoMock = mock[PageLinkDao]

      val links: List[PageLink] = (1 to 10).map(i => PageLink(s"/link$i")).toList
      when(pageLinkDaoMock.findUnseenLinks(anyInt())).thenReturn(Future.successful(links))

      when(pageLinkDaoMock.markLinksAsAllocated(any[List[String]])).thenReturn(Future.successful(UpdateResult.unacknowledged()))

      val pageDataDaoMock: PageDataDao[TestPageData] = mock[PageDataDao[TestPageData]]

      val dataManagerActor = system.actorOf(
        Props(
          classOf[DataManagerActor[TestPageData]], "site1", "page1", 30 seconds, pageLinkDaoMock, pageDataDaoMock, TestPageScraper
        )
      )
      dataManagerActor ! AllocateLinksCmd(1)

      expectMsg(ProcessLinksCmd(links.map(_.link)))

    }


    "received two AllocateLinksCmd and send back different links to each sender" in {

      var linksIndex = 0

      def nextLinksPack(n: Int): List[PageLink] = {
        val links = (linksIndex until linksIndex + n).map(i => PageLink(s"/link$i")).toList
        linksIndex += 1
        links
      }

      val pageLinkDaoMock = mock[PageLinkDao]
      when(pageLinkDaoMock.findUnseenLinks(anyInt())).thenAnswer(answer => Future.successful(nextLinksPack(answer.getArgument(0))))

      when(pageLinkDaoMock.markLinksAsAllocated(any[List[String]])).thenReturn(Future.successful(UpdateResult.unacknowledged()))

      val pageDataDaoMock: PageDataDao[TestPageData] = mock[PageDataDao[TestPageData]]

      val dataManagerActor = system.actorOf(
        Props(
          classOf[DataManagerActor[TestPageData]], "site1", "page1", 30 seconds, pageLinkDaoMock, pageDataDaoMock, TestPageScraper
        )
      )

      dataManagerActor ! AllocateLinksCmd(1)

      dataManagerActor ! AllocateLinksCmd(2)


      expectMsgAllOf(ProcessLinksCmd(List("/link0")), ProcessLinksCmd(List("/link1", "/link2")))

    }

  }


}
