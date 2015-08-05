package org.tycho.scraping

import scala.util.{Success, Failure}
import scala.concurrent._
import scala.concurrent.duration._
import scala.reflect.ClassTag
import org.apache.camel.Exchange
import io.scalac.{amqp => rr}
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
import redis._
import redis.commands._
import redis.api.lists._
import redis.clients.util._
import redis.clients.jedis._
//import scala.concurrent.ExecutionContext.Implicits.global
import redis.actors._
import redis.api.pubsub._
import java.net.InetSocketAddress

/*
//serializing classes to send over the wire
case class SimpleResponse(url: String, status: String, encoding: String, body: String)

object SimpleResponse {
	//this serialization sucks, recheck Spark stuff for some real solution
	implicit val byteStringFormatter = new ByteStringFormatter[SimpleResponse] {
		def serialize(data: SimpleResponse): ByteString = {
			//...
		}

		def deserialize(bs: ByteString): SimpleResponse = {
			//...
		}
	}
}
*/

//consuming from queues through Camel
class QueueConsumer() extends Consumer with ActorPublisher[String] {
	//def endpointUri = "spring-redis://localhost:6379?command=SUBSCRIBE&channels=mychannel"	//&listenerContainer=#listenerContainer
	def endpointUri = "rabbitmq://localhost:5672/urls?queue=urls&routingKey=urls&autoDelete=false&username=test&password=test"
	//SQS/ElasticMQ: should redirect sqs.REGION.amazonaws.com to http://localhost:9324/, but in %SystemRoot%\System32\drivers\etc\hosts fails...
	// def endpointUri = "aws-sqs://urls?accessKey=1234&secretKey=1234&region=ap-southeast-1&amazonSQSEndpoint=sqs.ap-southeast-1.amazonaws.com"

	def receive = {
		case msg: CamelMessage => {
			implicit val camelContext = CamelExtension(context.system).context
			println("Camel message: " + msg.bodyAs[String])
			onNext(msg.bodyAs[String])
		}
		case msg: ActorPublisherMessage.Cancel => {
			println(msg.getClass + ": the stream subscriber (" + sender() + ") canceled the subscription.")
			context.stop(self)
			// onError()
			// onComplete()
			// system.shutdown()
			// Runtime.getRuntime.exit(0)
		}
		case msg: ActorPublisherMessage.Request => {
			println(msg.getClass + ": the stream subscriber (" + sender() + ") wants more!")
			// deliverBuf()
			// http://doc.akka.io/docs/akka-stream-and-http-experimental/1.0-M2/scala/stream-integrations.html
		}
		case msg => {
			println("ERROR, got a: " + msg.getClass)
		}
	}
}

//Camel actor sink
class ActorSink extends ActorSubscriber with Producer {
	// http://camel.apache.org/kafka.html
	def endpointUri = "kafka://server:port?zookeeperHost=localhost&zookeeperPort=2181&topic=dumps"  //&groupId=&partitioner=&clientId=
	// override def transformOutgoingMessage ...
	// override def routeResponse ...

	import ActorSubscriberMessage._
	val MaxQueueSize = 10
	var queue = Map.empty[Int, ActorRef]

	// val router = {
	// 	val routees = Vector.fill(3) {
	// 		ActorRefRoutee(context.actorOf(Props[Worker]))
	// 	}
	// 	Router(RoundRobinRoutingLogic(), routees)
	// }

	override val requestStrategy = new MaxInFlightRequestStrategy(max = MaxQueueSize) {
		override def inFlightInternally: Int = queue.size
	}

	// def receive = {
	// 	case OnNext(Msg(id, replyTo)) =>
	// 		queue += (id -> replyTo)
	// 		assert(queue.size <= MaxQueueSize, s"queued too many: ${queue.size}")
	// 		router.route(Work(id), self)
	// 	case Reply(id) =>
	// 		queue(id) ! Done(id)
	// 		queue -= id
	// }
}

object AkkaScraping {
	implicit val system = ActorSystem()
	implicit val materializer = ActorMaterializer()
	implicit val ec = system.dispatcher

	val redditAPIRate = FiniteDuration(500, MILLISECONDS)
	val timeOut = FiniteDuration(10, SECONDS)

	//throttling the easy way
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

	def fetcher(): Flow[String, HttpResponse, Unit] = {
		import akka.http.scaladsl.model._
		import akka.http.scaladsl.Http
		var parallelism = 4
		Flow[String]
		.via(throttle(redditAPIRate))
		.mapAsync(parallelism)((url: String) => {
			tryCatch(()=>{
			val headers = List(
				//`Content-Type`(`application/json`)
			)
			val req = HttpRequest(uri = url).withHeaders(headers)
			println("time: " + System.nanoTime() / 1000000000.0)
			println(s"fetching $url")
			Http().singleRequest(req)
			})
		})
	}

	def decode = (resp: HttpResponse) => {
	tryCatch(()=>{
	// println("{")
	println("status: " + resp.status.toString())
	val enc = resp.encoding.value match {
		case "identity" => "UTF-8"
		case s => s
	}
	println("encoding: [" + enc + "]")
	resp.headers.foreach(h =>
		println("header: " + h.value())
	)
	println("type: " + resp.entity.contentType)
	//import scala.concurrent.ExecutionContext.Implicits.global
	val body = resp
		.entity.getDataBytes().asScala
		.map( _.decodeString(enc) )
		.runFold("") { case (s1, s2) => s1 + s2 }
		// .onComplete {
		// 	case Success(s: String) => s //println("body: " + s)
		// 	case Success(x) => { val s = "unknown: " + x; println(s); s }
		// 	case Failure(ex) => { val s = "error: " + ex; println(s); s }
		// }
	// println("}")
	// val redis = RedisClient()
	// redis.rpush("dumps", enc)
		body
		})
	}

	// def handleResp = (resp: HttpResponse) => {
	def decoder(): Flow[HttpResponse, String, Unit] = {
		Flow[HttpResponse]
		.mapAsync(4)(decode)
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

	def main(args: Array[String]): Unit = {
		//import system.dispatcher

		//throttling the sophisticated way
		//val limiterProps = Limiter.props(maxAvailableTokens = 10, tokenRefreshPeriod = new FiniteDuration(5, SECONDS), tokenRefreshAmount = 1)
		//val limiter = system.actorOf(limiterProps, name = "testLimiter")
		// limitGlobal(limiter, redditAPIRate) ~>

		//graphs the graphy way
		/*
		FlowGraph.closed() { implicit b =>
			import FlowGraph.Implicits._
			urlSource ~> fetcher ~> printSink
		}.run()
		*/

		// Reactive Rabbit way
		// reactiveRabbitSource()

		// Camel way
		Source.actorPublisher[String](Props(classOf[QueueConsumer]))

		// testy way
		// Source(List("http://akka.io/", "http://baidu.com/"))

		.via(fetcher)
		// .runForeach(resp => decode(resp))
		// .onComplete({
			// case _ =>
		// 		// system.shutdown()
		// 		// Runtime.getRuntime.exit(0)
		// })
		.via(decoder)
		.to(Sink.actorSubscriber[String](Props(classOf[ActorSink])))

	}
}
