package ramomar

import scala.concurrent.Future

import org.apache.kafka.clients.producer.RecordMetadata

import ramomar.twitter.Tweet

package object kafka {
  trait TweetsTopicConsumer {
    def subscribe(): Unit
    def consume(): Seq[Tweet]
    def close(): Unit
  }

  trait TweetsTopicProducer {
    def sendTweet(tweet: Tweet): Future[RecordMetadata]
  }
}
