package ramomar.kafka

import java.time.Duration

import com.typesafe.config.Config

import scala.collection.JavaConverters._
import org.apache.kafka.clients.consumer.{ConsumerRecord, KafkaConsumer}

import ramomar.twitter.Tweet

class TweetsConsumer(consumer: KafkaConsumer[String, String], config: Config) extends TweetsTopicConsumer {

  private val topic: String =
    config.getString("consumers.twitter.topic")

  private val pollingTimeout: Duration =
    Duration.ofMillis(config.getLong("consumers.twitter.polling-timeout-ms"))

  def subscribe(): Unit = {
    consumer.subscribe(Seq(topic).asJava)
  }

  def consume(): Seq[Tweet] = {
    val records: Seq[ConsumerRecord[String, String]] = consumer.poll(pollingTimeout).asScala.toList

    records.map(tweet => Tweet(tweet.value()))
  }

  def close(): Unit = {
    consumer.close()
  }
}
