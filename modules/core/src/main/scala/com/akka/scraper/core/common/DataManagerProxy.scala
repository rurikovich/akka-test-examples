package com.akka.scraper.core.common

import akka.actor.{ActorRef, ActorSelection, ActorSystem}
import akka.cluster.singleton.{ClusterSingletonProxy, ClusterSingletonProxySettings}

trait DataManagerProxy extends NodeRoles {

  def getDataManagerProxy(system: ActorSystem, scraperName: String): ActorSelection = {
    system.actorSelection(s"/user/${dataManagerProxyActorName(scraperName)}")
  }

  def createDataManagerProxy(system: ActorSystem, scraperName: String): ActorRef = {
    createProxy(system, dataManagerNameTemplate(scraperName))
  }

  private def dataManagerProxyActorName(scraperName: String): String = {
    proxyActorName(dataManagerNameTemplate(scraperName))
  }


  private def createProxy(system: ActorSystem, actorName: String): ActorRef = {
    system.actorOf(
      ClusterSingletonProxy.props(
        singletonManagerPath = s"/user/$actorName",
        settings = ClusterSingletonProxySettings(system).withRole(master)),
      name = proxyActorName(actorName))
  }

  private def proxyActorName(actorName: String) = {
    s"$actorName-Proxy"
  }
}
