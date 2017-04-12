package controllers

import controllers.buildinfo.BuildInfoBase
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.{Action, AnyContent, Controller}

trait HasVersionEndPoint {
  this: Controller => // Must be injected in a controller

  val buildInfoObj: BuildInfoBase

  private def versionAsJson: JsObject = Json.obj(
    "name" -> buildInfoObj.name,
    "version" -> buildInfoObj.version,
    "commit" -> buildInfoObj.commit,
    "build-time" -> buildInfoObj.builtAtString,
    "build-author" -> buildInfoObj.author
  )

  def version: Action[AnyContent] = Action { Ok(versionAsJson) }
}
