package modules

import com.google.inject.AbstractModule
import twitter4j.conf.{Configuration, ConfigurationBuilder}
import twitter4j.{TwitterStream, TwitterStreamFactory}

object TwitterStreamFactory {

  val twitterStream: TwitterStream = {
    println("creating tiwtter")
    val configuration: Configuration = new ConfigurationBuilder().setDebugEnabled(true)
      .setOAuthConsumerKey("YLvJsmRwEYpLwmJ1mtVwHeCB2")
      .setOAuthConsumerSecret("wG4VafHZAwnUUpct1oPTArQrPBrboHFJt4vHyJ4dR67rMsJxEN")
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
