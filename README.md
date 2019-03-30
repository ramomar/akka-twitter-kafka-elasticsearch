# akka-twitter-kafka-elasticsearch

This is an example app that integrates Twitter, Kafka, and Elasticsearch using Akka Streams.

*Since my objective with this app is to become familiar with the API's, I decided to not use any of the Alpakka plugins and place the producer and the consumer on the same app.*

## Features

- Connect to the Twitter streaming API and produce tweets to an Apache Kafka topic.
- Consume the Kafka topic and write tweets to Elastic search.

## Twitter credentials

You can setup your own Twitter credentials in the `application.conf` located in the `resources` folder.

## Building
Run `sbt compile`

## Running
Run `sbt run`