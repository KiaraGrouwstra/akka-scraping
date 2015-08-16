package controllers

import models._
import controllers._
import play.api.Routes
import play.api.cache._
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._
import play.api.mvc._
//import java.io.File
import scala.concurrent.duration._
import common.Global
import akka.actor._
import akka.remote._
import com.thenewmotion.akka.rabbitmq._
// import com.spingo.op_rabbit.PlayJsonSupport._
// import com.spingo.op_rabbit._
// import com.spingo.op_rabbit.consumer._
// // import com.spingo.op_rabbit.subscription.Directives._
import scala.concurrent.ExecutionContext.Implicits.global

/** Application controller, handles authentication */
class Application(val cache: CacheApi) extends Controller with Security { //JadeController

  val cacheDuration = 1.day

  /**
   * Caching action that caches an OK response for the given amount of time with the key.
   * NotFound will be cached for 5 mins. Any other status will not be cached.
   */
  def Caching(key: String, okDuration: Duration) =
    new Cached(cache)
      .status(_ => key, OK, okDuration.toSeconds.toInt)
      .includeStatus(NOT_FOUND, 5.minutes.toSeconds.toInt)

  /** Serves the index page, see views/index.scala.html */
  def index = Action { implicit request =>
    Ok(views.html.index())
  }
  
  val exchange = "urls"
      
//  def makeQueue(channel: Channel) {
  def makeQueue(channel: Channel, name: String) {
    val queue = channel.queueDeclare(name, false, false, false, null).getQueue
//    val ok: com.rabbitmq.client.AMQP.Queue.BindOk =
    channel.queueBind(queue, exchange, name)
  }

//  def publish = (channel: Channel) => {
  def publish = (channel: Channel, routing_key: String, msg: String) => {
//    channel.basicPublish(exchange, domain, null, url.getBytes)
    channel.basicPublish(exchange, routing_key, null, msg.getBytes)
  }
  
  def postUrls() = Action { implicit request =>
    // request.body.asFormUrlEncoded.get.map{ case(k,v) =>
    //   println(k + ": " + v.mkString)
    // }

    val input = request.body.asFormUrlEncoded.get.get("urls").get.mkString
    // println(input)

    // val urls = input.split("\\s+".r)
    val urls = input.split("\r\n")
    for (url <- urls) {
      println("URL: " + url.trim)
      // Global.rabbitMq ! QueueMessage(url.trim, queue = "urls")
      val uri = new URL(url)
      val host = uri.getHost()
      // TODO: strip off www(2?) and subdomains, i.e. just get the "\w+\.\w+$"...?
      // but assumes 1 TLD, failing for dual ones like .co.uk, and for none (IP-based, localhost), though those should lack subs too...
      // dual TLDs to pattern-match against: https://en.wikipedia.org/wiki/Second-level_domain
      // http://stackoverflow.com/a/14662475/1502035
      // on that topic I'll wanna block having people use my scraper for localhost, 127.0.0.1, 192.168.*.*...
      val domain = host
//      Global.channelActor ! ChannelMessage(makeQueue, dropIfNoChannel = false)
      Global.channelActor ! ChannelMessage(makeQueue(_: Channel, domain), dropIfNoChannel = false)
//      Global.channelActor ! ChannelMessage(publish, dropIfNoChannel = false)
      Global.channelActor ! ChannelMessage(publish(_: Channel, domain, url), dropIfNoChannel = false)
    }

    Redirect("/").flashing("success" -> ("Done! Added " + urls.size.toString() + " urls!"))
  }

  /**
   * Retrieves all routes via reflection.
   * http://stackoverflow.com/questions/12012703/less-verbose-way-of-generating-play-2s-javascript-router
   * @todo If you have controllers in multiple packages, you need to add each package here.
   */
  val routeCache = {
    val jsRoutesClasses = Seq(classOf[routes.javascript]) // TODO add your own packages
    jsRoutesClasses.flatMap { jsRoutesClass =>
      val controllers = jsRoutesClass.getFields.map(_.get(null))
      controllers.flatMap { controller =>
        controller.getClass.getDeclaredMethods.filter(_.getName != "_defaultPrefix").map { action =>
          action.invoke(controller).asInstanceOf[play.api.routing.JavaScriptReverseRoute]
        }
      }
    }
  }

  /**
   * Returns the JavaScript router that the client can use for "type-safe" routes.
   * Uses browser caching; set duration (in seconds) according to your release cycle.
   * @param varName The name of the global variable, defaults to `jsRoutes`
   */
  def jsRoutes(varName: String = "jsRoutes") = Caching("jsRoutes", cacheDuration) {
    Action { implicit request =>
      Ok(play.api.routing.JavaScriptReverseRouter(varName)(routeCache: _*)).as(JAVASCRIPT)
    }
  }

  /** Used for obtaining the email and password from the HTTP login request */
  case class LoginCredentials(email: String, password: String)

  /** JSON reader for [[LoginCredentials]]. */
  implicit val LoginCredentialsFromJson = (
    (__ \ "email").read[String](minLength[String](5)) ~
      (__ \ "password").read[String](minLength[String](8))
    )((email, password) => LoginCredentials(email, password))

  /**
   * Log-in a user. Expects the credentials in the body in JSON format.
   *
   * Set the cookie [[AuthTokenCookieKey]] to have AngularJS set the X-XSRF-TOKEN in the HTTP
   * header.
   *
   * @return The token needed for subsequent requests
   */
  def login() = Action(parse.json) { implicit request =>
    request.body.validate[LoginCredentials].fold(
      errors => {
        BadRequest(Json.obj("status" -> "KO", "message" -> JsError.toJson(errors)))
      },
      credentials => {
        // TODO Check credentials, log user in, return correct token
        User.findByEmailAndPassword(credentials.email, credentials.password).fold {
          BadRequest(Json.obj("status" -> "KO", "message" -> "User not registered"))
        } { user =>
          /*
           * For this demo, return a dummy token. A real application would require the following,
           * as per the AngularJS documentation:
           *
           * > The token must be unique for each user and must be verifiable by the server (to
           * > prevent the JavaScript from making up its own tokens). We recommend that the token is
           * > a digest of your site's authentication cookie with a salt) for added security.
           *
           */
          val token = java.util.UUID.randomUUID.toString
          cache.set(token, user.id.get)
          Ok(Json.obj("token" -> token))
            .withCookies(Cookie(AuthTokenCookieKey, token, None, httpOnly = false))
        }
      }
    )
  }

  /**
   * Log-out a user. Invalidates the authentication token.
   *
   * Discard the cookie [[AuthTokenCookieKey]] to have AngularJS no longer set the
   * X-XSRF-TOKEN in HTTP header.
   */
  def logout() = HasToken(parse.empty) { token => userId => implicit request =>
    cache.remove(token)
    Ok.discardingCookies(DiscardingCookie(name = AuthTokenCookieKey))
  }

}
