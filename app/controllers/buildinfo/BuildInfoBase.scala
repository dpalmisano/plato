package controllers.buildinfo

trait BuildInfoBase {
  val name: String
  val version: String
  val commit: String
  val author: String
  val builtAtString: String
}
