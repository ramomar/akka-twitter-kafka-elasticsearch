package ramomar.kafka

import scala.concurrent.{Future, Promise}

import com.typesafe.config.Config
import org.apache.kafka.clients.producer.{Callback, KafkaProducer, ProducerRecord, RecordMetadata}

import ramomar.twitter.Tweet

class TweetsProducer(producer: KafkaProducer[String, String],
                     config: Config) extends TweetsTopicProducer {
  private val topic = config.getString("producers.twitter.topic")

  def sendTweet(tweet: Tweet): Future[RecordMetadata] = {
    val record = new ProducerRecord[String, String](topic, tweet.content)

    val p: Promise[RecordMetadata] = Promise()

    producer.send(record, new OnSendCallback(p))

    p.future
  }

  private class OnSendCallback(promise: Promise[RecordMetadata]) extends Callback {
    def onCompletion(metadata: RecordMetadata, exception: Exception): Unit = Option(exception) match {
      case Some(e) => promise.failure(e)
      case None    => promise.success(metadata)
    }
  }
}
