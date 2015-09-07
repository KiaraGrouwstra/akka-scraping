package org.tycho.scraping
import org.tycho.scraping._
import org.tycho.scraping.AkkaScraping._
import org.scalatest._
//enable to increase info
//import org.scalatest.Assertions._
import akka.actor._
import akka.pattern.ask
import scala.concurrent.duration._
import akka.util.Timeout
//import akka.testkit.TestActorRef  //recommended to test with messages instead
//import akka.testkit.{ TestActorRef, TestActors, TestKit, ImplicitSender }
import akka.testkit._
import scala.util.{Success, Failure}
import scala.reflect.ClassTag
//import scala.reflect.api.TypeTags
import scala.reflect.runtime.universe._
import scala.concurrent._
import scala.concurrent.duration._
import akka.testkit.TestProbe
import io.scalac.{amqp => rr}

//scala
import scala.util.{Success, Failure}
import scala.concurrent._
import scala.concurrent.duration._
import scala.reflect.ClassTag

//akka
import akka._
import akka.actor._
import akka.camel._
import akka.japi._
import akka.japi.function._
import akka.pattern._
import akka.util._
import akka.http._
import akka.http.scaladsl._
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers._
import akka.stream._
import akka.stream.ActorMaterializer
import akka.stream.actor._
import akka.stream.actor.ActorPublisherMessage._
import akka.stream.actor.ActorSubscriberMessage._
import akka.stream.io._
import akka.stream.stage._
import akka.stream.scaladsl._

//redis
import redis._
import redis.commands._
import redis.api.lists._
import redis.clients.util._
import redis.clients.jedis._
//import scala.concurrent.ExecutionContext.Implicits.global
import redis.actors._
import redis.api.pubsub._

//reactive-kafka
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
//import kafka.serializer.{StringDecoder, StringEncoder}
import com.softwaremill.react.kafka.{ReactiveKafka, ProducerProperties, ConsumerProperties}
//raw kafka
// import java.util.Arrays
// import java.util.List
import java.util.Properties
// import kafka.message._
// import kafka.producer._
// import kafka.producer.{Producer, ProducerData}
// import kafka.clients.producer._
import org.apache.kafka.clients.producer._
import org.apache.kafka.common.serialization._

//json:
//spray-json
//import spray.json._
//import DefaultJsonProtocol._
//lift-json
//import net.liftweb.json.DefaultFormats
//import net.liftweb.json._
//genson
//import com.owlike.genson.defaultGenson._

//shapeless
//import shapeless._, poly._, syntax.std.tuple._
//import shapeless._
//import poly._
//import syntax.std.tuple._

//misc
import java.net.InetSocketAddress
import org.apache.camel.Exchange
//import myUtils._
//import org.tycho.scraping._
import org.tycho.scraping.myUtils._
import akka.contrib.throttle._
import akka.contrib.throttle.Throttler._
//import akka.util.duration._
//import akka.pattern.throttle.Throttler._
//import java.util.concurrent.TimeUnit._
import scala.concurrent.duration.{ Duration, FiniteDuration }
import scala.collection.JavaConversions._
import scala.reflect.runtime.universe._

case class MsgA()
case class MsgB()
case class MsgC()

class ComposableActor extends MyActor {
  val caseA = (msg: MsgA) => {
    sender() ! "A"
  }
  val caseB = (msg: MsgB) => {
    sender() ! "B"
  }
  override val cases = List(caseFn(caseA), caseFn(caseB)) ++ super.cases
//  override val cases = List(caseA, caseB).map(caseFn(_)) ++ super.cases
// ^ somehow still gives "TypeNotHandledException: ERROR, ComposableActor got MsgA" and "MsgB cannot be cast to MsgA"...
}

class ReceiverSpec extends ActorSpec {
  
  val probe = TestProbe()
//  val cml = CamelMessage("foo", Map(
//    "rabbitmq.ROUTING_KEY" -> "lol",
//    "rabbitmq.EXCHANGE_NAME" -> "urls",
//    "rabbitmq.CONTENT_TYPE" -> "json",
//    "rabbitmq.PRIORITY" -> "100",
//    "rabbitmq.CORRELATIONID" -> "123",
//    "rabbitmq.MESSAGE_ID" -> "456",
////    "rabbitmq.DELIVERY_MODE" -> true,
//    "rabbitmq.USERID" -> "test",
//    "rabbitmq.CLUSTERID" -> "clusterId",
//    "rabbitmq.REPLY_TO" -> "replyTo",
//    "rabbitmq.CONTENT_ENCODING" -> "contentEncoding",
//    "rabbitmq.TYPE" -> "type",
//    "rabbitmq.EXPIRATION" -> "expiration",
////    "rabbitmq.TIMESTAMP" -> "timestamp",
//    "rabbitmq.APP_ID" -> "appId"
//  ))
  val next = OnNext("foo")
  val actSub = List(next, OnComplete)  //, OnError
  
  val actors = Map(
//    TestActorRef(new ComposableActor) -> List(MsgA(), MsgB()),
//    TestActorRef(new MyStringActorPublisher) -> List("foo", Request, Cancel),
    TestActorRef(new StreamConnector) -> List(ActorPublisherMessage.Request(1), ActorPublisherMessage.Cancel),  //"foo", 
    TestActorRef(new SourceCreator(probe.ref)) -> List(ConsumerInfo("foo", 2 msgsPer 1.second)),
//    TestActorRef(new QueueConsumer("foo", probe.ref)) -> List(cml),
//    TestActorRef(new CamelDeletionMonitor) -> List(cml),
//    TestActorRef(new CamelCreationMonitor(probe.ref)) -> List(cml),
    TestActorRef(new RRDeletionMonitor) -> actSub,
    TestActorRef(new RRCreationMonitor(probe.ref)) -> actSub,
    TestActorRef(new ForwarderSink(probe.ref)) -> actSub,
    TestActorRef(new RawKafkaSink) -> List(OnError(new Exception("lol")), next, OnComplete)
  )
  actors.foreach{ case (k, v) =>
    k.checkHandled(v)
  }
  
//  // Given a urls-url-urls route in RabbitMQ...
//  
//  // ugh, this keeps sending to new randomly-named queues...
//  "RmqCamelSink" must {
//    "send to rmq" in {
//      val actor = TestActorRef(new RmqCamelSink("urls")("lol"))
////      actor receive cml
//      actor ! cml
//    }
//  }
//  
//  // ugh, this keeps fetching empty messages... where is it getting these anyway?
//  "RmqConsumer" must {
//    "get from rmq" in {
//      TestActorRef(new RmqConsumer(probe.ref, "urls"))  //()
//      // shouldn't even need to send it anything, right?
//      expectMsg(cml)
//    }
//  }
  
}
