package org.tycho.scraping
import org.tycho.scraping._
import org.apache.spark.rdd._
import org.apache.spark.streaming.StreamingContext.toPairDStreamFunctions

class FlightsDumps extends SparkKafkaFlow("dumps") {
  
//RDD: map, mapValues, filter, flatMap, pipe; stream-specific: aggregations by window
//  override def processRDD[T](rdd: RDD[T]): RDD[T] = {
////    println("Processing RDD from flights!")
//    rdd
//  }
  
//Iterator: map, filter, flatMap
//  override def processPart[T](it: Iterator[T]): Iterator[T] = {
////    println("Processing iterating partition from flights!")
//    it
//  }
  
//  override def processKV(tpl: Tuple2[String, String]): Tuple2[String, String] = {
//    tpl match {
//      case (k: String, v: String) => {
////        println("Processing tuple from flights!")
//        (k, v)
//      }
//      case (k, v: String) => {
////        println("Processing keyless tuple from flights!")
//        ("null", v)
//      }
//      case (k, v) => {
////        println("unknown tuple from flights!")
//        (k.toString(), v.toString())
//      }
//    }
//  }

}
