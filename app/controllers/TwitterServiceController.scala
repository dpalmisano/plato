package controllers

import java.time.LocalDateTime
import javax.inject.Inject

import models.TweetRepository
import play.api.mvc.{Action, AnyContent, Controller}
import services.TwitterService

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

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
    val lastInsertFuture = Future {
      tweetRepository.latest()
    }
    lastInsertFuture.map {
      case Success(localDateTime) => Ok(localDateTime.toString)
      case Failure(t) => InternalServerError("shit")
    }
  }

}
