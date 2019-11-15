package com.akka.test.scraper

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives.{complete, get, path, _}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import akka.util.ByteString
import com.github.simplyscala.MongoEmbedDatabase
import http.EmbeddedHttpServer.withEmbeddedServer
import org.bson.codecs.configuration.CodecRegistries.fromRegistries
import org.bson.codecs.configuration.CodecRegistry
import org.mongodb.scala.bson.codecs.DEFAULT_CODEC_REGISTRY
import org.mongodb.scala.bson.collection.immutable.Document
import org.mongodb.scala.{MongoClient, MongoDatabase}
import org.scalatest.FunSpec
import org.scalatest.Matchers._

import scala.collection.immutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class TestEmbededMongoSpec extends FunSpec with MongoEmbedDatabase {

  /* Default values to be used for the test cases */
  private val WAIT_TIME = 10000L
  private val MONGO_PORT = 27017
  private val MONGO_HOST = "127.0.0.1"
  private val MONGO_DATABASE = "test-db"
  private val MONGO_COLLECTION = "test-collection"

  /* The MongoDB connection - connecting to the MongoDBEmbedded instance */
  private lazy val collection = {
    val mongoClient: MongoClient = MongoClient(s"mongodb://$MONGO_HOST:$MONGO_PORT")
    val codecRegistry: CodecRegistry = fromRegistries(DEFAULT_CODEC_REGISTRY)
    val database: MongoDatabase = mongoClient.getDatabase(MONGO_DATABASE).withCodecRegistry(codecRegistry)
    database.getCollection(MONGO_COLLECTION)
  }

  implicit val aSys: ActorSystem = ActorSystem("test")

  implicit val materializer: ActorMaterializer = ActorMaterializer()


  describe("A MongoBD query to find db info") {
    it("should return a result") {
      withEmbedMongoFixture(port = 27017) {
        mongodProps =>
          import scala.concurrent.ExecutionContext.Implicits.global

          val size: Int = Await.result(collection.find(Document("originatingCountry" -> "AU")).toFuture().map(
            (list: Seq[Document]) => list.size
          ), 100 seconds)

          assert(size == 0, "FAILURE >>> query should return 2 results")
      }
    }


    it("embeded http should return a result") {


      withEmbeddedServer(port = 80, routes = path("store" / "apps")(get(complete("Hello")))) {

        val result: Future[HttpResponse] = Http().singleRequest(HttpRequest(uri = Uri("http://localhost/store/apps"))).leftSideValue

        val res: HttpResponse = Await.result(result, 10 seconds)

        val HttpResponse(StatusCodes.OK, _, entity, _) = res
        val body: Future[immutable.Seq[ByteString]] = entity.dataBytes.runWith(Sink.seq)
        Await.result(body, 10 seconds).head.utf8String shouldBe "Hello"

      }

    }

  }
}
