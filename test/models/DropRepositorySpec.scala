package models

import java.time.LocalDateTime

import org.scalatest.{BeforeAndAfterAll, Matchers, OptionValues, TryValues}
import org.scalatest.concurrent.Eventually
import org.scalatest.mock.MockitoSugar
import play.api.db.Database

class DropRepositorySpec extends RepositorySpec("plato-test")
with Matchers
with Eventually
with OptionValues
with TryValues
with BeforeAndAfterAll
with MockitoSugar {

  def withRepository[T](block: (DropRepository) => T): T = withDatabase {
    testDatabase =>
      val repository = new DropRepositoryTrait {
        override val database: Database = testDatabase
      }
      block(repository)
  }

  val testDropId = 1
  val londonGeoPoint = GeoPoint(51.4183, -0.3055)
  val outSideOfLondonGeoPoint = GeoPoint(41.9002, 12.4648) // Rome
  val testLondonDrop = Drop(
    testDropId,
    LocalDateTime.now(),
    "test-text",
    Some(londonGeoPoint),
    "test-lang"
  )

  "DropRepository" should "insert a drop and retrieve it by id" in withRepository {
    repository =>
      val insertResult = repository.insert(testLondonDrop)

      insertResult should be a 'success

      val findResult = repository.findById(testDropId)
      findResult should be a 'success
      findResult.get.get.createdAt shouldEqual testLondonDrop.createdAt
      findResult.get.get shouldEqual testLondonDrop
  }

  it should "not insert a drop if doesn't fall within london" in withRepository {
    repository =>
      val testOutsideOfLondonDrop = Drop(
        testDropId,
        LocalDateTime.now(),
        "test-text",
        Some(outSideOfLondonGeoPoint),
        "test-lang"
      )

      val insertResult = repository.insert(testOutsideOfLondonDrop)

      insertResult should be a 'success

      val findResult = repository.findById(testDropId)
      findResult should be a 'success
      findResult.get shouldEqual None
  }

}
