package ramomar

import ramomar.twitter.Tweet

package object kafka {
  trait TweetsTopicConsumer {
    def subscribe(): Unit
    def consume(): Seq[Tweet]
    def close(): Unit
  }

  trait TweetsTopicProducer {
    def sendTweet(tweet: Tweet)
  }
}
