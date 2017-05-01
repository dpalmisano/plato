package services

import java.time.LocalDateTime
import javax.inject.{Inject, Singleton}

import com.google.inject.ImplementedBy
import models.{GeoReferencedTweet, TweetRepository}
import play.api.{Configuration, Logger}
import twitter4j.{FilterQuery, TwitterStream}

import scala.concurrent.{ExecutionContext, Future}

object TwitterService {
  sealed trait TwitterServiceStartStatus

  object TwitterServiceStartStatus {
    case class  Successful(startedAt: LocalDateTime) extends TwitterServiceStartStatus
    case object Failed extends TwitterServiceStartStatus
  }

  sealed trait TwitterServiceStopStatus

  object TwitterServiceStopStatus {
    case class Successful(stoppedAt: LocalDateTime)  extends TwitterServiceStopStatus
    case object Failed      extends TwitterServiceStopStatus
  }

  sealed trait TwitterServiceLatestResult

  object TwitterServiceLatestResult {
    case class Successful(geoReferencedTweet: GeoReferencedTweet) extends TwitterServiceLatestResult
    case object NoTweetsAvailable extends TwitterServiceLatestResult
  }

}

@ImplementedBy(classOf[TwitterServiceImpl])
trait TwitterService {

  import TwitterService._

  val log = Logger("twitter-service")

  def twitterStream: TwitterStream
  def twitterListener: TwitterListener
  def tweetRepository: TweetRepository
  def conf: Configuration
  implicit def context: ExecutionContext

  def start(): Future[TwitterServiceStartStatus] = {
    log.info("starting listening to Twitter")
    twitterStream.addListener(twitterListener)

    /**
      * Greater London bounding box
      */
    val p1 = conf.getDoubleSeq("bounding.london.p1").get.map(_.toDouble).toArray
    val p2 = conf.getDoubleSeq("bounding.london.p2").get.map(_.toDouble).toArray

    log.info(s"listening to tweets from the bounding box: [${p1.mkString(",")}], [${p2.mkString(",")}]")
    twitterStream.filter(new FilterQuery().locations(p1, p2))
    Future.successful(TwitterServiceStartStatus.Successful(LocalDateTime.now()))
  }

  def stop(): Future[TwitterServiceStopStatus] = {
    log.info("stop listening to Twitter")
    twitterStream.cleanUp()
    twitterStream.shutdown()
    Future.successful(TwitterServiceStopStatus.Successful(LocalDateTime.now()))
  }

  def latest(): Future[TwitterServiceLatestResult] = {
    findLatest().map {
      case Some(geoReferencedTweet) => TwitterServiceLatestResult.Successful(geoReferencedTweet)
      case None => TwitterServiceLatestResult.NoTweetsAvailable
    }
  }

  def count(): Future[Int] = Future {
    tweetRepository.count().get
  }

  def langBreakdown(): Future[Map[String, Int]] = Future {
    tweetRepository.langBreakdown().get
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
(val conf: Configuration)
(implicit val context: ExecutionContext)
extends TwitterService {}