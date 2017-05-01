package controllers.json

import java.time.LocalDateTime
import play.api.libs.json.{Json, Writes}

import models.{GeoPoint, GeoReferencedTweet}

object ControllerWrites {

  implicit val localDateTimeWrites = new Writes[LocalDateTime] {
    def writes(ldt: LocalDateTime) = Json.obj(
      "startedAt" -> ldt.toString
    )
  }

  implicit val geoPointWrites = new Writes[GeoPoint] {
    def writes(geoPoint: GeoPoint) = Json.obj(
      "lat" -> geoPoint.lat,
      "lon" -> geoPoint.long
    )
  }

  implicit val geoReferencedTweetWrites = new Writes[GeoReferencedTweet] {
    def writes(grt: GeoReferencedTweet) = Json.obj(
      "tweetId" -> grt.id,
      "createdAt" -> grt.createdAt,
      "geoPoint" -> grt.geoPoint,
      "geoName" -> grt.gname,
      "isRetweet" -> grt.isRetweet,
      "text" -> grt.text,
      "lang" -> grt.lang
    )
  }

}
