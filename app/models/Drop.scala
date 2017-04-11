package models

import java.util.Date

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
  createdAt: Date,
  text: String,
  geoPoint: Option[GeoPoint],
  lang: String
) {
  def readable: String = s"$id-$createdAt-$lang-$geoPoint"
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
      status.getCreatedAt,
      status.getText,
      geoPoint,
      status.getLang
    )
  }
}

@ImplementedBy(classOf[DropRepositoryImpl])
trait DropRepository {
  def insert(drop: Drop): Try[Unit]
  def findById(dropId: Long): Try[Drop]
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
      SQL"""INSERT INTO tweet (id, created_at, text, point, lang, gid, gname) SELECT ${drop.id}, ${drop.createdAt}, ${drop.text}, POINT($roundLong, $roundLat), ${drop.lang}, london.gid, london.name FROM london WHERE ST_Contains(london.geom, St_SetSrid($pointStr::geometry, 4326)); """.execute()
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
    get[Date]("created_at") ~
    get[String]("text") ~
    get[PGpoint]("point") ~
    get[String]("lang") map {
      case id ~ createdAt ~ text ~ point ~ lang =>
        Drop(id, createdAt, text, Some(GeoPoint(point.y, point.x)), lang)
    }

  override def findById(dropId: Long):Try[Drop] = Try {
    database.withConnection { implicit  conn =>
      log.info(s"retrieving drop with id $dropId")
      SQL"""
        SELECT id, created_at, text, point, lang
        FROM Tweet
        WHERE id = $dropId
      """.as(dropParser.*).head
    }
  }

}

class DropRepositoryImpl @Inject()
(val database: Database)
extends DropRepositoryTrait
