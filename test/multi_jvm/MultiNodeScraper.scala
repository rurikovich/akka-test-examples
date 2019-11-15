import akka.cluster.Cluster
import akka.cluster.ClusterEvent.{CurrentClusterState, MemberUp}
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import akka.remote.testkit.{MultiNodeConfig, MultiNodeSpec}
import akka.stream.ActorMaterializer
import akka.testkit.ImplicitSender
import akka.util.Timeout
import com.akka.scraper.core.common.DataManagerProxy
import com.github.simplyscala.MongoEmbedDatabase
import com.typesafe.config.ConfigFactory
import http.EmbeddedHttpServer._
import multi_jvm.STMultiNodeSpec
import org.scalatest.MustMatchers._
import org.scalatest.concurrent.ScalaFutures
import scrapers.TestScrapers

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

object MultiNodeSampleConfig extends MultiNodeConfig {
  val node1 = role("master")
  val node2 = role("worker1")
  val node3 = role("worker2")
  //
  //  nodeConfig(node1)(ConfigFactory.load("conf/test-master.conf"))
  //
  //  nodeConfig(node2)(ConfigFactory.load("conf/test-worker.conf"))
  //
  //  nodeConfig(node3)(ConfigFactory.load("conf/test-worker.conf"))


  testTransport(on = true)

  nodeConfig(node1)(ConfigFactory.parseString(
    """
      |akka.cluster.roles=[master]
    """.stripMargin))

  nodeConfig(node2)(ConfigFactory.parseString(
    """
      |akka.cluster.roles=[worker]
    """.stripMargin))

  nodeConfig(node3)(ConfigFactory.parseString(
    """
      |akka.cluster.roles=[worker]
    """.stripMargin))

  commonConfig(ConfigFactory.parseString(
    """
      |akka.loglevel=INFO
      |akka.actor.provider = cluster
      |akka.remote.artery.enabled = on
      |akka.coordinated-shutdown.run-by-jvm-shutdown-hook = off
      |akka.coordinated-shutdown.terminate-actor-system = off
      |akka.cluster.run-coordinated-shutdown-when-down = off
    """.stripMargin))

}

class MultiNodeScraperSpecMultiJvmNode1 extends MultiNodeScraper

class MultiNodeScraperSpecMultiJvmNode2 extends MultiNodeScraper

class MultiNodeScraperSpecMultiJvmNode3 extends MultiNodeScraper


class MultiNodeScraper extends MultiNodeSpec(MultiNodeSampleConfig) with STMultiNodeSpec with ImplicitSender with MongoEmbedDatabase with DataManagerProxy with ScalaFutures {

  import MultiNodeSampleConfig._

  implicit val materializer: ActorMaterializer = ActorMaterializer()

  def initialParticipants = 3

  val MONGO_PORT = 27017
  val MONGO_HOST = "127.0.0.1"
  val MONGO_DATABASE = "scraper"
  val MONGO_COLLECTION = "test-collection"

  trait TestDbConfig {
    def mongoDbUri: String = s"mongodb://$MONGO_HOST:$MONGO_PORT"

    def dbName: String = MONGO_DATABASE
  }

  "A MultiNodeScraper" must {

    object ScraperNode1 extends ScraperStarter with TestScrapers with TestDbConfig
    object ScraperNode2 extends ScraperStarter with TestScrapers with TestDbConfig
    object ScraperNode3 extends ScraperStarter with TestScrapers with TestDbConfig

    "start all nodes" in within(15.seconds) {
      // subscribe to MemberUp cluster events that we can leverage to assert that all nodes are up
      Cluster(system).subscribe(testActor, classOf[MemberUp])

      // the subscription will result in us receiving an `CurrentClusterState` snapshot as the first event, which we must handle
      expectMsgClass(classOf[CurrentClusterState])

      // instruct the ActorSystem on each node to join the cluster
      Cluster(system) join node(node1).address


      // bootstrap the application on node 1
      runOn(node1) {
        ScraperNode1.start(system)
      }

      // bootstrap the application on node 2
      runOn(node2) {
        ScraperNode2.start(system)
      }

      // bootstrap the application on node 3
      runOn(node3) {
        ScraperNode3.start(system)
      }

      // verify that all nodes have reached the "Up" state by collecting MemberUp events
      receiveN(3).collect {
        case MemberUp(m) => m.address
      }.toSet must be(Set(node(node1).address, node(node2).address, node(node3).address))

      // enter a new "all-up" barrier
      testConductor.enter("all-up")
    }


    "send StartScrappingCmd to ScraperCluster and receive saved to db results" in within(5.minutes) {

      runOn(node1) {
        withEmbedMongoFixture(port = MONGO_PORT) {
          mongodProps =>
            withEmbeddedServer(port = 80, routes = path("test" / "apps")(get(complete("Hello")))) {

              ScraperNode1.scrapersList.headOption.foreach(
                (scraperFactory: ScraperFactory) =>
                  dataManagerProxy(scraperFactory) ! StartScrappingCmd(workersNumber = 1)
              )


              enterBarrier("site-parsing-started")

              log.error(s"withEmbedMongoFixture size=$size")

              implicit val timeout: Timeout = 3000 seconds

              ScraperNode1.scrapersList.headOption.foreach(
                (scraperFactory: ScraperFactory) => {
                  val sitePageWorkersFuture: Future[SitePageworkers] = (dataManagerProxy(scraperFactory) ? GetStatistics()).map {
                    case sitePageWorkers: SitePageworkers => sitePageWorkers
                  }
                  //TODO  не понятно почему сразу создается 2 WebScraperActor(по 1 на каждую ноду с ролью worker)
                  //TODO почему они не создаются по требованию
                  sitePageWorkersFuture.futureValue should equal(SitePageworkers(scraperFactory.site, scraperFactory.page, 2))
                }
              )


            }


        }
      }


      runOn(node2) {
        enterBarrier("site-parsing-started")
      }

      runOn(node3) {
        Thread.sleep(10 * 1000)

        enterBarrier("site-parsing-started")
      }


    }


  }

  private def dataManagerProxy(scraperFactory: ScraperFactory) = {
    getDataManagerProxy(system, scraperNameTemplate(scraperFactory.site, scraperFactory.page))
  }
}
