package services

import java.time.LocalDateTime

import com.typesafe.config.ConfigFactory
import models.{GeoPoint, GeoReferencedTweet, TweetRepository}

import scala.concurrent.ExecutionContext.Implicits.global
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FlatSpec, Matchers}
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito.{times, verify, when}
import play.api.Configuration
import twitter4j.{FilterQuery, TwitterStream}

import scala.util.{Failure, Success}

class TwitterServiceSpec
extends FlatSpec
with Matchers
with MockitoSugar
with ScalaFutures
{

  import TwitterService._

  private def service(
    mockTwitterStream: TwitterStream,
    mockTwitterListener: TwitterListener = mock[TwitterListener],
    mockTweetRepository: TweetRepository = mock[TweetRepository],
    mockConf: Configuration = mock[Configuration]
  ) = new TwitterService {
    override val twitterStream = mockTwitterStream
    override val twitterListener = mockTwitterListener
    override val tweetRepository = mockTweetRepository
    override def conf: Configuration = {
      val testConfig = ConfigFactory.load("test.conf")
      Configuration(testConfig)
    }
    override implicit val context = global
  }

  private val testGeoReferencedTweet = GeoReferencedTweet(
    1,
    LocalDateTime.now(),
    "test-text",
    Some(GeoPoint(1,1)),
    false,
    "it",
    "test-location"
  )

  private case object IntentionalException extends Exception("intentional-exception")

  "TwitterService" should "start listening to Twitter" in {

    /**
      * Greater London bounding box
      */
    val p1 = Array(-0.489, 51.28)
    val p2 = Array(0.236, 51.686)
    val query = new FilterQuery().locations(p1, p2)

    val mockTwitterStream = mock[TwitterStream]
    val mockTwitterListener = mock[TwitterListener]
    val svc = service(mockTwitterStream, mockTwitterListener)

    val result = svc.start()
    result.futureValue match {
      case TwitterServiceStartStatus.Successful(_: LocalDateTime) => assert(true)
      case _ => fail("response is not a date")
    }
    verify(mockTwitterStream, times(1)).addListener(mockTwitterListener)
    verify(mockTwitterStream, times(1)).filter(query)
  }

  it should "stop listening to Twitter" in {
    val mockTwitterStream = mock[TwitterStream]
    val svc = service(mockTwitterStream)

    val result = svc.stop()
    result.futureValue match {
      case TwitterServiceStopStatus.Successful(_: LocalDateTime) => assert(true)
      case _ => fail("response is not a date")
    }
    verify(mockTwitterStream, times(1)).cleanUp()
    verify(mockTwitterStream, times(1)).shutdown()
  }

  it should "return total count of tweets" in {
    val mockTwitterStream = mock[TwitterStream]
    val mockTweetRepository = mock[TweetRepository]
    when(mockTweetRepository.count()).thenReturn(Success(1))
    val svc = service(mockTwitterStream, mockTweetRepository = mockTweetRepository)

    val result = svc.count()
    result.futureValue shouldEqual 1
  }

  it should "return INTENTIONAL exception when repo fails on count" in {
    val mockTwitterStream = mock[TwitterStream]
    val mockTweetRepository = mock[TweetRepository]
    when(mockTweetRepository.count()).thenReturn(Failure(IntentionalException))
    val svc = service(mockTwitterStream, mockTweetRepository = mockTweetRepository)

    val result = svc.count()
    result.failed.futureValue shouldEqual IntentionalException
  }

  it should "return latest inserted tweet" in {
    val mockTwitterStream = mock[TwitterStream]
    val mockTweetRepository = mock[TweetRepository]
    when(mockTweetRepository.latest()).thenReturn(Success(Some(testGeoReferencedTweet)))
    val svc = service(mockTwitterStream, mockTweetRepository = mockTweetRepository)

    val result = svc.latest()
    result.futureValue shouldEqual TwitterServiceLatestResult.Successful(testGeoReferencedTweet)
  }

  it should "return NoTweetsAvailble when there are no tweets" in {
    val mockTwitterStream = mock[TwitterStream]
    val mockTweetRepository = mock[TweetRepository]
    when(mockTweetRepository.latest()).thenReturn(Success(None))
    val svc = service(mockTwitterStream, mockTweetRepository = mockTweetRepository)

    val result = svc.latest()
    result.futureValue shouldEqual TwitterServiceLatestResult.NoTweetsAvailable
  }

  it should "return INTENTIONAL exception when repo fails on latest tweet" in {
    val mockTwitterStream = mock[TwitterStream]
    val mockTweetRepository = mock[TweetRepository]
    when(mockTweetRepository.latest()).thenReturn(Failure(IntentionalException))
    val svc = service(mockTwitterStream, mockTweetRepository = mockTweetRepository)

    val result = svc.latest()
    result.failed.futureValue shouldEqual IntentionalException
  }

  it should "return lang breakdown" in {
    val testLangBreakdownMap = Map(
      "it" -> 2,
      "en" -> 1
    )
    val mockTwitterStream = mock[TwitterStream]
    val mockTweetRepository = mock[TweetRepository]
    when(mockTweetRepository.langBreakdown()).thenReturn(Success(testLangBreakdownMap))
    val svc = service(mockTwitterStream, mockTweetRepository = mockTweetRepository)

    val result = svc.langBreakdown()
    result.futureValue shouldEqual testLangBreakdownMap
  }

  it should "manage an error on lang breakdown" in {
    val mockTwitterStream = mock[TwitterStream]
    val mockTweetRepository = mock[TweetRepository]
    when(mockTweetRepository.langBreakdown()).thenReturn(Failure(IntentionalException))
    val svc = service(mockTwitterStream, mockTweetRepository = mockTweetRepository)

    val result = svc.langBreakdown()
    result.failed.futureValue shouldEqual IntentionalException
  }

}
