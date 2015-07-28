package controllers

import play.api._
import play.api.mvc._

object Application extends JadeController {

  def index = Action {
    Ok(render("index.jade"))
  }
}

/*
class Application extends Controller {

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

}
*/
