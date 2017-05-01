package modules

import com.google.inject.AbstractModule
import com.typesafe.config.ConfigFactory
import play.api.Logger
import twitter4j.conf.{Configuration, ConfigurationBuilder}
import twitter4j.{TwitterStream, TwitterStreamFactory}

object TwitterStreamFactory {

  val log = Logger("twitter-stream-factory")

  val twitterStream: TwitterStream = {
    val configFile = ConfigFactory.load("prod.conf")
    val config = play.api.Configuration(configFile)
    log.info("creating twitter client")
    val configuration: Configuration = new ConfigurationBuilder().setDebugEnabled(true)
      .setOAuthConsumerKey(config.getString("twitter.oauth.consumer.key").get)
      .setOAuthConsumerSecret(config.getString("twitter.oauth.consumer.secret").get)
      .setOAuthAccessToken(config.getString("twitter.oauth.access.token").get)
      .setOAuthAccessTokenSecret(config.getString("twitter.oauth.access.secret").get)
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
