package com.akka.scraper.core

import akka.actor.ActorSystem
import akka.cluster.Cluster
import com.akka.scraper.core.common.NodeRoles

trait ScraperStarter extends ScrapersRegistry with NodeRoles{

  def start(system: ActorSystem): Unit = {
    if (Cluster(system).getSelfRoles.contains(master)) {
      scrapersList.foreach(_.createScraper(system))
    }
    scrapersList.foreach(_.createDataManagerActorProxy(system))
  }

}
