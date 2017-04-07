package models

import java.util.Date

import org.scalatest.{Matchers, OptionValues, TryValues}
import org.scalatest.concurrent.Eventually
import org.scalatest.mock.MockitoSugar
import play.api.db.Database

class DropRepositorySpec extends RepositorySpec("plato-test")
with Matchers
with Eventually
with OptionValues
with TryValues
with MockitoSugar {

  def withRepository[T](block: (DropRepository) => T): T = withDatabase {
    testDatabase =>
      val repository = new DropRepositoryTrait {
        override val database: Database = testDatabase
      }
      block(repository)
  }

  "DropRepository" should "insert a drop" in withRepository {
    repository =>
      val drop = Drop(1, new Date(), "test-text", Some(GeoPoint(1.0, 2.0)), "test-lang")
      val insertResult = repository.insert(drop)

      insertResult should be a 'success
  }

}
