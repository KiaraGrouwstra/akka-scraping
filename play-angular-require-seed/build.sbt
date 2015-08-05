import WebKeys._
import play.PlayImport.PlayKeys.playRunHooks
import Gulp._

// TODO Replace with your project's/module's name
name := "play-angular-require-seed"

// TODO Set your organization here; ThisBuild means it will apply to all sub-modules
organization in ThisBuild := "your.organization"

// TODO Set your version here
version := "2.4.2-SNAPSHOT"

scalaVersion in ThisBuild := "2.11.7"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

val akkaVersion = "2.3.12"
val camelVersion = "2.15.2"
val opRabbitVersion = "1.0.0-M12"

resolvers ++= Seq(
  "SpinGo OSS" at "http://spingo-oss.s3.amazonaws.com/repositories/releases"
)

// Dependencies
libraryDependencies ++= Seq(
  // op-rabbit for RabbitMQ
  "com.spingo" %% "op-rabbit-core"        % opRabbitVersion,
  "com.spingo" %% "op-rabbit-play-json"   % opRabbitVersion,
  "com.spingo" %% "op-rabbit-json4s"      % opRabbitVersion,
  // "com.spingo" %% "op-rabbit-airbrake"    % opRabbitVersion,
  // "com.spingo" %% "op-rabbit-akka-stream" % opRabbitVersion,
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-remote" % akkaVersion,

  //Camel
  // "org.apache.camel" % "camel-rabbitmq" % camelVersion,
	// "org.apache.camel" % "camel-aws" % camelVersion,
	// "org.apache.camel" % "camel-spring-redis" % camelVersion,

  filters,
  cache,
  // WebJars (i.e. client-side) dependencies
  "org.webjars.bower" % "es5-shim" % "4.1.8",
  "org.webjars.bower" % "es6-shim" % "0.27.1",
  "org.webjars" % "requirejs" % "2.1.14-1",
  "org.webjars" % "jquery" % "1.11.1",
  "org.webjars" % "bootstrap" % "3.3.5" exclude("org.webjars", "jquery"),
  "org.webjars" % "angularjs" % "1.4.3" exclude("org.webjars", "jquery"),
  "org.webjars" % "angular-ui" % "0.4.0-3",
  //http://angular-ui.github.io/bootstrap/
  "org.webjars" % "angular-ui-bootstrap" % "0.13.0",
  //https://material.angularjs.org/latest/#/demo/
  //https://github.com/heyflock/angular-bootstrap-material
  "org.webjars" % "angular-material" % "0.10.1-rc3",
  //http://fezvrasta.github.io/bootstrap-material-design/
  //http://fezvrasta.github.io/bootstrap-material-design/bootstrap-elements.html
  "org.webjars" % "bootstrap-material-design" % "0.3.0",
  //"org.webjars" % "underscorejs" % "1.6.0-3",
  "org.webjars" % "lodash" % "3.9.0",
  "org.webjars" % "autoprefixer" % "5.2.0"
)

// Scala Compiler Options
scalacOptions in ThisBuild ++= Seq(
  "-target:jvm-1.8",
  "-encoding", "UTF-8",
  "-deprecation", // warning and location for usages of deprecated APIs
  "-feature", // warning and location for usages of features that should be imported explicitly
  "-unchecked", // additional warnings where generated code depends on assumptions
  "-Xlint", // recommended additional warnings
  "-Xcheckinit", // runtime error when a val is not initialized due to trait hierarchies (instead of NPE somewhere else)
  "-Ywarn-adapted-args", // Warn if an argument list is modified to match the receiver
  "-Ywarn-value-discard", // Warn when non-Unit expression results are unused
  "-Ywarn-inaccessible",
  "-Ywarn-dead-code"
)

routesGenerator := InjectedRoutesGenerator

//
// sbt-web configuration
// https://github.com/sbt/sbt-web
//

// Configure the steps of the asset pipeline (used in stage and dist tasks)
// rjs = RequireJS, uglifies, shrinks to one file, replaces WebJars with CDN
// digest = Adds hash to filename
// gzip = Zips all assets, Asset controller serves them automatically when client accepts them
pipelineStages := Seq(rjs, digest, gzip)

// RequireJS with sbt-rjs (https://github.com/sbt/sbt-rjs#sbt-rjs)
// ~~~
RjsKeys.paths += ("jsRoutes" -> ("/jsroutes" -> "empty:"))

//RjsKeys.mainModule := "main"

// Asset hashing with sbt-digest (https://github.com/sbt/sbt-digest)
// ~~~
// md5 | sha1
//DigestKeys.algorithms := "md5"
//includeFilter in digest := "..."
//excludeFilter in digest := "..."

// HTTP compression with sbt-gzip (https://github.com/sbt/sbt-gzip)
// ~~~
// includeFilter in GzipKeys.compress := "*.html" || "*.css" || "*.js"
// excludeFilter in GzipKeys.compress := "..."

// JavaScript linting with sbt-jshint (https://github.com/sbt/sbt-jshint)
// ~~~
// JshintKeys.config := ".jshintrc"

// All work and no play...
// emojiLogs

// run gulp
//playRunHooks += RunSubProcess("gulp")

// import se.woodenstake.SbtGulpTask
// SbtGulpTask.gulpTaskSettings

playRunHooks <+= baseDirectory.map(base => Gulp(base))
