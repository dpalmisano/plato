package models

import java.time.{LocalDateTime, ZoneId}

import com.google.inject.{ImplementedBy, Inject}
import play.api.db.Database
import twitter4j.Status
import anorm._
import org.postgresql.geometric.PGpoint
import anorm.SqlParser._
import play.api.Logger

import scala.util.Try

case class GeoPoint(lat: Double, long: Double) {
  override def toString = s"[$lat, $long]"
}

abstract class BaseTweet(
  tweetId: Long,
  createdAt: LocalDateTime,
  text: String,
  geoPoint: Option[GeoPoint],
  isRetweet: Boolean,
  lang: String
) {
  def readable: String = s"$tweetId-$createdAt-$lang-$isRetweet-$geoPoint"
  def isGeolocalised: Boolean = geoPoint.isDefined
}

case class Tweet(
  id: Long,
  createdAt: LocalDateTime,
  text: String,
  geoPoint: Option[GeoPoint],
  isRetweet: Boolean,
  lang: String
) extends BaseTweet(id, createdAt, text, geoPoint, isRetweet, lang)

case class GeoReferencedTweet(
  id: Long,
  createdAt: LocalDateTime,
  text: String,
  geoPoint: Option[GeoPoint],
  isRetweet: Boolean,
  lang: String,
  gname: String
) extends BaseTweet(id, createdAt, text, geoPoint, isRetweet, lang)

object Tweet {
  def fromStatus(status:Status): Tweet = {
    val geoLocation = status.getGeoLocation
    val geoPoint = if (geoLocation == null) {
      None
    } else {
      Some(GeoPoint(geoLocation.getLatitude, geoLocation.getLongitude))
    }
    Tweet(
      status.getId,
      status.getCreatedAt.toInstant.atZone(ZoneId.of("Z")).toLocalDateTime,
      status.getText,
      geoPoint,
      status.isRetweeted,
      status.getLang
    )
  }
}

@ImplementedBy(classOf[TweetRepositoryImpl])
trait TweetRepository {
  def insert(tweet: Tweet): Try[Unit]
  def findByTweetId(tweetId: Long): Try[Option[GeoReferencedTweet]]
  def latest(): Try[Option[GeoReferencedTweet]]
  def count(): Try[Int]
}

trait TweetRepositoryTrait extends TweetRepository {

  val log = Logger("drop-repository")

  val database: Database

  private implicit def columnToPGPoint: Column[PGpoint] =
    Column.nonNull1 { (value, meta) =>
      val MetaDataItem(qualified, nullable, clazz) = meta
      value match {
        case pgpoint: PGpoint => Right(pgpoint)
        case _ => Left(TypeDoesNotMatch(s"Cannot convert $value: " +
          s"${value.asInstanceOf[AnyRef].getClass} to PGPoint for column $qualified"))
      }
    }

  private val geoReferencedTweetParser =
      get[Long]("tweet_id") ~
      get[LocalDateTime]("created_at") ~
      get[String]("text") ~
      get[PGpoint]("point") ~
      get[Boolean]("is_retweet") ~
      get[String]("lang") ~
      get[String]("gname") map {
      case tweetId ~ createdAt ~ text ~ point ~ isRetweet ~ lang ~ gname =>
        GeoReferencedTweet(tweetId, createdAt, text, Some(GeoPoint(point.y, point.x)), isRetweet, lang, gname)
    }

  private val localDateTimeParser =
    get[LocalDateTime]("created_at")  map {
      case ldt: LocalDateTime => ldt
      case _ => throw new IllegalStateException()
    }

  override def insert(tweet: Tweet): Try[Unit] = Try {
    val point = tweet.geoPoint.get
    database.withConnection { implicit conn =>
      val roundLat = BigDecimal(point.lat).setScale(4, BigDecimal.RoundingMode.HALF_UP).toDouble
      val roundLong = BigDecimal(point.long).setScale(4, BigDecimal.RoundingMode.HALF_UP).toDouble
      val pointStr = s"POINT($roundLong $roundLat)"
      log.info(s"inserting tweet ${tweet.readable}")
      SQL"""INSERT INTO tweet (tweet_id, created_at, text, point, is_retweet, lang, gid, gname)
           SELECT ${tweet.id}, ${tweet.createdAt}, ${tweet.text}, POINT($roundLong, $roundLat), ${tweet.isRetweet}, ${tweet.lang}, london.gid, london.name FROM london WHERE ST_Contains(london.geom, St_SetSrid($pointStr::geometry, 4326)); """.execute()
    }
    ()
  }

  override def findByTweetId(tweetId: Long): Try[Option[GeoReferencedTweet]] = Try {
    database.withConnection { implicit  conn =>
      log.info(s"retrieving drop with tweet_id $tweetId")
      SQL"""
        SELECT tweet_id, created_at, text, point, is_retweet, lang, gname
        FROM Tweet
        WHERE tweet_id = $tweetId
      """.as(geoReferencedTweetParser.*).headOption
    }
  }

  override def latest(): Try[Option[GeoReferencedTweet]] = Try {
    database.withConnection {  implicit conn =>
      SQL"""
           SELECT tweet_id, created_at, text, point, is_retweet, lang, gid, gname
           FROM tweet
           ORDER BY created_at DESC LIMIT 1;
      """.as(geoReferencedTweetParser.*).headOption
    }
  }

  override def count(): Try[Int] = Try {
    database.withConnection { implicit conn =>
      SQL"""
           SELECT COUNT(*)
           FROM tweet;
      """.as(SqlParser.scalar[Int].single)
    }
  }
}

class TweetRepositoryImpl @Inject()
(val database: Database)
extends TweetRepositoryTrait
