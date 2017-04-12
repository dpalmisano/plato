package models

import java.time.LocalDateTime

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

  val testDropId = 1
  val testDrop = Drop(
    testDropId,
    LocalDateTime.now(),
    "test-text",
    Some(GeoPoint(51.4183, -0.3055)),
    "test-lang"
  )

  "DropRepository" should "insert a drop and retrieve it by id" in withRepository {
    repository =>
      val insertResult = repository.insert(testDrop)

      insertResult should be a 'success

      val findResult = repository.findById(testDropId)
      findResult should be a 'success
      findResult.get.createdAt shouldEqual testDrop.createdAt
      findResult.get shouldEqual testDrop
  }

}
