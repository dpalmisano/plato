package controllers

import javax.inject.Inject

import models.TweetRepository
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

  import services.TwitterService._
  import controllers.json.ControllerWrites._

  implicit val context: ExecutionContext

  def twitterService: TwitterService

  def start: Action[AnyContent] = Action.async {
    twitterService.start().map {
      case TwitterServiceStartStatus.Successful(startedAt) =>
        Ok(Json.toJson(startedAt))
      case TwitterServiceStartStatus.Failed => {
        InternalServerError("isr")
      }
    }
  }

  def stop: Action[AnyContent] = Action {
    val blockingResult: Future[TwitterServiceStopStatus] = twitterService.stop()
    blockingResult.value.get.get match {
      case TwitterServiceStopStatus.Successful(stoppedAt) =>
        Ok(Json.toJson(stoppedAt))
      case _ => InternalServerError("isr")
    }
  }

  def lastInsert: Action[AnyContent] = Action.async {
    twitterService.latest().map {
      case TwitterServiceLatestResult.Successful(geoReferencedTweet) =>
        Ok(Json.toJson(geoReferencedTweet))
      case TwitterServiceLatestResult.NoTweetsAvailable => {
        InternalServerError("nta")
      }
    }
  }

  def count: Action[AnyContent] = Action.async {
    twitterService.count().map {
      case count: Int => Ok(Json.obj(
        "numberOfTweets" -> count
      ))
    }.recover {
      case t => InternalServerError("internal server error")
    }
  }

  def langBreakdown: Action[AnyContent] = Action.async {
    twitterService.langBreakdown().map {
      case breakdown: Map[String, Int] => Ok("breakdown")
      case _ => InternalServerError("mah")
    }
  }

}
