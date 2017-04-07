package controllers

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer

import org.scalatest.mock.MockitoSugar
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FlatSpec, Matchers}

import org.mockito.Mockito.when

import play.api.test.Helpers._
import play.api.test.FakeRequest

import services.TwitterService

class TwitterServiceControllerSpec
extends FlatSpec
with Matchers
with MockitoSugar
with ScalaFutures {

  import TwitterService._

  implicit val actorSystem = ActorSystem()
  implicit val materializer = ActorMaterializer()

  def testController(mockTwitterService: TwitterService) =
    new TwitterServiceControllerTrait {
      override val twitterService = mockTwitterService
      override implicit val context = global
  }

  "TwitterServiceController" should "start listening to Twitter" in {
    val mockTwitterService = mock[TwitterService]
    when(mockTwitterService.start())
      .thenReturn(Future.successful(TwitterServiceStartStatus.Successful))
    val controller = testController(mockTwitterService)

    val request = FakeRequest(GET, "/start")
    val result = call(controller.start, request)
    status(result) shouldEqual OK
    contentAsString(result) shouldEqual "yo"
  }

  it should "return INTERNAL_SERVER_ERROR when twitter service start fails" in {
    val mockTwitterService = mock[TwitterService]
    when(mockTwitterService.start())
      .thenReturn(Future.successful(TwitterServiceStartStatus.Failed))
    val controller = testController(mockTwitterService)

    val request = FakeRequest(GET, "/start")
    val result = call(controller.start, request)
    status(result) shouldEqual INTERNAL_SERVER_ERROR
    contentAsString(result) shouldEqual "isr"
  }

  it should "stop listening to Twitter" in {
    val mockTwitterService = mock[TwitterService]
    when(mockTwitterService.stop())
      .thenReturn(Future.successful(TwitterServiceStopStatus.Successful))
    val controller = testController(mockTwitterService)

    val request = FakeRequest(GET, "/stop")
    val result = call(controller.stop, request)
    status(result) shouldEqual OK
    contentAsString(result) shouldEqual "stopped"
  }

  it should "return INTERNAL_SERVER_ERROR when twitter service stop fails" in {
    val mockTwitterService = mock[TwitterService]
    when(mockTwitterService.stop())
      .thenReturn(Future.successful(TwitterServiceStopStatus.Failed))
    val controller = testController(mockTwitterService)

    val request = FakeRequest(GET, "/stop")
    val result = call(controller.stop, request)
    status(result) shouldEqual INTERNAL_SERVER_ERROR
    contentAsString(result) shouldEqual "isr"
  }

}
