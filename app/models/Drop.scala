package models

import java.util.Date

import com.google.inject.{ImplementedBy, Inject}
import play.api.db.Database
import twitter4j.Status
import anorm._
import play.api.Logger

import scala.util.Try

case class GeoPoint(lat: Double, long: Double)

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
}

trait DropRepositoryTrait extends DropRepository {

  val log = Logger("repo")

  val database: Database

  override def insert(drop: Drop): Try[Unit] = Try {
    val point = drop.geoPoint.get
    database.withConnection { implicit conn =>
      val roundLat = BigDecimal(point.lat).setScale(4, BigDecimal.RoundingMode.HALF_UP).toDouble
      val roundLong = BigDecimal(point.long).setScale(4, BigDecimal.RoundingMode.HALF_UP).toDouble
      val pointStr = s"POINT($roundLong $roundLat)"
      println("[sql] before")
        val result = SQL""" INSERT INTO tweet (id, created_at, text, point, lang, gid, gname) SELECT ${drop.id}, ${drop.createdAt}, ${drop.text}, POINT($roundLong, $roundLat), ${drop.lang}, london.gid, london.name FROM london WHERE ST_Contains(london.geom, St_SetSrid($pointStr::geometry, 4326)); """.execute()
      println(result)
    }
    println("[sql] after")
    ()
  }
}

class DropRepositoryImpl @Inject()
(val database: Database)
extends DropRepositoryTrait
