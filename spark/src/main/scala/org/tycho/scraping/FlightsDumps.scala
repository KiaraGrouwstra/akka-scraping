package org.tycho.scraping
import org.tycho.scraping._
import org.apache.spark.rdd._
import org.apache.spark.streaming.StreamingContext.toPairDStreamFunctions

class FlightsDumps extends SparkKafkaFlow("dumps") {
  
//RDD: keys/values, map/mapValues, flatMap/flatMapValues, filter, pipe; stream-specific: aggregations by window
  override def processRDD[T](rdd: RDD[T]): RDD[T] = {
    //Making a DataFrame is super cool, but Kafka kinda takes k/v pairs, so think of a different destination for this? Batch-only? 
      rdd
      .filter((k,v) => k contains "kayak.com")
//      .mapValues((k,v) => """{"url":"$k","result":"$v"}""")
//      .values
  }
  
}
