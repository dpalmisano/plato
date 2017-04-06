package controllers

import scala.concurrent.ExecutionContext.Implicits.global
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FlatSpec, Matchers}
import play.api.test.Helpers._
import org.scalatest.mock.MockitoSugar
import play.api.test.FakeRequest
import services.TwitterService

import scala.concurrent.ExecutionContext

class TwitterServiceControllerSpec
extends FlatSpec
with Matchers
with MockitoSugar
with ScalaFutures {

  def testController(mockTwitterService: TwitterService) =
    new TwitterServiceControllerTrait {
      override val twitterService = mockTwitterService
      override implicit val context = global
  }

  "TwitterServiceController" should "start listening to Twitter" in {
    val mockTwitterService = mock[TwitterService]
    val controller = testController(mockTwitterService)

    val request = FakeRequest(GET, "/start")
    val result = call(controller.start, request)
    status(result) shouldEqual OK

  }

}
