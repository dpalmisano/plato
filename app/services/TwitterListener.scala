package services

import javax.inject.Inject

import com.google.inject.{ImplementedBy, Singleton}
import models.{Drop, DropRepository}
import play.api.Logger
import twitter4j.{StallWarning, Status, StatusDeletionNotice, StatusListener}

@ImplementedBy(classOf[TwitterListenerImpl])
trait TwitterListener extends StatusListener {

  def dropRepository: DropRepository

  private val log = Logger("twitter-listener-log")

  override def onStallWarning(warning: StallWarning): Unit = ???

  override def onDeletionNotice(statusDeletionNotice: StatusDeletionNotice): Unit = ()

  override def onScrubGeo(userId: Long, upToStatusId: Long): Unit = ???

  override def onStatus(status: Status): Unit = {
    val drop = Drop.fromStatus(status)
    if(drop.isGeolocalised) {
      val insertResult = dropRepository.insert(drop)
      insertResult.failed.foreach { t =>
        log.error(s"error while inserting drop ${drop.readable} into repo", t)
      }
      insertResult.get
    }
  }

  override def onTrackLimitationNotice(numberOfLimitedStatuses: Int): Unit = ???

  override def onException(ex: Exception): Unit = println(ex.getMessage)
}

@Singleton
class TwitterListenerImpl @Inject()
(val dropRepository: DropRepository)
extends TwitterListener {}
