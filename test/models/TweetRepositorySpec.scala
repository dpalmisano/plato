package models

import java.time.LocalDateTime

import org.scalatest.{BeforeAndAfterAll, Matchers, OptionValues, TryValues}
import org.scalatest.concurrent.Eventually
import org.scalatest.mock.MockitoSugar
import play.api.db.Database

class TweetRepositorySpec extends RepositorySpec("plato-test")
with Matchers
with Eventually
with OptionValues
with TryValues
with BeforeAndAfterAll
with MockitoSugar {

  def withRepository[T](block: (TweetRepository) => T): T = withDatabase {
    testDatabase =>
      val repository = new TweetRepositoryTrait {
        override val database: Database = testDatabase
      }
      block(repository)
  }

  val testTweetId = 1
  val londonGeoPoint = GeoPoint(51.4183, -0.3055)
  val outSideOfLondonGeoPoint = GeoPoint(41.9002, 12.4648) // Rome
  val testCreatedAt = LocalDateTime.now()
  val testLondonTweet = Tweet(
    testTweetId,
    testCreatedAt,
    "test-text",
    Some(londonGeoPoint),
    false,
    "test-lang"
  )
  val testGeoReferencedTweet = GeoReferencedTweet(
    testTweetId,
    testCreatedAt,
    "test-text",
    Some(londonGeoPoint),
    false,
    "test-lang",
    "Kingston upon Thames"
  )

  "DropRepository" should "insert a drop and retrieve it by id" in withRepository {
    repository =>
      val insertResult = repository.insert(testLondonTweet)

      insertResult should be a 'success

      val findResult = repository.findByTweetId(testTweetId)
      findResult should be a 'success
      findResult.get.get.createdAt shouldEqual testGeoReferencedTweet.createdAt
      findResult.get.get shouldEqual testGeoReferencedTweet
  }

  it should "not insert a drop if doesn't fall within london" in withRepository {
    repository =>
      val testOutsideOfLondonDrop = Tweet(
        testTweetId,
        LocalDateTime.now(),
        "test-text",
        Some(outSideOfLondonGeoPoint),
        false,
        "test-lang"
      )

      val insertResult = repository.insert(testOutsideOfLondonDrop)

      insertResult should be a 'success

      val findResult = repository.findByTweetId(testTweetId)
      findResult should be a 'success
      findResult.get shouldEqual None
  }

  it should "get the createdAt of the latest inserted tweet" in withRepository {
    repository =>
      val secondTweetDate = testLondonTweet.createdAt.plusMinutes(1)
      repository.insert(testLondonTweet) should be a 'success

      val secondTweetId = 2
      val secondTweet = Tweet(
        secondTweetId,
        secondTweetDate,
        "test-text",
        Some(londonGeoPoint),
        false,
        "test-lang"
      )
      repository.insert(secondTweet) should be a 'success

      val findResult = repository.findByTweetId(secondTweetId)
      findResult should be a 'success
      findResult.get.get.id shouldEqual secondTweetId
      findResult.get.get.createdAt shouldEqual secondTweetDate

      val latestResult = repository.latest()
      latestResult should be a 'success
      latestResult.get.get.id shouldEqual secondTweetId
      latestResult.get.get.createdAt shouldEqual secondTweetDate
  }

}
