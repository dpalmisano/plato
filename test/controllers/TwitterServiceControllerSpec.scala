package controllers

import java.time.LocalDateTime

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import models.{GeoPoint, GeoReferencedTweet}
import org.scalatest.mock.MockitoSugar
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FlatSpec, Matchers}
import org.mockito.Mockito.when
import play.api.libs.json.Json
import play.api.test.Helpers._
import play.api.test.FakeRequest
import services.TwitterService

class TwitterServiceControllerSpec
extends FlatSpec
with Matchers
with MockitoSugar
with ScalaFutures {

  import TwitterService._
  import controllers.json.ControllerWrites._

  implicit val actorSystem = ActorSystem()
  implicit val materializer = ActorMaterializer()

  def testController(mockTwitterService: TwitterService) =
    new TwitterServiceControllerTrait {
      override val twitterService = mockTwitterService
      override implicit val context = global
  }

  private case object IntentionalException extends Exception("intentional-exception")

  private val testCreatedAt = LocalDateTime.now()
  private val testGeoPoint = GeoPoint(1, 1)
  private val testGeoReferencedTweet = GeoReferencedTweet(
    1,
    testCreatedAt,
    "test-text",
    Some(testGeoPoint),
    false,
    "it",
    "test-gname"
  )

  "TwitterServiceController" should "start listening to Twitter" in {
    val mockTwitterService = mock[TwitterService]
    when(mockTwitterService.start())
      .thenReturn(Future.successful(TwitterServiceStartStatus.Successful(testCreatedAt)))
    val controller = testController(mockTwitterService)

    val request = FakeRequest(GET, "/start")
    val result = call(controller.start, request)
    status(result) shouldEqual OK
    contentAsJson(result) shouldEqual Json.toJson(testCreatedAt)
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
      .thenReturn(Future.successful(TwitterServiceStopStatus.Successful(testCreatedAt)))
    val controller = testController(mockTwitterService)

    val request = FakeRequest(GET, "/stop")
    val result = call(controller.stop, request)
    status(result) shouldEqual OK
    contentAsJson(result) shouldEqual Json.toJson(testCreatedAt)
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

  it should "return number of tweets" in {
    val mockTwitterService = mock[TwitterService]
    when(mockTwitterService.count())
      .thenReturn(Future.successful(1))
    val controller = testController(mockTwitterService)

    val request = FakeRequest(GET, "/count")
    val result = call(controller.count, request)
    status(result) shouldEqual OK
    contentAsJson(result) shouldEqual Json.obj(
      "numberOfTweets" -> 1
    )
  }

  it should "return INTERNAL_SERVER_ERROR when number of tweets fails" in {
    val mockTwitterService = mock[TwitterService]
    when(mockTwitterService.count())
      .thenReturn(Future.failed(IntentionalException))
    val controller = testController(mockTwitterService)

    val request = FakeRequest(GET, "/count")
    val result = call(controller.count, request)
    status(result) shouldEqual INTERNAL_SERVER_ERROR
    contentAsString(result) shouldEqual "internal server error"
  }

  it should "return latest tweet" in {
    val mockTwitterService = mock[TwitterService]
    when(mockTwitterService.latest())
      .thenReturn(Future.successful(TwitterServiceLatestResult.Successful(testGeoReferencedTweet)))
    val controller = testController(mockTwitterService)

    val request = FakeRequest(GET, "/latest")
    val result = call(controller.lastInsert, request)
    status(result) shouldEqual OK
    contentAsJson(result) shouldEqual Json.toJson(
      testGeoReferencedTweet
    )
  }

  it should "return empty JSON when no tweets are available" in {
    val mockTwitterService = mock[TwitterService]
    when(mockTwitterService.latest())
      .thenReturn(Future.successful(TwitterServiceLatestResult.NoTweetsAvailable))
    val controller = testController(mockTwitterService)

    val request = FakeRequest(GET, "/latest")
    val result = call(controller.lastInsert, request)
    status(result) shouldEqual NO_CONTENT
  }

  it should "return INTERNAL_SERVER_ERROR when last insert fails" in {
    val mockTwitterService = mock[TwitterService]
    when(mockTwitterService.latest())
      .thenReturn(Future.failed(IntentionalException))
    val controller = testController(mockTwitterService)

    val request = FakeRequest(GET, "/latest")
    val result = call(controller.lastInsert, request)
    status(result) shouldEqual INTERNAL_SERVER_ERROR
    contentAsString(result) shouldEqual "internal server error"
  }

}
