package services

import scala.concurrent.ExecutionContext.Implicits.global

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FlatSpec, Matchers}
import org.scalatest.mock.MockitoSugar

import org.mockito.Mockito.{times, verify}

import twitter4j.{FilterQuery, TwitterStream}

class TwitterServiceSpec
extends FlatSpec
with Matchers
with MockitoSugar
with ScalaFutures
{

  import TwitterService._

  private def service(
    mockTwitterStream: TwitterStream,
    mockTwitterListener: TwitterListener = mock[TwitterListener]
  ) = new TwitterService {
    override val twitterStream = mockTwitterStream
    override val twitterListener = mockTwitterListener
    override implicit val context = global
  }

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
    result.futureValue shouldEqual TwitterServiceStartStatus.Successful
    verify(mockTwitterStream, times(1)).addListener(mockTwitterListener)
    verify(mockTwitterStream, times(1)).filter(query)
  }

  it should "stop listening to Twitter" in {
    val mockTwitterStream = mock[TwitterStream]
    val svc = service(mockTwitterStream)

    val result = svc.stop()
    result.futureValue shouldEqual TwitterServiceStopStatus.Successful
    verify(mockTwitterStream, times(1)).cleanUp()
    verify(mockTwitterStream, times(1)).shutdown()
  }

}
