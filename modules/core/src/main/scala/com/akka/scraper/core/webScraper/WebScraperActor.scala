package com.akka.scraper.core.webScraper

import akka.actor.{Actor, ActorLogging, ActorRef, Cancellable, PoisonPill}
import com.akka.scraper.core.common.DataManagerProxy
import net.ruippeixotog.scalascraper.browser.JsoupBrowser.JsoupDocument
import org.jsoup.Jsoup

import scala.concurrent.duration.FiniteDuration

//TODO продумать поведене при рестарте
class WebScraperActor[PageData](scraperName: String, processLinkTimeout: FiniteDuration, pageScraper: PageScraper[PageData]) extends Actor with ActorLogging with DataManagerProxy {
  log.error(s"ScraperDebug WebScraperActor starting")

  val newLinksButchSize = 10

  val selfRef: ActorRef = self

  override def receive: Receive = {
    case cmd: ParsePageCmd =>

     try {
       log.error(s"ScraperDebug WebScraperActor ${selfRef.path.name} recieved ParsePageCmd cmd=$cmd")
       val pageLink = cmd.page

       log.error(s"ScraperDebug WebScraperActor try to parse ${pageScraper.BASE_URL + pageLink}")
       val doc: JsoupDocument = downloadPage(pageLink)
       getDataManagerProxy(context.system, scraperName) ! SavePageFetchResultCmd[PageData](pageLink, pageScraper.parseLinks(doc), pageScraper.parsePageData(pageLink, doc))
     } catch {
       case e:Exception=>
         log.error(s"ScraperDebug WebScraperActor try to parse page e=${e.getStackTraceString}")
     }

    case cmd: StartCmd =>
      log.error(s"ScraperDebug WebScraperActor ${selfRef.path.name} send AllocateLinksCmd($newLinksButchSize)")
      getDataManagerProxy(context.system, scraperName) ! AllocateLinksCmd(newLinksButchSize)

    case cmd: StopCmd =>
      log.error(s"ScraperDebug WebScraperActor ${selfRef.path.name} recieved stopCmd")
      stopMyself()

    case cmd: ProcessLinksCmd =>
      log.error(s"ScraperDebug WebScraperActor ${selfRef.path.name} recieved ProcessLinksCmd cmd=$cmd")
      cmd.links.headOption.foreach(selfRef ! ParsePageCmd(_))

      if (cmd.links.size >= 2) {
        log.error(s"ScraperDebug WebScraperActor ${selfRef.path.name} send other links to myself with delay=$processLinkTimeout")
        scheduleLinksProcessing(cmd.links.tail)
      } else {
        log.error(s"ScraperDebug WebScraperActor ${selfRef.path.name} send AllocateLinksCmd($newLinksButchSize)")
        getDataManagerProxy(context.system, scraperName) ! AllocateLinksCmd(newLinksButchSize)
      }

    case msg =>
      log.error(s"ScraperDebug WebScraperActor ${selfRef.path.name} msg=$msg")
  }

  protected def downloadPage(pageLink: String): JsoupDocument = {
    JsoupDocument(Jsoup.connect(pageScraper.BASE_URL + pageLink).timeout(0).get())
  }

  protected def stopMyself(): Unit = {
    selfRef ! PoisonPill
  }

  private def scheduleLinksProcessing(links: List[String]): Cancellable = {
    import context.dispatcher
    context.system.scheduler.scheduleOnce(processLinkTimeout, selfRef, ProcessLinksCmd(links))
  }

}

object WebScraperActor {

  case class ParsePageCmd(page: String)

  case class ProcessLinksCmd(links: List[String])

  case class StartCmd()

  case class StopCmd()

}
