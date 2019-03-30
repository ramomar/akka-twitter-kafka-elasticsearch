package ramomar

import akka.stream.scaladsl.Source
import akka.util.ByteString
import play.api.libs.oauth.{ConsumerKey, RequestToken}
import play.api.libs.ws.StandaloneWSClient

import scala.concurrent.{ExecutionContext, Future}

package object twitter {
  case class Tweet(content: String)

  trait TwitterService {
    protected val ws: StandaloneWSClient
    protected implicit val ec: ExecutionContext

    protected val consumerKey: ConsumerKey
    protected val token: RequestToken

    def trackTweets(keywords: Seq[String]): Future[Source[ByteString, _]]
  }
}
