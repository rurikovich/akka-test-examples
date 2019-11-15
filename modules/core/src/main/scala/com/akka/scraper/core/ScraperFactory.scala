package com.akka.scraper.core

import akka.actor.{ActorRef, ActorSystem}

trait ScraperFactory {

  def site: String

  def page: String

  def createScraper(system: ActorSystem): ActorRef

  def createDataManagerActorProxy(system: ActorSystem): ActorRef
}
