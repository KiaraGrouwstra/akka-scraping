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
  // def setupPublisher(channel: Channel, self: ActorRef) {
  //   val queue = channel.queueDeclare().getQueue
  //   channel.queueBind(queue, "amq.fanout", "")
  // }
  //could customize with ChannelActor.props(setupPublisher) to auto make/bind queue if channel new
  // val channelActor: ActorRef = (connectionActor ! CreateChannel(ChannelActor.props()))
  val channelActor: ActorRef = connectionActor.createChannel(ChannelActor.props())


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
