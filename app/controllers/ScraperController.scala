package controllers

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.routing.Routee
import akka.util.Timeout
import javax.inject._
import play.api.libs.json.Json
import play.api.mvc._

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps

import play.api.libs.json._


@Singleton
class ScraperController @Inject()(cc: ControllerComponents,
                                  @Named("scraperActorSystem") system: ActorSystem
                                 )(implicit exec: ExecutionContext) extends AbstractController(cc) with DataManagerProxy {

  implicit val timeout: Timeout = 30 seconds

  implicit val format: OWrites[SitePageworkers] = Json.writes[SitePageworkers]

  def startSitePageSrapping(site: String, page: String): Action[AnyContent] = Action.async {
    Future {
      getDataManagerProxy(system, scraperNameTemplate(site, page)) ! StartScrappingCmd(workersNumber = 2)
      Ok("startSitePageSrapping")
    }
  }

  def stopSitePageSrapping(site: String, page: String): Action[AnyContent] = Action.async {
    Future {
      Ok("stopSitePageSrapping")
    }
  }

  def increaseWorkerNumber(site: String, page: String, n: Int): Action[AnyContent] = Action.async {
    Future {
      Ok("increaseWorkerNumber")
    }
  }

  def reduceWorkerNumber(site: String, page: String, n: Int): Action[AnyContent] = Action.async {
    Future {
      Ok("reduceWorkerNumber")
    }
  }


  def sitePageWorkersStats(site: String, page: String): Action[AnyContent] = Action.async {
    (getDataManagerProxy(system, scraperNameTemplate(site, page)) ? GetStatistics()).map {
      case sitePageWorkers: SitePageworkers => Ok(Json.toJson(sitePageWorkers))
      case _ => BadRequest("")
    }
  }


}
