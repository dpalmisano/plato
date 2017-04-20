package modules

import java.util.Calendar

import com.google.inject.AbstractModule
import play.api.Logger
import twitter4j.conf.{Configuration, ConfigurationBuilder}
import twitter4j.{TwitterStream, TwitterStreamFactory}

object TwitterStreamFactory {

  val log = Logger("twitter-stream-factory")

  val twitterStream: TwitterStream = {
    log.info("creating twitter client")
    print("creating twitter on: " + Calendar.getInstance().getTime)
    val configuration: Configuration = new ConfigurationBuilder().setDebugEnabled(true)
      .setOAuthConsumerKey("FpfMFuLJyrNZb2ce60yxaQvQX")
      .setOAuthConsumerSecret("hi5erWhGD571fpGrqaCSXRoWpciwVSOsmq06WHs2yhd7xye9PQ")
      .setOAuthAccessToken("14656799-mGm232uwhb55Csm1NAFHfNST35YDgtWOtZKaxCVp3")
      .setOAuthAccessTokenSecret("F9h6z10eGEB4141Yn5QxqbB64XVce91YGDC2ywhlfKYwP")
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
