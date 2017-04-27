package controllers

import javax.inject.Inject

import models.TweetRepository
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

  implicit val context: ExecutionContext

  def twitterService: TwitterService
  def tweetRepository: TweetRepository

  def start: Action[AnyContent] = Action.async {
    twitterService.start().map {
      case TwitterServiceStartStatus.Successful => Ok("yo")
      case TwitterServiceStartStatus.Failed     => {
        InternalServerError("isr")
      }
    }
  }

  def stop: Action[AnyContent] = Action {
    val blockingResult: Future[TwitterServiceStopStatus] = twitterService.stop()
    blockingResult.value.get.get match {
      case TwitterServiceStopStatus.Successful => Ok("stopped")
      case _ => InternalServerError("isr")
    }
  }

  def lastInsert: Action[AnyContent] = Action.async {
    twitterService.latest().map {
      case TwitterServiceLatestResult.Successful(geoReferencedTweet) => Ok(geoReferencedTweet.readable)
      case TwitterServiceLatestResult.Failed => {
        InternalServerError("isr")
      }
    }
  }

  def count: Action[AnyContent] = Action.async {
    twitterService.count().map {
      case count: Int => Ok(count.toString)
      case _ => InternalServerError("mah")
    }
  }

}
