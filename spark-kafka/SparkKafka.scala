package org.tycho.fun

import java.nio.ByteBuffer
import java.util.{HashMap, List, Properties}	//importing Map too causes error
import scala.collection.JavaConversions._
import kafka.serializer.StringDecoder

///*
import org.apache.spark._
import org.apache.spark.streaming._
import org.apache.spark.streaming.StreamingContext._
import org.apache.spark.streaming.kafka._
import org.apache.spark.SparkContext._
import org.apache.spark.SparkConf
import org.apache.spark.examples.streaming.StreamingExamples
import org.apache.spark.Logging

object SparkKafka {
	def main(args: Array[String]) {
//		StreamingExamples.setStreamingLogLevels()
		val sparkConf = new SparkConf().setAppName("SparkKafka")
		val ssc = new StreamingContext(sparkConf, Seconds(2))

/////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//Kafka (direct) through Spark -- works, but doesn't support groups, meaning this doesn't allow having multiple Spark workers like this pull work from the same queue... so I suppose Spark normally only has one central node pull the work? Not promising for using Spark + Kafka for scraping work...
///shared/spark-1.4.0-bin-hadoop2.6/bin/spark-submit --master local[*] --class org.tycho.fun.SparkKafka /shared/Scala/spark-kafka/target/scala-2.10/spark-kafka-assembly-0.1-SNAPSHOT.jar localhost:9092 test2
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////

		if (args.length != 2) {
			System.err.println("Usage: SparkKafka <brokers> <topics>")
			System.exit(1)
		}

		val Array(brokers, topics) = args
		val topicsSet = topics.split(",").toSet
		val kafkaParams = Map[String, String](
			//"group.id" -> "group1",	//can I do this?
			"metadata.broker.list" -> brokers
		)
		val messages = KafkaUtils.createDirectStream[String, String, StringDecoder, StringDecoder](ssc, kafkaParams, topicsSet)

		////////////////////////////////////////////

		val lines = messages.map(_._2)
		val words = lines.flatMap(_.split(" "))
		val wordCounts = words.map(x => (x, 1L)).reduceByKey(_ + _)
		wordCounts.print()

/////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Kafka (receiver) through Spark -- doesn't seem to really get/show the messages?
///shared/spark-1.4.0-bin-hadoop2.6/bin/spark-submit --master local[*] --class org.tycho.fun.SparkKafka /shared/Scala/spark-kafka/target/scala-2.10/spark-kafka-assembly-0.1-SNAPSHOT.jar localhost:2181 group1 test2 1
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////

//		if (args.length < 4) {
//			System.err.println("Usage: SparkKafka <zkQuorum> <group> <topics> <numThreads>")
//			System.exit(1)
//		}
//
//		val Array(zkQuorum, group, topics, numThreads) = args
//		ssc.checkpoint("checkpoint")
//		val topicMap = topics.split(",").map((_, numThreads.toInt)).toMap
//		val lines = KafkaUtils.createStream(ssc, zkQuorum, group, topicMap).map(_._2)
//
//		////////////////////////////////////////////
//
//		val words = lines.flatMap(_.split(" "))
//		words.print()
//		val wordCounts = words.map(x => (x, 1L))
//			.reduceByKeyAndWindow(_ + _, _ - _, Seconds(10), Seconds(2), 2)
//		wordCounts.print()

/////////////////////////////////////////////////////////////////////////////////////////////////////////////////

		ssc.start()
		ssc.awaitTermination()
	}
}

/////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// raw Kafka -- only one of two receiving stuff at a time, even with partitioned topic?
//java -jar /shared/Scala/spark-kafka/target/scala-2.10/spark-kafka-assembly-0.1-SNAPSHOT.jar test2
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////

//import kafka.consumer._
//import kafka.javaapi.consumer.ConsumerConnector
//import kafka.message.MessageAndMetadata
//
//import Consumer._
//
//object Consumer {
//	private def createConsumerConfig(): ConsumerConfig = {
//		val props = new Properties()
//		val settings = Map[String, String](
//			"zookeeper.connect" -> "127.0.0.1:2181",
//			"group.id" -> "group1",
//			"zookeeper.session.timeout.ms" -> "400",
//			"zookeeper.sync.time.ms" -> "200",
//			"auto.commit.interval.ms" -> "1000"
//		)
//		for ((k,v) <- settings)
//			props.put(k, v)
//		new ConsumerConfig(props)
//	}
//}
//
//class Consumer(private val topic: String) extends Thread {
//	private val consumer = kafka.consumer.Consumer.createJavaConsumerConnector(createConsumerConfig())
//
//	override def run() {
//		val topicCountMap = new HashMap[String, Integer]()
//		topicCountMap.put(topic, 1)
//		val consumerMap = consumer.createMessageStreams(topicCountMap)
//		val stream = consumerMap.get(topic).get(0)
//		for (msg <- stream) {
//			//println("Key: " + ByteBuffer.wrap(msg.key()).getInt)
//			println("Key: " + msg.key())
//			println("Msg: " + new String(msg.message()))
//		}
//	}
//}
//
//object SparkKafka {
//	def main(args: Array[String]) = {
//		val topic = "test2";
//		val consumerThread = new Consumer(topic);
//		consumerThread.start();
//	}
//}

