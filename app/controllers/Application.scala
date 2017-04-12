package controllers

import controllers.buildinfo.BuildInfo
import play.api.mvc.Controller

class Application extends Controller with HasVersionEndPoint {
  val buildInfoObj = BuildInfo
}
