package com.akka.scraper.core.dao

import java.util.Date

import com.akka.scraper.core.master.settings.ScraperSettings._
import com.akka.scraper.core.model.PageLink
import org.bson.codecs.configuration.CodecRegistries.{fromProviders, fromRegistries}
import org.bson.codecs.configuration.CodecRegistry
import org.mongodb.scala.bson.codecs.DEFAULT_CODEC_REGISTRY
import org.mongodb.scala.bson.codecs.Macros._
import org.mongodb.scala.model.Filters
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model.Updates._
import org.mongodb.scala.result.UpdateResult
import org.mongodb.scala.{Completed, MongoClient, MongoCollection, MongoDatabase}
import play.api.Logger

import scala.concurrent.Future

class PageLinkDao(site: String, mongoDbUri: String, dbName: String) {
  val logger = Logger(this.getClass.getName)

  private lazy val mongoClient: MongoClient = MongoClient(mongoDbUri)
  private lazy val codecRegistry: CodecRegistry = fromRegistries(fromProviders(classOf[PageLink]), DEFAULT_CODEC_REGISTRY)
  private lazy val database: MongoDatabase = mongoClient.getDatabase(dbName).withCodecRegistry(codecRegistry)
  private lazy val pageLinkCollection: MongoCollection[PageLink] = database.getCollection(s"$site-pageLink")

  def findUnseenLinks(n: Int): Future[Seq[PageLink]] = {
    pageLinkCollection.find(
      filter = and(lt(nextVisitDateFieldName, now), lt(nextAllocateDateFieldName, now))
    ).limit(n).toFuture()
  }


  def saveNewLinks(newLinks: List[String]): Future[Completed] = pageLinkCollection.insertMany(newLinks.map(PageLink(_))).toFuture()

  def markLinkAsVisited(link: String): Future[PageLink] = pageLinkCollection.findOneAndUpdate(
    filter = Filters.eq(linkFieldName, link),
    update = set(nextVisitDateFieldName, nextVisiteDate)
  ).toFuture()

  def markLinksAsAllocated(links: List[String]): Future[UpdateResult] = {
    pageLinkCollection.updateMany(
      filter = in(linkFieldName, links: _*),
      update = set(nextAllocateDateFieldName, nextAllocateDate)
    ).toFuture()
  }

  def findExistingLinks(links: List[String]): Future[Seq[PageLink]] = pageLinkCollection.find(
    filter = Filters.in(linkFieldName, links: _*)
  ).toFuture()

  private def now = new Date

}
