package models

import scala.util.Try

import org.scalatest.{FlatSpec, BeforeAndAfterAll}

import play.api.Logger
import play.api.db.{Database, Databases}
import play.api.db.evolutions.Evolutions

import anorm.SQL

abstract class RepositorySpec(databaseName: String)
  extends FlatSpec
    with BeforeAndAfterAll {

  val log = Logger(s"repository-spec-$databaseName")

  override def beforeAll(): Unit = {
    createPostgresDatabase()
    super.beforeAll()
  }

  override def afterAll(): Unit = {
    super.afterAll()
    dropPostgresDatabase()
  }

  def withDatabase[T](block: Database => T): T = {
    withPostgresDatabase(databaseName) { database =>
      Evolutions.applyEvolutions(database)
      try {
        block(database)
      } finally {
        Evolutions.cleanupEvolutions(database)
      }
    }
  }

  private def createPostgresDatabase(): Unit = {
    withPostgresDatabase("postgres") { database =>
      database.withConnection { implicit conn =>
        Try { SQL(s"""create database "$databaseName" """).execute }
          .recover { case t =>
            log.error(s"Error creating database $databaseName", t)
          }
      }
      () // don't care about the return value
    }
  }

  private def dropPostgresDatabase(): Unit = {
    withPostgresDatabase("postgres") { database =>
      database.withConnection { implicit conn =>
        SQL(s"""drop database if exists "$databaseName" """).execute
      }
      () // don't care about the return value
    }
  }

  private def withPostgresDatabase[T](database: String)(block: Database => T): T = {
    val username = sys.env.getOrElse("PGUSER", sys.env("USER"))
    val password = sys.env.getOrElse("PGPASSWORD", "")
    val port = sys.env.getOrElse("PGPORT", "5432")
    Databases.withDatabase(
      driver = "org.postgresql.Driver",
      url = s"jdbc:postgresql://localhost:$port/$database",
      config = Map(
        "username" -> username,
        "password" -> password
      )) (block)
  }

}
