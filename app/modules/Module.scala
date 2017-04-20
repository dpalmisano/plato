package modules

import com.google.inject.AbstractModule
import play.api.Logger
import twitter4j.conf.{Configuration, ConfigurationBuilder}
import twitter4j.{TwitterStream, TwitterStreamFactory}

object TwitterStreamFactory {

  val log = Logger("twitter-stream-factory")

  val twitterStream: TwitterStream = {
    log.info("creating twitter client")
    val configuration: Configuration = new ConfigurationBuilder().setDebugEnabled(true)
      .setOAuthConsumerKey("")
      .setOAuthConsumerSecret("")
      .setOAuthAccessToken("")
      .setOAuthAccessTokenSecret("")
      .build()
    new TwitterStreamFactory(configuration).getInstance()
  }
}

class Module extends AbstractModule {

  import TwitterStreamFactory.twitterStream

  override def configure(): Unit = {
    bind(classOf[TwitterStream]).toInstance(twitterStream)
  }
}
