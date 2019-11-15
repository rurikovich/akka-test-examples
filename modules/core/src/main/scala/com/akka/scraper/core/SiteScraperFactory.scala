package com.akka.scraper.core

import akka.actor.{ActorRef, ActorSystem, PoisonPill, Props}
import akka.cluster.singleton.{ClusterSingletonManager, ClusterSingletonManagerSettings}
import com.akka.scraper.core.common.{DataManagerProxy, NodeRoles}
import com.akka.scraper.core.master.DataManagerActor
import org.bson.codecs.configuration.CodecRegistry
import play.api.Logger

import scala.concurrent.duration._
import scala.reflect.ClassTag

abstract class SiteScraperFactory[PageData](override val site: String,
                                            override val page: String,
                                            pageDataRegistry: CodecRegistry,
                                            pageScraper: PageScraper[PageData],
                                            mongoDbUri: String,
                                            dbName: String
                                           )(implicit ct: ClassTag[PageData]) extends DataManagerProxy with ScraperFactory with NodeRoles {
  val logger = Logger(this.getClass.getName)

  private def createPageDataDao(): PageDataDao[PageData] = new PageDataDao[PageData](site, page, pageDataRegistry, mongoDbUri, dbName)

  private def createPageLinkDao(): PageLinkDao = new PageLinkDao(site, mongoDbUri, dbName)

  def createScraper(system: ActorSystem): ActorRef = {
    val ref = system.actorOf(
      ClusterSingletonManager.props(
        singletonProps = Props(classOf[DataManagerActor[PageData]], site, page, 30 seconds, createPageLinkDao(), createPageDataDao(), pageScraper),
        terminationMessage = PoisonPill,
        settings = ClusterSingletonManagerSettings(system).withRole(master)
      ),
      name = dataManagerNameTemplate(scraperNameTemplate(site, page))
    )
    logger.error(s"ScraperDebug SiteScraperFactory DataManagerActorSingleton site=$site page=$page created ref=$ref")
    ref
  }


  def createDataManagerActorProxy(system: ActorSystem): ActorRef = {
    createDataManagerProxy(system, scraperNameTemplate(site, page))
  }


}
