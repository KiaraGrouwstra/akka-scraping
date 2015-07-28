name := """webui"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.6"

libraryDependencies ++= Seq(
  //Jade
  "de.neuland" % "jade4j" % "0.3.11" from "https://raw.github.com/neuland/jade4j/master/releases/de/neuland/jade4j/0.3.11/jade4j-0.3.11.jar",
  "com.googlecode.concurrentlinkedhashmap" % "concurrentlinkedhashmap-lru" % "1.3.1",
  "org.apache.commons" % "commons-jexl" % "2.1.1",

  //Play/Slick
  jdbc,
  "com.typesafe.slick" %% "slick" % "3.0.0",
  "com.typesafe.slick" %% "slick-codegen" % "3.0.0",
  "com.zaxxer" % "HikariCP" % "2.3.3",
  specs2 % Test,
  cache,
  ws
)

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

// Play provides two styles of routers, one expects its actions to be injected, the
// other, legacy style, accesses its actions statically.
routesGenerator := InjectedRoutesGenerator
