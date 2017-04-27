package services

import javax.inject.{Inject, Singleton}

import com.google.inject.ImplementedBy
import models.{GeoReferencedTweet, TweetRepository}
import play.api.Logger
import twitter4j.{FilterQuery, TwitterStream}

import scala.concurrent.{ExecutionContext, Future}

object TwitterService {
  sealed trait TwitterServiceStartStatus

  object TwitterServiceStartStatus {
    case object Successful  extends TwitterServiceStartStatus
    case object Failed      extends TwitterServiceStartStatus
  }

  sealed trait TwitterServiceStopStatus

  object TwitterServiceStopStatus {
    case object Successful  extends TwitterServiceStopStatus
    case object Failed      extends TwitterServiceStopStatus
  }

  sealed trait TwitterServiceLatestResult

  object TwitterServiceLatestResult {
    case class Successful(geoReferencedTweet: GeoReferencedTweet) extends TwitterServiceLatestResult
    case object Failed extends TwitterServiceLatestResult
  }

}

@ImplementedBy(classOf[TwitterServiceImpl])
trait TwitterService {

  import TwitterService._

  val log = Logger("twitter-service")

  def twitterStream: TwitterStream
  def twitterListener: TwitterListener
  def tweetRepository: TweetRepository
  implicit def context: ExecutionContext

  def start(): Future[TwitterServiceStartStatus] = {
    log.info("starting listening to Twitter")
    twitterStream.addListener(twitterListener)

    /**
      * Greater London bounding box
      */
    val p1 = Array(-0.489, 51.28)
    val p2 = Array(0.236, 51.686)

    twitterStream.filter(new FilterQuery().locations(p1, p2))

    Future.successful(TwitterServiceStartStatus.Successful)
  }

  def stop(): Future[TwitterServiceStopStatus] = {
    log.info("stop listening to Twitter")
    twitterStream.cleanUp()
    twitterStream.shutdown()
    Future.successful(TwitterServiceStopStatus.Successful)
  }

  def latest(): Future[TwitterServiceLatestResult] = {
    findLatest().map {
      case Some(geoReferencedTweet) => TwitterServiceLatestResult.Successful(geoReferencedTweet)
      case None => TwitterServiceLatestResult.Failed
    }
  }

  def count(): Future[Int] = Future {
    tweetRepository.count().get
  }

  private def findLatest(): Future[Option[GeoReferencedTweet]] = Future {
    tweetRepository.latest().get
  }

}

@Singleton
class TwitterServiceImpl @Inject()
(val twitterStream: TwitterStream)
(val twitterListener: TwitterListener)
(val tweetRepository: TweetRepository)
(implicit val context: ExecutionContext)
extends TwitterService {}