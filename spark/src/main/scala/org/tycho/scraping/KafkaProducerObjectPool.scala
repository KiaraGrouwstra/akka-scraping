package org.tycho.scraping
import org.tycho.scraping._
import scala.collection.JavaConversions._
import org.apache.commons.pool2.impl.{GenericObjectPool, GenericObjectPoolConfig}
import org.apache.commons.pool2.ObjectPool

//@RequiredArgsConstructor
class KafkaProducerObjectPool(brokerList: String, topic: String, producerProperties: Map[String, String]) extends LazySerializableObjectPool[KafkaProducerApp] {
  protected override def createDelegate(): ObjectPool[KafkaProducerApp] = {
    val producerFactory = new BaseKafkaProducerAppFactory(brokerList, defaultTopic = Option(topic))
    val pooledObjectFactory = new PooledKafkaProducerAppFactory(producerFactory)
    val poolConfig = new GenericObjectPoolConfig()
      val maxNumProducers = 10
      poolConfig.setMaxTotal(maxNumProducers)
      poolConfig.setMaxIdle(maxNumProducers)
    new GenericObjectPool[KafkaProducerApp](pooledObjectFactory, poolConfig)
  }
}
