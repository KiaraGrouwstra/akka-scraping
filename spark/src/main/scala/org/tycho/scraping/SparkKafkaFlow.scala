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
import org.apache.commons.io.FileUtils
import scala.collection.mutable
import scala.concurrent.duration._
import scala.language.reflectiveCalls
import org.apache.commons.pool2.impl.{GenericObjectPool, GenericObjectPoolConfig}
//import com.miguno.avro.Tweet

abstract class SparkKafkaFlow(inputTopic: String = "dumps") extends Serializable {
//How do I detect dependent flows so I could batch reset them on change?

//  implicit class RDDStringPlus(val rdd: RDD[String]) {
//      def parseJson(sqlContext: SqlContext) = sqlContext.read.json(rdd)
//  }
//  val sc: SparkContext // An existing SparkContext.
//  val sqlContext = new org.apache.spark.sql.SQLContext(sc)
//  // createSchemaRDD is used to implicitly convert an RDD to a SchemaRDD.
//  import sqlContext.createSchemaRDD
  
  val kafkaBrokers = "127.0.0.1:9092"
//  val inputTopic = "dumps"
  val outputTopic = this.getClass().getSimpleName()
  val producerKeySer = classOf[StringSerializer]  //IntegerSerializer
  val producerValSer = classOf[StringSerializer]
  
  val producerProperties = Map(
    "bootstrap.servers" -> "localhost:9092",
    "client.id" -> "DemoProducer",
    "key.serializer" -> producerKeySer.getName(),
    "value.serializer" -> producerValSer.getName()
  )
  
  case class SrlzrSet[A,B](keySer: Class[_ <: Serializer[A]], valSer: Class[_ <: Serializer[B]])
  
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

  def processRDD[T](rdd: RDD[T]): RDD[T] = {
//    println("Processing RDD from parent!")
    rdd
  }
  
  def run() {
    val ssc = prepareSparkStreaming()

/////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//Kafka (direct) through Spark -- works, but doesn't support groups, meaning this doesn't allow having multiple Spark workers like this pull work from the same queue... so I suppose Spark normally only has one central node pull the work? Not promising for using Spark + Kafka for scraping work...
///shared/spark-1.4.0-bin-hadoop2.6/bin/spark-submit --master local[*] --class org.tycho.fun.SparkKafka /shared/Scala/spark-kafka/target/scala-2.10/spark-kafka-assembly-0.1-SNAPSHOT.jar localhost:9092 test2
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    //wait, weren't these part of the producer properties already?
    val serSet = ssc.sparkContext.broadcast(SrlzrSet(producerKeySer, producerValSer))
    
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
        processRDD(rdd)
        .foreachPartition { part =>
          val p = producerPool.value.getDelegate().borrowObject()
          part.foreach {
//            case tweet: Tweet =>
//            val bytes = converter.value.apply(tweet)
            item =>
            val (k,v) = item
            println("Sending message [" + k + ": " + v + "] to topic [" + outputTopic + "]!")
            p.send(
              serSet.value.keySer.newInstance().serialize("",k),
              serSet.value.valSer.newInstance().serialize("",v),
              Option(outputTopic)
            )  //.get()
          }
          try {
            producerPool.value.returnObject(p)
          } catch {
            case msg: Exception => {
//              println("EXCEPTION: " + msg)
              println("Active producers: " + producerPool.value.getNumActive() + ", idle producers: " + producerPool.value.getNumIdle())
            }
          }
        }
      }

		ssc.start()
		ssc.awaitTermination()
    ssc.stop(stopSparkContext = true, stopGracefully = true)
//    FileUtils.deleteQuietly(sparkCheckpointRootDir)

	}
}
