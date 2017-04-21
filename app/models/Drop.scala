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

case class Drop(
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

object Drop {
  def fromStatus(status:Status): Drop = {
    val geoLocation = status.getGeoLocation
    val geoPoint = if (geoLocation == null) {
      None
    } else {
      Some(GeoPoint(geoLocation.getLatitude, geoLocation.getLongitude))
    }
    Drop(
      status.getId,
      status.getCreatedAt.toInstant.atZone(ZoneId.of("Z")).toLocalDateTime,
      status.getText,
      geoPoint,
      status.isRetweet,
      status.getLang
    )
  }
}

@ImplementedBy(classOf[DropRepositoryImpl])
trait DropRepository {
  def insert(drop: Drop): Try[Unit]
  def findByTweetId(dropId: Long): Try[Option[Drop]]
}

trait DropRepositoryTrait extends DropRepository {

  val log = Logger("drop-repository")

  val database: Database

  override def insert(drop: Drop): Try[Unit] = Try {
    val point = drop.geoPoint.get
    database.withConnection { implicit conn =>
      val roundLat = BigDecimal(point.lat).setScale(4, BigDecimal.RoundingMode.HALF_UP).toDouble
      val roundLong = BigDecimal(point.long).setScale(4, BigDecimal.RoundingMode.HALF_UP).toDouble
      val pointStr = s"POINT($roundLong $roundLat)"
      log.info(s"inserting drop ${drop.readable}")
      SQL"""INSERT INTO tweet (tweet_id, created_at, text, point, is_retweet, lang, gid, gname)
           SELECT ${drop.id}, ${drop.createdAt}, ${drop.text}, POINT($roundLong, $roundLat), ${drop.isRetweet}, ${drop.lang}, london.gid, london.name FROM london WHERE ST_Contains(london.geom, St_SetSrid($pointStr::geometry, 4326)); """.execute()
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

  val dropParser =
    get[Long]("id") ~
    get[LocalDateTime]("created_at") ~
    get[String]("text") ~
    get[PGpoint]("point") ~
    get[Boolean]("is_retweet") ~
    get[String]("lang") map {
      case id ~ createdAt ~ text ~ point ~ isRetweet ~ lang =>
        Drop(id, createdAt, text, Some(GeoPoint(point.y, point.x)), isRetweet, lang)
    }

  override def findByTweetId(tweetId: Long):Try[Option[Drop]] = Try {
    database.withConnection { implicit  conn =>
      log.info(s"retrieving drop with tweet_id $tweetId")
      SQL"""
        SELECT id, created_at, text, point, is_retweet, lang
        FROM Tweet
        WHERE tweet_id = $tweetId
      """.as(dropParser.*).headOption
    }
  }

}

class DropRepositoryImpl @Inject()
(val database: Database)
extends DropRepositoryTrait
