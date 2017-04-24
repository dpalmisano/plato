package services

import java.util.Date

import scala.util.Success

import org.scalatest.FlatSpec
import org.scalatest.mock.MockitoSugar

import org.mockito.Mockito.{ when, verify, times, never }
import org.mockito.ArgumentMatchers.any

import twitter4j.{ GeoLocation, Status }

import models.{Tweet, TweetRepository}

class TwitterListenerSpec
extends FlatSpec
with MockitoSugar {

  private val testLat = 10.0
  private val testLong = 20.0
  private val testStatusId = 1
  private val testCreatedAt = new Date()
  private val testText = "test-text"
  private val testLang = "test-lang"

  private def listener(mockDropRepository: TweetRepository) = new TwitterListener {
    override val tweetRepository = mockDropRepository
  }

  def status(geoLocation: GeoLocation):Status = {
    val status = mock[Status]
    when(status.getGeoLocation).thenReturn(geoLocation)
    when(status.getId).thenReturn(testStatusId)
    when(status.getCreatedAt).thenReturn(testCreatedAt)
    when(status.getText).thenReturn(testText)
    when(status.getLang).thenReturn(testLang)
    status
  }

  "TwitterListener" should "insert a drop into the repository" in {
    val mockDropRepository = mock[TweetRepository]
    when(mockDropRepository.insert(any[Tweet])).thenReturn(Success(()))
    val twitterListener = listener(mockDropRepository)

    val mockStatus = status(new GeoLocation(testLat, testLong))

    twitterListener.onStatus(mockStatus)
    verify(mockDropRepository, times(1)).insert(any[Tweet])
  }

  it should "not insert into the repository if the tweet is not geolocalised" in {
    val mockDropRepository = mock[TweetRepository]
    val twitterListener = listener(mockDropRepository)

    val mockStatus = status(null)

    twitterListener.onStatus(mockStatus)
    verify(mockDropRepository, never()).insert(any[Tweet])
  }

}
