package org.tycho.scraping

import scala._
import java.nio.ByteBuffer
import java.util.{HashMap, List, Properties}	//importing Map too causes error
import scala.collection.JavaConversions._
import scala.collection.convert._
import org.apache.log4j.{Level, Logger}
import kafka.serializer.StringDecoder
//spark
import org.apache.spark._
import org.apache.spark.streaming._
import org.apache.spark.streaming.StreamingContext._
import org.apache.spark.streaming.kafka._
import org.apache.spark.SparkContext._
import org.apache.spark.SparkConf
import org.apache.spark.storage._
import org.apache.spark.rdd._
//import org.apache.spark.examples.streaming.StreamingExamples
import org.tycho.scraping._
import java.io.File
import org.apache.spark.Logging
import org.tycho.scraping.myUtils._
import org.apache.kafka.clients.producer._
import org.apache.kafka.common.serialization._
//kafka-storm example
import com.twitter.bijection.Injection
import com.twitter.bijection.avro.SpecificAvroCodecs
//import kafka.message.MessageAndMetadata
//import kafka.serializer.DefaultDecoder
import org.apache.commons.io.FileUtils
import scala.collection.mutable
import scala.concurrent.duration._
import scala.language.reflectiveCalls
import org.apache.commons.pool2.impl.{GenericObjectPool, GenericObjectPoolConfig}
//import com.miguno.kafkastorm.kafka.KafkaProducerApp
/*
import com.miguno.avro.Tweet
import com.miguno.kafkastorm.integration.IntegrationTest
import com.miguno.kafkastorm.kafka.{BaseKafkaProducerAppFactory, ConsumerTaskContext, KafkaProducerApp, PooledKafkaProducerAppFactory}
import com.miguno.kafkastorm.logging.LazyLogging
import com.miguno.kafkastorm.spark.serialization.KafkaSparkStreamingRegistrator
import com.miguno.kafkastorm.testing.{EmbeddedKafkaZooKeeperCluster, KafkaTopic}
*/

abstract class SparkKafkaFlow(inputTopic: String = "dumps") extends Serializable {
  
  val kafkaBrokers = "127.0.0.1:9092"
//  val inputTopic = "dumps"
  val outputTopic = this.getClass().getSimpleName()
  val producerKeySer = classOf[StringSerializer]  //IntegerSerializer
  val producerValSer = classOf[StringSerializer]
  val producerProperties = Map(
    "bootstrap.servers" -> "localhost:9092",
    "client.id" -> "DemoProducer",
//  "key.serializer" -> classOf[].getName(),
//    "key.serializer" -> classOf[StringSerializer].getName(),
//    "value.serializer" -> classOf[StringSerializer].getName()
    "key.serializer" -> producerKeySer.getName(),
    "value.serializer" -> producerValSer.getName()
   )
  
//  val sparkCheckpointRootDir = {
//    val r = (new scala.util.Random).nextInt()
//    val path = Seq(System.getProperty("java.io.tmpdir"), "spark-test-checkpoint-" + r).mkString(File.separator)
//    new File(path)
//  }
  
  def prepareSparkStreaming(): StreamingContext = {
    val sparkConf = {
      val topicPartitions = 1
      val cores = topicPartitions + 1
      new SparkConf()
    // .setMaster("spark://master:7077")
      .setMaster(s"local[$cores]")
      .setAppName("SparkKafka")
//      .set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
//      .set("spark.kryo.registrator", classOf[KafkaSparkStreamingRegistrator].getName)
      .set("spark.shuffle.manager", "SORT")
      // false: access in/out from outside the streaming app at the cost of memory
      .set("spark.streaming.unpersist", "true")
      .set("sun.io.serialization.extendedDebugInfo", "true")
    }
    val batchInterval = Seconds(1)
    val ssc = new StreamingContext(sparkConf, batchInterval)
//    ssc.checkpoint(sparkCheckpointRootDir.toString)
    ssc
  }

//  org.apache.spark.streaming.kafka.KafkaRDD
  def processRDD[T](rdd: RDD[T]): RDD[T] = {
//  def processRDD(rdd: RDD[(String,String)]): RDD[(String,String)] = {
//    println("Processing RDD from parent!")
    rdd
  }
  
//  def processPart[T](it: Iterator[T]): Iterator[T] = {
////  def processPart(it: Iterator[(String,String)]): Iterator[(String,String)] = {
////    println("Processing iterating partition from parent!")
//    it
//  }
//  
//  def processKV(tpl: Tuple2[String, String]): Tuple2[String, String] = {
//    tpl match {
//      case (k: String, v: String) => {
////        println("Processing tuple from parent!")
//        (k, v)
//      }
//      case (k, v: String) => {
////        println("Processing keyless tuple from parent!")
//        ("null", v)
//      }
//      case (k, v) => {
////        println("unknown tuple!")
//        (k.toString(), v.toString())
//      }
//    }
//  }

//  def main(args: Array[String]) {
  def run() {
    val ssc = prepareSparkStreaming()

/////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//Kafka (direct) through Spark -- works, but doesn't support groups, meaning this doesn't allow having multiple Spark workers like this pull work from the same queue... so I suppose Spark normally only has one central node pull the work? Not promising for using Spark + Kafka for scraping work...
///shared/spark-1.4.0-bin-hadoop2.6/bin/spark-submit --master local[*] --class org.tycho.fun.SparkKafka /shared/Scala/spark-kafka/target/scala-2.10/spark-kafka-assembly-0.1-SNAPSHOT.jar localhost:9092 test2
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    val producerPool = {
      val pool = new KafkaProducerObjectPool(kafkaBrokers, outputTopic, producerProperties)
      ssc.sparkContext.broadcast(pool)
    }

    val messages = {
      val topics = inputTopic
  		val topicsSet = topics.split(",").toSet
  		val kafkaParams = Map[String, String](
  //			"group.id" -> "group1",	//can I do this?
//        "auto.offset.reset" -> "smallest",  //consume from start
  			"metadata.broker.list" -> kafkaBrokers
  		)
  		KafkaUtils.createDirectStream[String, String, StringDecoder, StringDecoder](ssc, kafkaParams, topicsSet)
    }

		////////////////////////////////////////////

      messages
//      .map { case bytes =>
//        converter.value.invert(bytes) match {
//          case Success(tweet) => tweet
//          case Failure(e) =>
//        }
//      }
      .foreachRDD { rdd =>
//          println("rdd: " + rdd.toString())
//        println(rdd.getClass())
//        rdd
        processRDD(rdd)
        .foreachPartition { part =>
//          println("part: " + part.toString())
//          val p = producer.value  //.borrowObject()
//          val p = producerPool.value.borrowObject()
//          println("borrowing")
          val p = producerPool.value.getDelegate().borrowObject()
          part
//          processPart(part)
          .foreach {
//            case tweet: Tweet =>
//            val bytes = converter.value.apply(tweet)
            item =>
            val (k,v) = item
//            val (k,v) = processKV(item)
            println("Sending message [" + k + ": " + v + "] to topic [" + outputTopic + "]!")
//            p.send(new ProducerRecord[String, String](outputTopic, k, v)).get()
//            p.send(k, v, Option(outputTopic))  //.get()
//            val ser = new StringSerializer()
            p.send(
//              ser.serialize("",k),
//              ser.serialize("",v),
//              producerKeySer.serialize("",k),
//              producerValSer.serialize("",v),
//              (new producerKeySer).serialize("",k),
//              (new producerValSer).serialize("",v),
              (new StringSerializer).serialize("",k),
              (new StringSerializer).serialize("",v),
              Option(outputTopic)
            )  //.get()
//              val props = map2Props(producerProperties)
//              val p = new KafkaProducer[String, String](props)
//              p.send(new ProducerRecord[String, String](outputTopic, k, v)).get()
          }
//          println("returning")
//          try {
            // could failures here cause memory leaks?
            producerPool.value.returnObject(p)
//          }
//          println("Active producers: " + producerPool.value.getNumActive() + ", idle producers: " + producerPool.value.getNumIdle())
        }
      }

		ssc.start()
		ssc.awaitTermination()
    ssc.stop(stopSparkContext = true, stopGracefully = true)
//    FileUtils.deleteQuietly(sparkCheckpointRootDir)

	}
}
