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
  id: Long,
  createdAt: LocalDateTime,
  text: String,
  geoPoint: Option[GeoPoint],
  isRetweet: Boolean,
  lang: String
) {
  def readable: String = s"$id-$createdAt-$lang-$isRetweet-$geoPoint"
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
trait DropRepository {
  def insert(drop: Tweet): Try[Unit]
  def findByTweetId(dropId: Long): Try[Option[Tweet]]
}

trait TweetRepositoryTrait extends DropRepository {

  val log = Logger("drop-repository")

  val database: Database

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

  implicit def columnToPGPoint: Column[PGpoint] =
    Column.nonNull1 { (value, meta) =>
      val MetaDataItem(qualified, nullable, clazz) = meta
      value match {
        case pgpoint: PGpoint => Right(pgpoint)
        case _ => Left(TypeDoesNotMatch(s"Cannot convert $value: " +
          s"${value.asInstanceOf[AnyRef].getClass} to PGPoint for column $qualified"))
      }
    }

  val tweetParser =
    get[Long]("id") ~
    get[LocalDateTime]("created_at") ~
    get[String]("text") ~
    get[PGpoint]("point") ~
    get[Boolean]("is_retweet") ~
    get[String]("lang") map {
      case id ~ createdAt ~ text ~ point ~ isRetweet ~ lang =>
        Tweet(id, createdAt, text, Some(GeoPoint(point.y, point.x)), isRetweet, lang)
    }

  override def findByTweetId(tweetId: Long):Try[Option[Tweet]] = Try {
    database.withConnection { implicit  conn =>
      log.info(s"retrieving drop with tweet_id $tweetId")
      SQL"""
        SELECT id, created_at, text, point, is_retweet, lang
        FROM Tweet
        WHERE tweet_id = $tweetId
      """.as(tweetParser.*).headOption
    }
  }

}

class TweetRepositoryImpl @Inject()
(val database: Database)
extends TweetRepositoryTrait
