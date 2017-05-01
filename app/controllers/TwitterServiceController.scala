package controllers

import javax.inject.Inject

import models.TweetRepository
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, Controller}
import services.TwitterService

import scala.concurrent.{ExecutionContext, Future}

class TwitterServiceController @Inject()
(val twitterService: TwitterService)
(val tweetRepository: TweetRepository)
(implicit val context: ExecutionContext)
  extends TwitterServiceControllerTrait

trait TwitterServiceControllerTrait extends Controller {

  private val log = Logger("twitter-service-controller")

  import services.TwitterService._
  import controllers.json.ControllerWrites._

  implicit val context: ExecutionContext

  def twitterService: TwitterService

  def start: Action[AnyContent] = Action.async {
    twitterService.start().map {
      case TwitterServiceStartStatus.Successful(startedAt) =>
        Ok(Json.toJson(startedAt))
      case TwitterServiceStartStatus.Failed => {
        log.error("error while starting twitter")
        InternalServerError("internal server error")
      }
    }
  }

  def stop: Action[AnyContent] = Action {
    val blockingResult: Future[TwitterServiceStopStatus] = twitterService.stop()
    blockingResult.value.get.get match {
      case TwitterServiceStopStatus.Successful(stoppedAt) =>
        Ok(Json.toJson(stoppedAt))
      case _ => {
        log.error("error while stopping twitter")
        InternalServerError("internal server error")
      }
    }
  }

  def lastInsert: Action[AnyContent] = Action.async {
    twitterService.latest().map {
      case TwitterServiceLatestResult.Successful(geoReferencedTweet) =>
        Ok(Json.toJson(geoReferencedTweet))
      case TwitterServiceLatestResult.NoTweetsAvailable =>
        NoContent
    }.recover {
      case t =>
        log.error("error while getting last inserted tweet", t)
        InternalServerError("internal server error")
    }
  }

  def count: Action[AnyContent] = Action.async {
    twitterService.count().map {
      case count: Int => Ok(Json.obj(
        "numberOfTweets" -> count
      ))
    }.recover {
      case t =>
        log.error("error while getting total number of tweets", t)
        InternalServerError("internal server error")
    }
  }

  def langBreakdown: Action[AnyContent] = Action.async {
    twitterService.langBreakdown().map {
      case breakdown: Map[String, Int] => Ok(
        Json.toJson(breakdown)
      )
    }.recover {
      case t =>
        log.error("error while getting language breakdown", t)
        InternalServerError("internal server error")
    }
  }

}
