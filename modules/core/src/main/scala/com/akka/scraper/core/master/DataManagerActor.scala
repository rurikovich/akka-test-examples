package com.akka.scraper.core.master

import akka.actor.{Actor, ActorContext, ActorLogging, ActorRef, Props}
import akka.cluster.routing.{ClusterRouterPool, ClusterRouterPoolSettings}
import akka.pattern.ask
import akka.routing.{RoundRobinPool, Routees}
import akka.util.Timeout
import com.akka.scraper.core.common.NodeRoles
import com.akka.scraper.core.master.util.MathHelper
import org.mongodb.scala.result.UpdateResult

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}

//TODO продумать поведене при рестарте
/**
  * Этот актор предназначен для:
  * 1) приема результатов парсинга страниц сайта
  * 2) для приема новых ссылок для последующей обработки
  * 3) раздачи новых ссылок
  * 4) управлением пулом WebScraperActor'ов
  *
  * @param onePageProcessTimeout определяет период парсинга одной ссылки для данного типа страниц. например 1 раз в 30 секунд или 1 раз в 2 мин
  */

class DataManagerActor[PageData](site: String,
                                 page: String,
                                 onePageProcessTimeout: FiniteDuration,
                                 pageLinkDao: PageLinkDao,
                                 pageDataDao: PageDataDao[PageData],
                                 pageScraper: PageScraper[PageData]
                                ) extends Actor with ActorLogging with MathHelper with NodeRoles {
  log.error(s"ScraperDebug DataManagerActor. starting")

  val futureAwaitDuration: FiniteDuration = 1000 seconds

  implicit val timeout: Timeout = 3000 seconds

  val webScrapeRouter: ActorRef = createWebScrapeRouter(context, site, page, pageScraper)

  override def receive: Receive = {

    case cmd: AllocateLinksCmd =>
      log.error(s"ScraperDebug DataManagerActor. AllocateLinksCmd cmd=$cmd")
      try {
        val linksFronDb = Await.result(pageLinkDao.findUnseenLinks(cmd.n), futureAwaitDuration)
        val links: List[String] = if (linksFronDb.isEmpty) pageScraper.entryPointLinks else linksFronDb.map(_.link).toList
        val result: UpdateResult = Await.result(pageLinkDao.markLinksAsAllocated(links), futureAwaitDuration)
        sender() ! ProcessLinksCmd(links)
        log.error(s"ScraperDebug DataManagerActor. Scraper allocated links=$links to ${sender().path}")

      } catch {
        case e: Exception =>
          log.error(s"ScraperDebug DataManagerActor. Scraper allocated links FAILED error=${e.getStackTraceString}")
      }


    case cmd: SavePageFetchResultCmd[PageData] =>
      log.error(s"ScraperDebug DataManagerActor. PageFetchResult cmd=$cmd")

      val linksToAdd: List[String] = Await.result(findNotExistingLinks(cmd.newLinks), futureAwaitDuration)
      if (linksToAdd.nonEmpty) {
        Await.result(pageLinkDao.saveNewLinks(filterDublicates(linksToAdd)), futureAwaitDuration)
        log.error(s"ScraperDebug DataManagerActor. Scraper saved links = $linksToAdd ")
      }

      cmd.fetchResult.foreach {
        pageData =>
          pageDataDao.save(pageData).onComplete {
            case Success(savedPageData) =>
              log.error(s"ScraperDebug DataManagerActor. Scraper app info saved = ${cmd.fetchResult} added")
              pageLinkDao.markLinkAsVisited(cmd.link).onComplete(
                l => log.error(s"ScraperDebug DataManagerActor. Scraper link = ${cmd.link} marked as visited")
              )

            case Failure(t) => log.error(s"ScraperDebug DataManagerActor. Scraper app info saving FAILED: reason=$t")
          }
      }

    case cmd: StartScrappingCmd =>
      log.error(s"ScraperDebug DataManagerActor. StartScrappingCmd send StartCmd to $webScrapeRouter")
      webScrapeRouter forward StartCmd()

    case cmd: StopScrappingCmd =>
      webScrapeRouter ! StopCmd()

    case cmd: IncreaseWorkerNumberCmd =>

    case cmd: ReduceWorkerNumberCmd =>

    case cmd: GetStatistics =>
      log.error(s"ScraperDebug DataManagerActor GetStatistics")
      val senderRef = sender()
      (webScrapeRouter ? akka.routing.GetRoutees).map {
        case routees: Routees => senderRef ! SitePageworkers(site, page, routees.routees.size)
        case _ => senderRef ! SitePageworkers(site, page, 0)
      }

    case _ =>
  }

  private def findNotExistingLinks(links: List[String]): Future[List[String]] = pageLinkDao.findExistingLinks(links).map {
    existingLinks =>
      val notExistingLinks = listsDiff(links, existingLinks.map(_.link).toList)
      log.error(s"ScraperDebug DataManagerActor findNotExistingLinks. links = $links existingLinks=$existingLinks notExistingLinks=$notExistingLinks")
      notExistingLinks
  }

  private def createWebScrapeRouter(context: ActorContext, site: String, page: String, pageScraper: PageScraper[PageData]): ActorRef = {
    val scraperName: String = scraperNameTemplate(site, page)
    log.error(s"ScraperDebug ScrapingManagerActor. creating ClusterRouterPool ${scraperName}-Router")
    val ref = context.actorOf(
      props = ClusterRouterPool(
        local = RoundRobinPool(0),
        settings = ClusterRouterPoolSettings(
          totalInstances = 1000,
          maxInstancesPerNode = 1,
          allowLocalRoutees = false
        ).withUseRoles(worker)
      ).props(
        Props(classOf[WebScraperActor[PageData]], scraperName, onePageProcessTimeout, pageScraper)
      ),
      name = s"$scraperName-Router"
    )

    ref
  }

}

object DataManagerActor {

  case class StopScrappingCmd()

  case class IncreaseWorkerNumberCmd()

  case class ReduceWorkerNumberCmd()

  case class StartScrappingCmd(workersNumber: Int)

  case class AllocateLinksCmd(n: Int)

  case class GetStatistics()

  case class SavePageFetchResultCmd[PageData](link: String, newLinks: List[String], fetchResult: Option[PageData])

  case class SitePageworkers(site: String, page: String, activeWorkers: Int)

  case class SitePageLinkStats(site: String, page: String, visitedLinks: List[String], inProcessLinks: List[String], newLinks: List[String])

  def dataManagerNameTemplate(scraperName: String) = s"$scraperName-DataManager"

}
