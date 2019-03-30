package ramomar

import java.util.Properties

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Failure
import scala.collection.JavaConverters._

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.GraphDSL
import akka.stream.scaladsl.{Sink, Source}
import org.apache.kafka.clients.consumer.{ConsumerConfig, KafkaConsumer}
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerConfig}
import play.api.libs.oauth.{ConsumerKey, RequestToken}
import play.api.libs.ws.ahc.StandaloneAhcWSClient
import com.sksamuel.elastic4s.http.{ElasticClient, ElasticProperties}
import com.typesafe.config.{Config, ConfigFactory}
import ramomar.elasticsearch.{ElasticSearch, ElasticSearchService}
import ramomar.elasticsearch.{Document => ElasticDocument}
import ramomar.kafka.{TweetsConsumer, TweetsProducer, TweetsTopicConsumer, TweetsTopicProducer}
import ramomar.twitter.{Tweet, Twitter}

// Since I only want to familiarize myself with the APIs it's fine if we have consumer and producer on the same app.

object Main extends App {

  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  implicit val system: ActorSystem = ActorSystem()

  system.registerOnTermination {
    System.exit(0)
  }

  implicit val materializer: ActorMaterializer = ActorMaterializer()

  val config: Config = ConfigFactory.load()

  val wsClient = StandaloneAhcWSClient()

  val consumerKey = ConsumerKey(
    key = config.getString("twitter.consumer-api-keys.api-key"),
    secret = config.getString("twitter.consumer-api-keys.api-secret")
  )
  val token = RequestToken(
    token = config.getString("twitter.access-tokens.token"),
    secret = config.getString("twitter.access-tokens.secret")
  )

  val twitterClient = new Twitter(consumerKey, token, wsClient)

  val elasticSearchClient: ElasticClient = ElasticClient(
    ElasticProperties(config.getString("elasticsearch.host"))
  )

  val producerProps: Properties = {
    val props = new Properties()

    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, config.getString("producers.twitter.bootstrap-server"))
    props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, config.getString("producers.twitter.enable-idempotence"))
    props.put(ProducerConfig.ACKS_CONFIG, config.getString("producers.twitter.acks"))
    props.put(ProducerConfig.RETRIES_CONFIG, config.getInt("producers.twitter.retries"))
    props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, config.getInt("producers.twitter.max-inflight-requests"))
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, config.getString("producers.twitter.key-serializer-class"))
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, config.getString("producers.twitter.value-serializer-class"))
    props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, config.getString("producers.twitter.compression-type"))
    props.put(ProducerConfig.LINGER_MS_CONFIG, config.getInt("producers.twitter.linger-ms"))
    props.put(ProducerConfig.BATCH_SIZE_CONFIG, config.getInt("producers.twitter.batch-size"))


    props
  }

  val kafkaProducer: KafkaProducer[String, String] = new KafkaProducer[String, String](producerProps)

  val consumerProps: Properties = {
    val props = new Properties()

    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, config.getString("consumers.twitter.bootstrap-server"))
    props.put(ConsumerConfig.GROUP_ID_CONFIG, config.getString("consumers.twitter.group-id"))
    props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, config.getString("consumers.twitter.enable-auto-commit"))
    props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, config.getString("consumers.twitter.auto-commit-interval-ms"))
    props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, config.getString("consumers.twitter.session-timeout-ms"))
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, config.getString("consumers.twitter.key-deserializer-class"))
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, config.getString("consumers.twitter.key-deserializer-class"))

    props
  }

  val kafkaConsumer: KafkaConsumer[String, String] = new KafkaConsumer[String, String](consumerProps)

  val tweetsProducer: TweetsTopicProducer = new TweetsProducer(kafkaProducer, config)

  val tweetsConsumer: TweetsTopicConsumer = new TweetsConsumer(kafkaConsumer, config)

  val elasticSearch: ElasticSearchService = new ElasticSearch(elasticSearchClient)

  // Connect to Twitter streaming API and create a Source.
  val tweetsSource: Source[String, Future[NotUsed]] = Source.fromFutureSource[String, NotUsed] {
    twitterClient.trackTweets(config.getStringList("twitter.client.tracking-words").asScala).map { response =>
      GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
        builder.add(response.map(_.utf8String))
      }
    }
  }

  // Produce tweets in a Kafka topic.
  tweetsSource.runWith {
    Sink.foreach(r => tweetsProducer.sendTweet(Tweet(r)))
  }

  // Consume Kafka topic of tweets.
  val tweetsTopicSource: Source[Seq[Tweet], NotUsed] = Source.unfoldResource[Seq[Tweet], TweetsTopicConsumer](
    create = () => { tweetsConsumer.subscribe(); tweetsConsumer },
    read   = consumer => Option(consumer.consume()),
    close  = consumer => consumer.close()
  )

  // Index tweets in ElasticSearch.
  tweetsTopicSource
    .filter(_.nonEmpty)
    .wireTap(ts => println(ts.map(_.content).mkString("\n")))
    .runWith {
      Sink.foreach[Seq[Tweet]] { tweets =>
        val documents = tweets.map(t => ElasticDocument("tweets", "tweet", Seq("raw_json" -> t.content)))
        if (documents.nonEmpty) {
          elasticSearch.indexDocuments(documents).onComplete {
            case Failure(e) => println(e)
            case _ => ()
          }
        }
      }
    }
}
