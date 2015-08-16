package common {

import play._
import play.api._
// for RabbitMQ interop
import com.thenewmotion.akka.rabbitmq._
// import com.spingo.op_rabbit.PlayJsonSupport._
// import com.spingo.op_rabbit._
// import com.spingo.op_rabbit.consumer._
// // import com.spingo.op_rabbit.subscription.Directives._
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.Play.current
import play.api.libs.concurrent.Akka
import akka.actor._
import akka.remote._
import play.Application;
import play.GlobalSettings;
import play.Logger;

object Global extends GlobalSettings {

  val system = ActorSystem()
  // val rabbitMq = system.actorOf(Props[RabbitControl])

  val factory = new ConnectionFactory()
  val config = com.typesafe.config.ConfigFactory.load().getConfig("rabbitmq")
  factory.setHost(config.getString("host"))
  factory.setPort(config.getInt("port"))
  factory.setUsername(config.getString("username"))
  factory.setPassword(config.getString("password"))
  val connectionActor = system.actorOf(ConnectionActor.props(factory))
  // this function will be called each time new channel received
  def setupPublisher(channel: Channel, self: ActorRef) {
    //hm, I fear I'll wanna do this queue creation/binding for each URL domain received, not just once like this...
//    val queue = channel.queueDeclare().getQueue
//    val queue = channel.queueDeclare("queue_name", false, false, false, null).getQueue
//    channel.queueBind(queue, "amq.fanout", "")
  }
  //could customize with ChannelActor.props(setupPublisher) to auto make/bind queue if channel new...
  //... but that's pub/sub pattern while I want shared queue. Not to mention I wanna distinguish domains, not channels.
  // val channelActor: ActorRef = (connectionActor ! CreateChannel(ChannelActor.props()))
  println("Synchronously connecting to RabbitMQ!")
  val channelActor: ActorRef = connectionActor.createChannel(ChannelActor.props(setupPublisher))
  println("Connected!")


  override def beforeStart(app: Application) {
    Logger.info("starting!")
    super.beforeStart(app)
  }

  override def onStart(app: Application) {
    Logger.info("started!")
    // initialize your stuff here
    super.onStart(app)
  }

  override def onStop(app: Application) {
    Logger.info("stopping!")
    super.onStop(app)
  }

}

}
