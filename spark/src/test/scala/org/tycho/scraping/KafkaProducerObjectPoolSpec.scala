package org.tycho.scraping
import org.tycho.scraping._
import org.scalatest._
import org.apache.kafka.common.serialization._
//enable to increase info
//import org.scalatest.Assertions._

class KafkaProducerObjectPoolSpec extends UnitSpec {
  
  val kafkaBrokers = "127.0.0.1:9092"
  val inputTopic = "dumps"
  val outputTopic = "out"
  val producerProperties = Map(
    "bootstrap.servers" -> "localhost:9092",
    "client.id" -> "DemoProducer",
//  "key.serializer" -> classOf[IntegerSerializer].getName(),
    "key.serializer" -> classOf[StringSerializer].getName(),
    "value.serializer" -> classOf[StringSerializer].getName()
   )
  
  def fixt = new {
    val pool = new KafkaProducerObjectPool(kafkaBrokers, outputTopic, producerProperties)
  }

  it should "make a kafka pool" in {
    val f = fixt
    val res: KafkaProducerObjectPool = f.pool
  }

  it should "have a delegate" in {
    val f = fixt
    val res: org.apache.commons.pool2.ObjectPool[KafkaProducerApp] = f.pool.getDelegate()
  }

  it should "have a borrowable object" in {
    val f = fixt
//    val res: KafkaProducerApp = f.pool.borrowObject()
    val res: KafkaProducerApp = f.pool.getDelegate().borrowObject()
  }

}