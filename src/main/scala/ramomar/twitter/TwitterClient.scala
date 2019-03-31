package ramomar.twitter

import akka.stream.scaladsl.Source
import akka.util.ByteString
import play.api.libs.oauth.{ConsumerKey, OAuthCalculator, RequestToken}
import play.api.libs.ws._

import scala.concurrent.{ExecutionContext, Future}

class TwitterClient(protected val consumerKey: ConsumerKey,
              protected val token: RequestToken,
              protected val ws: StandaloneWSClient)
             (protected implicit val ec: ExecutionContext)
  extends TwitterService {

  def trackTweets(keywords: Seq[String]): Future[Source[ByteString, _]] = {
    ws.url("https://stream.twitter.com/1.1/statuses/filter.json")
      .withQueryStringParameters("track" -> keywords.mkString(","))
      .sign(OAuthCalculator(consumerKey, token))
      .stream()
      .map { response =>
        val source: Source[ByteString, _] = response.bodyAsSource
        source
    }
  }
}
