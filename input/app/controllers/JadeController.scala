// package controllers
//
// import de.neuland.jade4j.Jade4J
// import de.neuland.jade4j.JadeConfiguration
// import de.neuland.jade4j.template.FileTemplateLoader
// import de.neuland.jade4j.template.JadeTemplate
// import play.api.mvc.Controller
// //import play.api.templates.Html
// import play.twirl.api.Html
// import play.twirl.api.HtmlFormat
// import scala.collection.JavaConversions._
// import java.io.IOException
//
//
// trait JadeController extends Controller {
//   val jadeConfig = new JadeConfiguration
//
//   val templateLoader = {
//     jadeConfig.setTemplateLoader(new FileTemplateLoader("app/views/", "UTF-8"))
//     jadeConfig.setMode(Jade4J.Mode.HTML)
//     jadeConfig.setPrettyPrint(true)
//   }
//
//   def render(template: String): Html = {
//     render(template, Map[String, String]())
//   }
//
//   def render(template: String, context: Map[String, Object]): Html = {
//     try {
//       val jadeTemplate: JadeTemplate = jadeConfig.getTemplate(template)
//       //new Html(new StringBuilder(jadeConfig.renderTemplate(jadeTemplate, context)))
//       Html.apply(new StringBuilder(jadeConfig.renderTemplate(jadeTemplate, context)).toString())
//     }
//     catch {
//       case ex: IOException => {
//         println("Missing file exception")
//         //return new Html(new StringBuilder)
//         Html.apply("")
//       }
//     }
//   }
// }
