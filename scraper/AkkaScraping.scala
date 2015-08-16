package org.tycho.scraping

//conflicted
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
import akka.stream._
import akka.stream.ActorMaterializer
import akka.stream.actor._
import akka.stream.actor.ActorPublisherMessage._
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
import kafka.serializer.{StringDecoder, StringEncoder}
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

object AkkaScraping {

//marshall/serialize with (akka-http-)spray-json
//case class SimpleResponse(url: String, status: String, encoding: String, body: String)

implicit class PartialFunctionList[A,B](val lst: List[PartialFunction[A,B]]) {
    def combine = lst reduceLeft (_ orElse _)
}

val elseCase: PartialFunction[Any, Unit] = {
  case msg => {
    println("ERROR, got a: " + msg.getClass.getSimpleName())
  }
}

val cancelCase: PartialFunction[Any, Unit] = {
  case msg: ActorPublisherMessage.Cancel => {
    println("The stream canceled the subscription.")
    // context.stop(self)
    // onError()
    // onComplete()
    // system.shutdown()
    Runtime.getRuntime.exit(0)
  }
}

val requestCase: PartialFunction[Any, Unit] = {
  case msg: ActorPublisherMessage.Request => {
    println("The stream wants more!")
    // deliverBuf()
    // http://doc.akka.io/docs/akka-stream-and-http-experimental/1.0-M2/scala/stream-integrations.html
  }
}

class MyActorPublisher[T]() extends ActorPublisher[T] {
  val forwardCase: PartialFunction[Any, Unit] = {
// Unfortunately due to type erasure I cannot properly make the case variable this way...
// So hardcoding to string right now cuz that's what my only instance needs, but still need a better solution. :( 
    case msg: T => {
      onNext(msg)
    }
  }
  // Potentially never gets to the else case anymore, needs testing!...
  val receive = List(forwardCase, cancelCase, requestCase, elseCase).combine
}

//Redefining for String here cuz otherwise it seems type erasure would ruin my pattern matching...
class MyStringActorPublisher() extends MyActorPublisher[String] {
  override val forwardCase: PartialFunction[Any, Unit] = {
    case msg: String => {
      onNext(msg)
    }
  }
}

//silly shell class acting as the start of the stream -- the domain-specific stuff before this (url grabbing and throttling) I wanna do in regular actors, cuz not sure how to do those using streams.
class StreamConnector() extends MyStringActorPublisher {}

abstract class MyCamelConsumer(fn: (CamelMessage) => Unit) extends akka.camel.Consumer {
  val camelCase: PartialFunction[Any, Unit] = {
    case msg: CamelMessage => {
      implicit val camelContext = CamelExtension(context.system).context
      println("Camel message: " + msg.bodyAs[String])
      fn(msg)
    }
  }
  val receive = List(camelCase, elseCase).combine
}

//consuming from queues through Camel
class QueueConsumer(dest: ActorRef) extends MyCamelConsumer((msg: CamelMessage) => { implicit camelContext: org.apache.camel.CamelContext =>
  dest ! msg.bodyAs[String]
}) {
  def endpointUri = "rabbitmq://localhost:5672/urls?queue=urls&routingKey=urls&autoDelete=false&username=test&password=test"
}

//Monitor RabbitMQ queue.created events through Camel
class CreationMonitor(dest: ActorRef) extends MyCamelConsumer((msg: CamelMessage) => { implicit camelContext: org.apache.camel.CamelContext =>
//        dest ! msg.bodyAs[String]
      // I actually need to pass it the queue and throttling delay needed to create the appropriate QueueConsumer...
//        dest ! ...
}) {
  def endpointUri = "rabbitmq://localhost:5672/queue.created?queue=queue.created&routingKey=queue.created&autoDelete=false&username=test&password=test"
}

abstract class MyActorSubscriber extends ActorSubscriber {
	import ActorSubscriberMessage._
	val MaxQueueSize = 10
	var queue = Map.empty[Int, ActorRef]
	override val requestStrategy = new MaxInFlightRequestStrategy(max = MaxQueueSize) {
		override def inFlightInternally: Int = queue.size
	}
}

//Camel actor sink
class ActorSink extends MyActorSubscriber with akka.camel.Producer {
	// http://camel.apache.org/kafka.html
	def endpointUri = "kafka://server:port?zookeeperHost=localhost&zookeeperPort=2181&topic=dumps"  //&groupId=&partitioner=&clientId=
}

class rawKafkaSink extends MyActorSubscriber {

  val producer = {
    val props = map2Props(Map(
      "bootstrap.servers" -> "localhost:9092",
      "client.id" -> "DemoProducer",
  //  "key.serializer" -> classOf[IntegerSerializer].getName(),
      "key.serializer" -> classOf[StringSerializer].getName(),
      "value.serializer" -> classOf[StringSerializer].getName()
     ))
    new KafkaProducer[String, String](props)
  }
  val topic = "dumps"
  
	def receive = {
		case akka.stream.actor.ActorSubscriberMessage.OnNext(msg) => {
      
      //      val body = msg.toString()
      val (url: String, body: String) = msg
      val key = url

//      println("Sending message [" + body + "] to topic [" + topic + "]!")
      println("Sending to topic [" + topic + "]!")
			producer.send(new ProducerRecord[String, String](topic, key, body)).get()
			// async callback: https://github.com/apache/kafka/blob/43b92f8b1ce8140c432edf11b0c842f5fbe04120/examples/src/main/java/kafka/examples/Producer.java
			// producer.send(new ProducerRecord[Integer, String](topic, messageNo, messageStr), new DemoCallBack(startTime, messageNo, messageStr))
//			println("Sent!")
		}
    case msg: ActorSubscriberMessage.OnError => {
      println("Received an OnError: " + msg)
      Runtime.getRuntime.exit(0)
    }
		case msg => {
			println("ERROR, got a: " + msg.getClass)
		}
	}
}

//object AkkaScraping {
	implicit val system = ActorSystem()
	implicit val materializer = ActorMaterializer()
	implicit val ec = system.dispatcher

	val throttlingRate = FiniteDuration(1000, MILLISECONDS)
	val timeOut = FiniteDuration(10, SECONDS)

  //pick a way of throttling / rate limiting; I've got the tick-based way, limitGlobal + Limiter.scala, and TimerBasedThrottler (http://doc.akka.io/docs/akka/snapshot/contrib/throttle.html).
  //my needs are low, so just pick whichever I can use for multiple domains by giving each input actor one of these.
  //these delay getting messages to another actor though...
  //which doesn't work with push-based data flows from queues since they don't require pushing.
  //Instead buffer the fetching while preventing greed with small mailboxes?

	//throttling the tick-based way
	def throttle[T](rate: FiniteDuration): Flow[T, T, Unit] = {
		Flow() { implicit b =>
			import akka.stream.scaladsl.FlowGraph.Implicits._
			println("throttling!")
			val zip = b.add(Zip[T, Unit.type]())
			Source(rate, rate, Unit) ~> zip.in1
			(zip.in0, zip.out)
		}.map(_._1)
	}
  
	/*
	//throttling the sophisticated way
	def limitGlobal[T](limiter: ActorRef, maxAllowedWait: FiniteDuration): Flow[T, T, Unit] = {
		import akka.pattern.ask
		import akka.util.Timeout
		var parallelism = 4
		Flow[T].mapAsync(parallelism)((element: T) => {
			import system.dispatcher
			implicit val triggerTimeout = Timeout(maxAllowedWait)
			val limiterTriggerFuture = limiter ? Limiter.WantToPass
			println("limiting")
			limiterTriggerFuture.map((_) => element)
		})
	}

	//def writeContents: Sink[HttpResponse, Unit] = //???

	*/

	def tryCatch[T](f: ()=>T): T = {
		try {
			f()
		} catch {
			case msg: Exception => {
				println("EXCEPTION: " + msg)
				system.shutdown()
				Runtime.getRuntime.exit(0)
				f()
			}
		}
	}

	def fetcher(): Flow[String, (String, HttpResponse), Unit] = {
		import akka.http.scaladsl.model._
		import akka.http.scaladsl.Http
		var parallelism = 4
		Flow[String]
		.via(throttle(throttlingRate))
		.mapAsync(parallelism)((url: String) => {
			tryCatch(()=>{
			val headers = List(
				//`Content-Type`(`application/json`)
			)
			val req = HttpRequest(uri = url).withHeaders(headers)
			println("time: " + System.nanoTime() / 1000000000.0)
			println(s"fetching $url")
      val fut = Http().singleRequest(req)
      fut.map((resp: HttpResponse) => (url, resp))
//      Future{ (url, url) }
			})
		})
	}

//	def decode(resp: HttpResponse): Future[String] = {
//    //...
//	}

	// def handleResp = (resp: HttpResponse) => {
	def decoder(): Flow[(String, HttpResponse), (String, String), Unit] = {
		Flow[(String, HttpResponse)]
    .mapAsync(4)((tpl: Tuple2[String, HttpResponse]) => {
  tryCatch(()=>{
    val (url: String, resp: HttpResponse) = tpl
//  println("{")
//  println("status: " + resp.status.toString())
  println(resp.status.toString() + " - " + url)
  val enc = resp.encoding.value match {
    case "identity" => "UTF-8"
    case s => s
  }
//  println("encoding: [" + enc + "]")
//  resp.headers.foreach(h =>
//    println("header: " + h.value())
//  )
//  println("type: " + resp.entity.contentType)
  //import scala.concurrent.ExecutionContext.Implicits.global
  val body = resp
    .entity.getDataBytes().asScala
    .map( _.decodeString(enc) )
    .runFold("") { case (s1, s2) => s1 + s2 }
//  println("}")
    body.map((s: String) => (url, s))
    })
  })
	}

	def reactiveRabbitSource(): Source[String, Unit] = {
		val amqpSett = rr.ConnectionSettings(
			addresses         = scala.collection.immutable.Seq(rr.Address(host = "localhost", port = 5672)),
			virtualHost       = "/",
			username          = "test",
			password          = "test",
			heartbeat         = None,
			timeout           = FiniteDuration(5, SECONDS),
			//automaticRecovery = false,
			recoveryInterval  = FiniteDuration(5, SECONDS)
		)
		//val rmq = rr.Connection()
		val rmq = rr.Connection(amqpSett)
		//val exchange = Sink(rmq.publish(exchange = "dumps", routingKey = "resp"))
		Source(rmq.consume(queue = "urls")).map(_.message.body.decodeString("UTF-8"))
	}

	val kafka = new ReactiveKafka()

	def kafkaSource(topic: String) = Source(kafka.consume(ConsumerProperties(
	  brokerList = "localhost:9092",
	  zooKeeperHost = "localhost:2181",
		topic = topic,
	  groupId = "groupName",
	  decoder = new StringDecoder()
	)))

	def kafkaSink(topic: String) = Sink(kafka.publish(ProducerProperties(
	  brokerList = "localhost:9092",
		topic = topic,
	  clientId = "groupName",
	  encoder = new StringEncoder()
	)))

	// Source(publisher).map(_.toUpperCase).to(Sink(subscriber)).run()

	def main(args: Array[String]): Unit = {
		//import system.dispatcher

		//throttling the sophisticated way
		//val limiterProps = Limiter.props(maxAvailableTokens = 10, tokenRefreshPeriod = new FiniteDuration(5, SECONDS), tokenRefreshAmount = 1)
		//val limiter = system.actorOf(limiterProps, name = "testLimiter")
		// limitGlobal(limiter, throttlingRate) ~>

		//graphs the graphy way
		/*
		FlowGraph.closed() { implicit b =>
			import FlowGraph.Implicits._
			urlSource ~> fetcher ~> printSink
		}.run()
		*/

    // testy way
    // Source(List("http://akka.io/", "http://baidu.com/"))

		// Reactive Rabbit way
		// reactiveRabbitSource()

		// Camel way
//		Source.actorPublisher[String](Props(classOf[QueueConsumer]))
    
    // ActorPublisher connecting my regular domain-specific actors to the stream
    val connectorSource = Source.actorPublisher[String](Props(classOf[StreamConnector]))
    //now initiate the stream...
    val connectorRef = Flow[String]
		.via(fetcher)
		// .runForeach(resp => decode(resp))
		.via(decoder)
		// .runForeach(println)
		// .runWith(Sink.foreach(println))
		// .to(Sink.actorSubscriber[String](Props(classOf[ActorSink])))
		.to(Sink.actorSubscriber[(String, String)](Props(classOf[rawKafkaSink])))
		// .to(kafkaSink("dumps"))
//		.run()
     .runWith(connectorSource)
    
    
    println("Ran!")

    //TimerBasedThrottler
    // for each domain-specific queue...
    val throttler = system.actorOf(Props(classOf[TimerBasedThrottler], 2 msgsPer 1.second))
    throttler ! SetTarget(Some(connectorRef))
    val camelActor = system.actorOf(Props(classOf[QueueConsumer], throttler))
    //throttler ! "msg to send to that actor through this one"

		// .onComplete({
		// 	case _ =>
		// 		system.shutdown()
		// 		Runtime.getRuntime.exit(0)
		// })

	}
}
