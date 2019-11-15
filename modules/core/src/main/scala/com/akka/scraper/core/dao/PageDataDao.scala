package com.akka.scraper.core.dao

import org.bson.codecs.configuration.CodecRegistries.fromRegistries
import org.bson.codecs.configuration.CodecRegistry
import org.mongodb.scala.bson.codecs.DEFAULT_CODEC_REGISTRY
import org.mongodb.scala.{Completed, MongoClient, MongoCollection, MongoDatabase}

import scala.concurrent.Future
import scala.reflect.ClassTag

class PageDataDao[PageData](site: String, page: String, pageDataRegistry: CodecRegistry, mongoDbUri: String, dbName: String)(implicit ct: ClassTag[PageData]) {

  lazy val mongoClient: MongoClient = MongoClient(mongoDbUri)
  lazy val database: MongoDatabase = mongoClient.getDatabase(dbName)
  lazy val collection: MongoCollection[PageData] = database.getCollection[PageData](s"$site-$page-pageData").withCodecRegistry(fromRegistries(pageDataRegistry, DEFAULT_CODEC_REGISTRY))

  def save(pageData: PageData): Future[Completed] = collection.insertOne(pageData).toFuture()
}
