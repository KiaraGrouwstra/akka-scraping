package org.tycho.scraping
// Hm, Is there a way to convert classes to actors, functions to case classes/objects + corresponding Receive partials?
// cases classes: https://github.com/julianpeeters/case-class-generator
// case objects: just strings named after the no-param methods?
// other non-case classes (such as strings) in receivers won't be created this way (all would be wrapped in method case classes), but that's fine I guess.

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
import akka.io.IO

//import akka.http._
//import akka.http.scaladsl._
//import akka.http.scaladsl.model._
//import akka.http.scaladsl.model.headers._

import _root_.spray.can.Http
import _root_.spray.http._
import _root_.spray.httpx.RequestBuilding._
import _root_.spray.http.HttpMethods._
import _root_.spray.http.HttpHeaders._

import akka.stream._
import akka.stream.ActorMaterializer
import akka.stream.actor._
import akka.stream.actor.ActorPublisherMessage._
import akka.stream.actor.ActorSubscriberMessage._
import akka.stream.io._
import akka.stream.stage._
import akka.stream.scaladsl._
//import akka.typed._
//import akka.typed.ScalaDSL._
//import akka.typed.AskPattern._

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
import _root_.shapeless._
import _root_.shapeless.poly._
import _root_.shapeless.syntax.std.tuple._
import _root_.shapeless.ops.function._

//misc
import java.net.InetSocketAddress
import org.apache.camel.Exchange
//import myUtils._
//import org.tycho.scraping._
import org.tycho.scraping.myUtils._
//import org.tycho.scraping.TychoMacros._
import org.tycho.misc.TychoMacros._
import akka.contrib.throttle._
import akka.contrib.throttle.Throttler._
//import akka.util.duration._
//import akka.pattern.throttle.Throttler._
//import java.util.concurrent.TimeUnit._
import scala.concurrent.duration.{ Duration, FiniteDuration }
import scala.collection.JavaConversions._
import scala.reflect.runtime.universe._
import scala.concurrent.ExecutionContext.Implicits.global
import language.experimental.macros


object AkkaScraping {
  var parallelism = 4

  import scala.reflect.runtime.{universe => ru}
  def getTypeTag[T: ru.TypeTag](obj: T) = ru.typeTag[T]
  def getParTTag[T: ru.TypeTag, S <: Any](fn: T => S) = ru.typeTag[T]
  def getRetTTag[T: ru.TypeTag, S <: Any](fn: S => T) = ru.typeTag[T]
  //def getClssTag[T: ClassTag](obj: T) = classTag[T]
  //def getParCTag[T: ClassTag, S <: Any](fn: T => S) = classTag[T]
  //def getRetCTag[T: ClassTag, S <: Any](fn: S => T) = classTag[T]
  //import akka.stream.scaladsl._
  def dynamicFlow[T](tt: ru.TypeTag[T]) = Flow[T]
  //def isFuture[T](tt: ru.TypeTag[Future[T]]) = "yes"
  def fut[T](v: T): Future[T] = Future{v}
//  def wrapFut[F, I <: HList, O](f: F)
//    (implicit ftp: FnToProduct.Aux[F, I => O], ffp: FnFromProduct[I => Option[O]])
//    : ffp.Out = ffp(i => ftp(f)(i) andThen fut)

  class TypeNotHandledException(msg: String) extends RuntimeException(msg)

  //marshall/serialize with (akka-http-)spray-json
  //case class SimpleResponse(url: String, status: String, encoding: String, body: String)

  implicit class ReceiveList(val lst: List[Actor.Receive]) {
    def combine = lst reduceLeft (_ orElse _)
  }

  implicit class ActorContextImpl(val context: ActorContext) {
    def getOrMake(name: String, props: Props) = {
      context.child(name) match {
        case Some(ref) =>
          ref
        case None =>
          context.actorOf(props, name)
      }
    }
  }

  def caseFn[T](fn: T => Unit)(implicit ct: ClassTag[T]): Actor.Receive = {
    case ct(msg: T) => {
      fn(msg)
    }
  }

  trait MyActor extends Actor {
  //  def cases: List[Actor.Receive] = List(elseCase(this.getClass.getSimpleName())).map(caseFn(_))
    def cases: List[Actor.Receive] = List(elseCase(this.getClass)).map(caseFn(_))
    // extend as follows:
  //  override def cases = List(fooCase).map(caseFn(_)) ++ super.cases
  //  val receive = cases.map(caseFn).combine
    def receive = cases.combine

    override def preStart() {
//      println("Created a " + this.getClass.getSimpleName() + ": " + self.path)
    }
  }

  //def elseCase(actorClass: String) = (msg: Any) => {
  def elseCase[T <: Actor](actor: Class[T]) = (msg: Any) => {
  //  throw new TypeNotHandledException(s"ERROR, $actorClass got " + msg.getClass.getSimpleName())
    throw new TypeNotHandledException(s"ERROR, " + actor.getSimpleName() + " got " + msg.getClass.getSimpleName())
  }

  //class MyActorPublisher[T:ClassTag]()(implicit ct: ClassTag[T]) extends ActorPublisher[T] with MyActor {
  //class MyActorPublisher[T:ClassTag]() extends ActorPublisher[T] with MyActor {
  class MyActorPublisher[T]() extends ActorPublisher[T] with MyActor {
    val MaxBufferSize = 10
    var buf = Vector.empty[T]

//    val fullCase: Receive = {
//      case msg: T if buf.size == MaxBufferSize => {
//        //        sender() ! JobDenied
//        // fuck, what can I do here to prevent the message from going lost? buffering just delays the overflow...
////        sender() ! msg
//        // wait, who says the sender knows how to bounce this message back here?
//        Thread.sleep(100)
//        self ! msg
//        // TODO: find a fix, this approach is horrible.
//      }
//    }

//    @tailrec
    final def deliverBuf(): Unit = {
      if (totalDemand > 0) {
        if (totalDemand <= Int.MaxValue) {
          val (use, keep) = buf.splitAt(totalDemand.toInt)
          buf = keep
          use foreach onNext
        } else {
          val (use, keep) = buf.splitAt(Int.MaxValue)
          buf = keep
          use foreach onNext
          deliverBuf()
        }
      }
    }

//    val forwardCase = (msg: T) => {
//  //  val forwardCase = ct(msg: T) => {
//      // fails due to erasure, so making hardcoded String child now but TODO: find fix
////      onNext(msg)
//      if (buf.isEmpty && totalDemand > 0)
//        onNext(msg)
//      else {
//        buf :+= msg
//        deliverBuf()
//      }
//    }

    val cancelCase = (msg: ActorPublisherMessage.Cancel) => {
      println("The stream canceled the subscription.")
      //  Runtime.getRuntime.exit(0)
      context.stop(self)
    }

    val requestCase = (msg: ActorPublisherMessage.Request) => {
      println("The stream wants " + msg.n + " more!")
      // http://doc.akka.io/docs/akka-stream-and-http-experimental/1.0-M2/scala/stream-integrations.html
      deliverBuf()
    }

    // allow calling stream error/complete from outside by sending the respective messages here
    def errorCase = (msg: ActorSubscriberMessage.OnError) => {
      onError(msg.cause)
    }

    // TODO: keep getting "type OnComplete is not a member of object akka.stream.actor.ActorSubscriberMessage"... why?
//    def onCompleteCase = (msg: ActorSubscriberMessage.OnComplete) => {
    def onCompleteCase = (msg: ActorSubscriberMessage) => {
      onComplete()
    }

//    override def cases = List(fullCase, caseFn(forwardCase), caseFn(cancelCase), caseFn(requestCase), caseFn(errorCase), caseFn(onCompleteCase)) ++ super.cases
  }

  //Redefining for String here cuz otherwise it seems type erasure would ruin my pattern matching...
  class MyStringActorPublisher() extends MyActorPublisher[String] {
//    override
    val fullCase: Receive = {
      case msg: String if buf.size == MaxBufferSize => {
        //        sender() ! JobDenied
        // fuck, what can I do here to prevent the message from going lost? buffering just delays the overflow...
        //        sender() ! msg
        // wait, who says the sender knows how to bounce this message back here?
        Thread.sleep(100)
        self ! msg
        // TODO: find a fix, this approach is horrible.
      }
    }

//    override
    val forwardCase = (msg: String) => {
//      onNext(msg)
      if (buf.isEmpty && totalDemand > 0)
        onNext(msg)
      else {
        buf :+= msg
        deliverBuf()
      }
    }
  //  override def cases = List(forwardCase, cancelCase, requestCase).map(caseFn(_)) ++ super.cases
    override def cases = List(fullCase, caseFn(forwardCase), caseFn(cancelCase), caseFn(requestCase), caseFn(errorCase), caseFn(onCompleteCase)) ++ super.cases
  }

  //silly shell class acting as the start of the stream -- the domain-specific stuff before this (url grabbing and throttling) I wanna do in regular actors, cuz not sure how to do those using streams.
  class StreamConnector() extends MyStringActorPublisher {}

  //I can't get Camel to work...
  //def rmqConsUri(queue: String)(exchange: String = queue, routing_key: String = queue) = {
  //  "rabbitmq://localhost:5672/" + exchange + "?queue=" + queue + "&routingKey=" + routing_key + "&autoDelete=false&username=test&password=test"
  //}
  //
  //def rmqProdUri(exchange: String)(key: String = exchange) = {
  //  "rabbitmq://localhost:5672/" + exchange + "?routingKey=" + key + "&exchangeType=topic&autoDelete=false&username=test&password=test"
  //}
  //
  //abstract class CamelConsumer(fn: (CamelMessage) => Unit) extends akka.camel.Consumer with MyActor {
  //  val camelCase = (msg: CamelMessage) => {
  //    implicit val camelContext = CamelExtension(context.system).context
  //    println("Camel message: " + msg.bodyAs[String])
  //    fn(msg)
  //  }
  //  override def cases = List(camelCase).map(caseFn(_)) ++ super.cases
  ////  override def cases = List(caseFn(camelCase)) ++ super.cases
  //}
  //
  ////consuming from queues through Camel
  //abstract class QueueConsumer(dest: ActorRef) extends CamelConsumer((msg: CamelMessage) => { implicit camelContext: org.apache.camel.CamelContext =>
  //  dest ! msg.bodyAs[String]
  //}) {}
  //
  //class RmqConsumer(dest: ActorRef, queue: String)
  ////(exchange: String = queue, routing_key: String = queue)
  //extends QueueConsumer(dest) {
  //  def endpointUri = rmqConsUri(queue)()  //(exchange, routing_key)
  //}
  //
  ////Monitor RabbitMQ queue.deleted events through Camel
  //class CamelDeletionMonitor() extends CamelConsumer((msg: CamelMessage) => { implicit camelContext: org.apache.camel.CamelContext =>
  //  val queue = msg.getHeaderAs("name", classOf[String], camelContext)
  //  val queueActor = system.actorSelection("/user/consumer_" + queue)
  //  queueActor ! PoisonPill
  //  val throtActor = system.actorSelection("/user/throttler_" + queue)
  //  throtActor ! PoisonPill
  //}) {
  //  def endpointUri = rmqConsUri("queue.deleted")()
  //}
  //
  //Monitor RabbitMQ queue.created events through Camel
  //class CamelCreationMonitor(sourceCreator: ActorRef) extends CamelConsumer((msg: CamelMessage) => { implicit camelContext: org.apache.camel.CamelContext =>
  //  println("msg: " + msg.toString)
  //  println("body: " + msg.bodyAs[String])
  //  msg.getHeaders.foreach{ case (k, v) =>
  //    println(k + ": " + v)
  //  }
  //  val queue = msg.getHeaderAs("name", classOf[String], camelContext)
  //  val rate = 2 msgsPer 1.second  //TODO: make this variable
  //  sourceCreator ! ConsumerInfo(queue, rate)
  //}) {
  //  def endpointUri = rmqConsUri("queue.created")()
  //}

  //Monitor RabbitMQ queue.deleted events through ReactiveRabbit
  class RRDeletionMonitor() extends MyActorSubscriber {
    override def onNextCase = (onNext: OnNext) => {
      val el = onNext.element
//      println("Deletion monitor got a " + el.getClass.getSimpleName() + ": " + el.toString())
      val queue = el.toString()
  //    println("Deletion monitor would like to delete " + queue)
      //TODO: how could I make any of this work with Reactive Streams stuff, like an Akka Stream based on that ReactiveRabbit org.reactivestreams.Publisher?
      //I can't PoisonPill them without an actor reference, but streams seem closed, and a Publisher for a Source doesn't give any. Putting it in a separate
      //ActorSystem to shutdown() that seems extreme? Further complicates things in terms of having to do cross-ActorSystem communication (akka remote?) too...
      //SO answer: I ended up just going back to the simple java driver and writing a small wrapper for it to abstract over the blocking nature of the consumer.
    }
  }

  //Monitor RabbitMQ queue.created events through ReactiveRabbit
  class RRCreationMonitor(sourceCreator: ActorRef) extends MyActorSubscriber {
    override def onNextCase = (onNext: OnNext) => {
      val el = onNext.element
//      println("Creation monitor got a " + el.getClass.getSimpleName() + ": " + el.toString())
      val queue = el.toString()
  //    println("Creation monitor would like to create " + queue)
      val rate = 2 msgsPer 1.second  //TODO: make this variable
      sourceCreator ! ConsumerInfo(queue, rate)
    }
  }

  case class ConsumerInfo(queue: String, rate: akka.contrib.throttle.Throttler.Rate)

  //actually create the needed actors for a domain
  class SourceCreator(connectorRef: ActorRef) extends MyActor {
    val infoCase = (ci: ConsumerInfo) => {
//      println("Creator gonna create " + ci.queue + " with " + ci.rate)
      //TODO: fix _ to a /
      val throttler = context.getOrMake("throttler_" + ci.queue, Props(classOf[TimerBasedThrottler], ci.rate))
      throttler ! SetTarget(Some(connectorRef))
  //    val camelActor = system.actorOf(Props(classOf[RmqConsumer], throttler, ci.queue), "consumer_" + ci.queue)
      val rrSource =
      //how to do fixed addresses for deletion for queue? I don't think I'll still be able to kill this this way, right?...
      reactiveRabbitSource(ci.queue)
      .map(_.message.body.decodeString("UTF-8"))
      .to(Sink.actorSubscriber[String](Props(classOf[ForwarderSink], throttler)))
      .run()
      //TODO: how can I do manual acking here to ensure unhandled messages will return to the todo queue?
    }
    override def cases = List(infoCase).map(caseFn(_)) ++ super.cases
  }

  //initially split off from MyActorSubscriber from when CamelSink still did stream integration, so it could get the needed functionality with the `receive` override
  trait ActorSubscriberStrategy extends ActorSubscriber {
    // MaxInFlightRequestStrategy is useful if messages are queued internally or delegated to other actors.
    // WatermarkRequestStrategy is a good strategy if the actor performs work itself.
    val MaxQueueSize = 10
    var queue = Map.empty[Int, ActorRef]
    override val requestStrategy = new MaxInFlightRequestStrategy(max = MaxQueueSize) {
      override def inFlightInternally: Int = queue.size
    }
  }

  trait MyActorSubscriber extends ActorSubscriberStrategy with MyActor {

    def onNextCase: OnNext => Unit = (onNext: OnNext) => {
      throw new Exception("onNext undefined for " + this.getClass.getSimpleName())
    }

    def errorCase = (onError: ActorSubscriberMessage.OnError) => {
      println(this.getClass.getSimpleName() + " received an OnError: " + onError.cause)
  //    Runtime.getRuntime.exit(0)
    }
  //  def errorCase = (msg: OnError) => elseCase(this.getClass.getSimpleName()).apply(msg)
  //  def errorCase = (msg: OnError) => elseCase(this.getClass).apply(msg)

  //  def onCompleteCase = (msg: OnComplete) => {
    def onCompleteCase = (msg: ActorSubscriberMessage) => {
  // TODO: keep getting "type OnComplete is not a member of object akka.stream.actor.ActorSubscriberMessage"... why?
      println(this.getClass.getSimpleName() + " got an OnComplete from the queue! ditch actor?")
    }

  //  override def cases = List(onNextCase, errorCase, onCompleteCase).map(caseFn(_)) ++ super.cases
    override def cases = List(caseFn(onNextCase), caseFn(errorCase), caseFn(onCompleteCase)) ++ super.cases
  }

  //Forwards from stream to actor, improving on Sink.actorRef with back-pressure.
  //TODO: handle onCompleteMessage/Failure, deciding whether to terminate destination actors, though harder if chain of multiple?
  class ForwarderSink(actor: ActorRef) extends MyActorSubscriber {
    override def onNextCase = (onNext: OnNext) => {
  //  def onNextCase(onNext: OnNext): Unit = {
//      println("Forwarding " + onNext.element)
  //    actor forward onNext.element
      actor ! onNext.element
    }
  }

  ////Camel actor sink, ActorSubscriber stream integration split off for testing modularity
  //trait CamelSink extends Actor with akka.camel.Producer {
  ////  override def receive = super[Producer].receive
  //}
  //
  //def kafkaProdUri = {
  //  // http://camel.apache.org/kafka.html
  //  "kafka://localhost:9092?zookeeperHost=localhost&zookeeperPort=2181&topic=dumps"  //&groupId=&partitioner=&clientId=
  //}
  //
  //// does this work now?
  //class KafkaCamelSink extends CamelSink {
  //  def endpointUri = kafkaProdUri
  //}
  //
  //class RmqCamelSink(exch: String)(key: String = exch) extends CamelSink {
  //  def endpointUri = rmqProdUri(exch)(key)
  //}

  class RawKafkaSink extends MyActorSubscriber {

    val producer = {
      val props = map2Props(Map(
        "bootstrap.servers" -> "localhost:9092",
  //      "client.id" -> "DemoProducer",
        "key.serializer" -> classOf[StringSerializer].getName(),
        "value.serializer" -> classOf[StringSerializer].getName()
       ))
      new KafkaProducer[String, String](props)
    }
    val topic = "dumps"

    def tplCase(tpl: Tuple2[String, String]): Unit = {
  //  def tplCase = (tpl: Tuple2[String, String]): Unit => { //tpl =>
      //case (url: String, body: String) => {
        val (url: String, body: String) = tpl
        val key = url
        println("Storing [" + key + "] to Kafka topic [" + topic + "]!")
        producer.send(new ProducerRecord[String, String](topic, key, body)) //.get()
        // async callback: https://github.com/apache/kafka/blob/43b92f8b1ce8140c432edf11b0c842f5fbe04120/examples/src/main/java/kafka/examples/Producer.java
        // producer.send(new ProducerRecord[Integer, String](topic, messageNo, messageStr), new DemoCallBack(startTime, messageNo, messageStr))
      //}
    }

    override def cases = List(caseFn(tplCase)) ++ super.cases
  }

//object AkkaScraping {
	implicit val system = ActorSystem()
	implicit val materializer = ActorMaterializer()
//	implicit val ec = system.dispatcher

	val throttlingRate = FiniteDuration(1000, MILLISECONDS)
  val timeOut = FiniteDuration(10, SECONDS)
  implicit val sprayTimeout: Timeout = Timeout(15.seconds)

  //pick a way of throttling / rate limiting; I've got the tick-based way, limitGlobal + Limiter.scala, and TimerBasedThrottler (http://doc.akka.io/docs/akka/snapshot/contrib/throttle.html).
  //my needs are low, so just pick whichever I can use for multiple domains by giving each input actor one of these.
  //these delay getting messages to another actor though...
  //which doesn't work with push-based data flows from queues since they don't require pushing.
  //Instead buffer the fetching while preventing greed with small mailboxes?

	//throttling the tick-based way
	def throttle[T](rate: FiniteDuration): Flow[T, T, Unit] = {
		Flow() { implicit b =>
			import akka.stream.scaladsl.FlowGraph.Implicits._
//			println("throttling!")
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
//		import akka.http.scaladsl.model._
//		import akka.http.scaladsl.Http
		Flow[String]
		.via(throttle(throttlingRate))
		.mapAsync(parallelism)((url: String) => {
			tryCatch(()=>{
        println(s"fetching $url")
//  			println("time: " + System.nanoTime() / 1000000000.0)
        val headers = List(
          //`Content-Type`(`application/json`)
        )
        val req = HttpRequest(uri = url).withHeaders(headers)
//        val fut = Http().singleRequest(req)
//        val fut = (IO(_root_.spray.can.Http) ? req).mapTo[HttpResponse]
        val fut: Future[_root_.spray.http.HttpResponse] = (IO(_root_.spray.can.Http) ? req).mapTo[HttpResponse]
        fut.map((resp: HttpResponse) => (url, resp))
        //Future{ (url, url) }
			})
		})
	}

  def printResp(resp: HttpResponse, url: String) = {
    resp.headers.foreach(h =>
      println("header [" + h.lowercaseName + "]: " + h.value)
    )
//    println("type: " + resp.entity.contentType)
  }

	// def handleResp = (resp: HttpResponse) => {
	def decoder(): Flow[(String, HttpResponse), (String, String), Unit] = {
		Flow[(String, HttpResponse)]
    .mapAsync(4)((tpl: Tuple2[String, HttpResponse]) => {
      tryCatch(()=> {
        val (url: String, resp: HttpResponse) = tpl
        //  println("{")
        val enc = resp.encoding.value match {
          case "identity" => "UTF-8"
          case s => s
        }
        //  println("encoding: [" + enc + "]")
        println(resp.status.toString() + " - " + url)
        resp.status.intValue match {
          case x if 200 until 299 contains x => {
            //import scala.concurrent.ExecutionContext.Implicits.global
//            val body = resp
//              .entity.getDataBytes().asScala
//              .map( _.decodeString(enc) )
//              .runFold("") { case (s1, s2) => s1 + s2 }
            val body = resp.entity.asString
//            val body = resp.entity.data.asString
            //  println("}")
//            body.map((s: String) => (url, s))
            Future((url, body))
          }
          case x if 300 until 399 contains x => {
            //Nope, Spray's [`spray.can.host-connector.max-redirects`](http://spray.io/documentation/1.2.2/spray-can/http-client/) not merged into Akka Http
            //Akka ticket: https://github.com/akka/akka/issues/15990
            printResp(resp, url)
            throw new Exception("Unhandled redirect at " + url)
//            resp.getHeader("Location") match {
//              case Some(h: HttpHeader) =>
//                val location = h.value
//                // TODO: feed this location url back to fetcher, can't return this.
//                // ... or just ensure fetcher handles them
//                throw new Exception("Unhandled redirect to " + location)
//              case None =>
//                throw new Exception("No location!")
//            }
          }
          case _ => {
            printResp(resp, url)
            // drop it? cuz nothing to return.
            throw new Exception("BadResponse: " + resp.status.toString() + " - " + url)
          }
        }
      })
    })
	}

  // Wait, was this the one that actually worked?
  // I think reason I wanted to try Camel with RabbitMQ was that regular actors I can just create more of on the fly, unlike stream components (I think...)
  // Could this kind of thing be used as a regular actor as well?
	def reactiveRabbitSource(queue: String = "urls"): Source[rr.Delivery, Unit] = {
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
//    Source(rmq.consume(queue)).map(_.message.body.decodeString("UTF-8"))
    val pub: org.reactivestreams.Publisher[rr.Delivery] = rmq.consume(queue)
    Source(pub)
//      .map(_
      //https://github.com/ScalaConsultants/reactive-rabbit/blob/master/src/main/scala/io/scalac/amqp/Delivery.scala
      //Delivery: message: Message, deliveryTag: DeliveryTag, exchange: String, routingKey: String, redeliver: Boolean
//      .message
      //https://github.com/ScalaConsultants/reactive-rabbit/blob/master/src/main/scala/io/scalac/amqp/Message.scala
      //Message: body: ByteString, contentType: Option[MediaType], contentEncoding: Option[String], headers: Map[String, String], priority: Option[Int] 0-9, correlationId: Option[String], replyTo: Option[String], messageId: Option[String], timestamp: Option[DateTime], `type`: Option[String], userId: Option[String], appId: Option[String]
      //headers: name, vhost, durable, auto_delete, arguments, owner_pid
//      .body.decodeString("UTF-8")
//    )
	}

//	val kafka = new ReactiveKafka()
//
//	def kafkaSource(topic: String) = Source(kafka.consume(ConsumerProperties(
//	  brokerList = "localhost:9092",
//	  zooKeeperHost = "localhost:2181",
//		topic = topic,
//	  groupId = "groupName",
//	  decoder = new StringDecoder()
//	)))
//
  // I think this didn't work, since I switched to that RawKafkaSink...
//	def kafkaSink(topic: String) = Sink(kafka.publish(ProducerProperties(
//	  brokerList = "localhost:9092",
//		topic = topic,
//	  clientId = "groupName",
//	  encoder = new StringEncoder()
//	)))

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

    val kafkaSink = system.actorOf(Props(classOf[RawKafkaSink]))

    // testy way
    // Source(List("http://akka.io/", "http://baidu.com/"))

		// Reactive Rabbit way
//		 reactiveRabbitSource()

		// Camel way
//		Source.actorPublisher[String](Props(classOf[QueueConsumer]))
    
    // ActorPublisher connecting my regular domain-specific actors to the stream
    val connectorSource = Source.actorPublisher[String](Props(classOf[StreamConnector]))
    //now initiate the stream...
    val connectorRef = Flow[String]
		.via(fetcher)
//    .via(throttle(throttlingRate))
//    .via(flw(fetcher _))
		// .runForeach(resp => decode(resp))

		.via(decoder)

		// .runForeach(println)
		// .runWith(Sink.foreach(println))
		// .to(Sink.actorSubscriber[String](Props(classOf[KafkaCamelSink])))
		.to(Sink.actorSubscriber[(String, String)](Props(classOf[ForwarderSink], kafkaSink)))
		// .to(kafkaSink("dumps"))
//		.run()
     .runWith(connectorSource)
    
    
//    println("Ran!")
    
    val creator = system.actorOf(Props(classOf[SourceCreator], connectorRef))
//    val creationMonitor = system.actorOf(Props(classOf[CamelCreationMonitor], creator))
//    val deletionMonitor = system.actorOf(Props(classOf[CamelDeletionMonitor]))
//    val creationMonitor = system.actorOf(Props(classOf[RRCreationMonitor], creator))
//    val deletionMonitor = system.actorOf(Props(classOf[RRDeletionMonitor]))
    
    val creationMonitor = 
    reactiveRabbitSource("queue.created")
    .map(_.message.headers("name"))
    .to(Sink.actorSubscriber[String](Props(classOf[RRCreationMonitor], creator)))
    .run()

    val deletionMonitor = 
    reactiveRabbitSource("queue.deleted")
    .map(_.message.headers("name"))
    .to(Sink.actorSubscriber[String](Props(classOf[RRDeletionMonitor])))
    .run()

    //create the initial domain-specific actors (also through SourceCreator), plus one-per-class monitoring ones
    //poll http://localhost:15672/api/bindings with auth test:test; filter results by "source":"urls"; grab resulting `destination` or `routing_key`
    //or poll queues endpoint?
    //tell creator to actually make the appropriate actors based on this info.

    // copy from fetcher
    val url = "http://localhost:15672/api/bindings"
    val creds = BasicHttpCredentials("test", "test")
//    val authorization = headers.Authorization(creds)
    val authorization = Authorization(creds)
    val headers = List(authorization)
    val req = HttpRequest(uri = url, headers = headers)
//    val req = HttpRequest(uri = url)
//      .withHeaders(headers)
//      ~> addCredentials(creds)
//    val fut = Http().singleRequest(req)
    val fut = (IO(_root_.spray.can.Http) ? req).mapTo[HttpResponse]

//    println("checking RabbitMQ API")
//    val resp = fut.result(Duration(10, SECONDS))
    val resp = Await.result(fut, Duration(10, SECONDS))
    
    // copy from decoder
//    val enc = resp.encoding.value match {
//      case "identity" => "UTF-8"
//      case s => s
//    }
//    val joined = resp
//      .entity.getDataBytes().asScala
//      .map( _.decodeString(enc) )
//      .runFold("") { case (s1, s2) => s1 + s2 }
//    val body = Await.result(joined, Duration(10, SECONDS))
    val body = resp.entity.asString
//    println(body)
    
    //val json = body.parseJson
    //implicit val formats = DefaultFormats
    //val json = parse(body)
    import com.owlike.genson.defaultGenson._
//    println("gonna parse")
    val json = fromJson[List[Map[String,Any]]](body)
//    println("gonna filter")
//    println(json)
    val url_bindings = json.filter(_("source") == "urls")  //filter results by "source":"urls"
//    println(url_bindings)
//    println("gonna map")
    val queue_names = url_bindings.map(_("destination").asInstanceOf[String])  //grab resulting `destination` or `routing_key`
//    println(queue_names)
//    println("gonna iterate over all " + queue_names.size + " items!")
    queue_names.foreach(q => {
//      println("queue: " + q)
      // copy from creation monitor
      val rate = 2 msgsPer 1.second  //TODO: make this variable
      creator ! ConsumerInfo(q, rate)
    })
    
	}
}
