package controllers

import javax.inject.Inject

import play.api.Logger
import play.api.mvc.{Action, AnyContent, Controller}
import services.TwitterService

import scala.concurrent.{ExecutionContext, Future}

class TwitterServiceController @Inject()
(val twitterService: TwitterService)
(implicit val context: ExecutionContext)
  extends TwitterServiceControllerTrait

trait TwitterServiceControllerTrait extends Controller {

  import services.TwitterService._

  implicit val context: ExecutionContext

  def twitterService: TwitterService

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

}
