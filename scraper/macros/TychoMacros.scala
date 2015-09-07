package org.tycho.misc
// macros
//import scala.reflect.macros._
import language.experimental.macros
//import reflect.macros.Context
import scala.reflect.macros.blackbox.Context
import scala.annotation.StaticAnnotation
import scala.reflect.runtime.{universe => ru}

object TychoMacros {

  def flwMacro(c: Context)(s: c.Expr[Any]): c.Expr[Any] = {
    import c.universe._
//    println("before: " + show(s.tree))
//    ((url: String, resp: akka.http.scaladsl.model.HttpResponse) => AkkaScraping.this.decoderFlw(url, resp))
//    val out =
    c.Expr( q"""dynamicFlow(getParTTag($s.tupled)).mapAsync(parallelism)($s.tupled)""")
//    println("after: " + show(out.tree))
//    out
  }
  def flw(s: Any) = macro flwMacro

// Failed hard:
//  println("macro: " + getTypeTag(flw{decoderFlw _}).tpe)
//  old: akka.stream.scaladsl.Flow[(String, akka.http.scaladsl.model.HttpResponse),(String, String),Unit]
//  macro: Any


}
