lazy val commonSettings = Seq(
  //	organization := "org.tycho",
  offline := true,
  scalaVersion := "2.11.7"
)

val scalaVer = "2.11.7"
val metaVersion = "0.1.0-SNAPSHOT"
//val akkaVersion = "2.4.0-RC1"
val akkaVersion = "2.3.12"
val camelVersion = "2.15.2"	// Camel-Akka: 2.13.4
val kafkaVersion = "0.8.2.1"
val sprayVersion = "1.3.3"

lazy val core = (project in file(".")).
  dependsOn(macroSub).
  settings(commonSettings: _*).
  settings(
    name := "akka-scraping",
    mainClass in assembly := Some("org.tycho.scraping.AkkaScraping"),

    // % "provided"
    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % "2.2.4" % "test",
      "org.scalacheck" %% "scalacheck" % "1.12.4" % "test",
      "org.scalameta" %% "scalameta" % metaVersion,
      //	"org.scalameta" %% "scalahost" % metaVersion,
      "org.scala-lang" % "scala-reflect" % scalaVer,
      "org.scala-lang" % "scala-compiler" % scalaVer,
      "org.scala-lang" % "scala-library" % scalaVer,

      // akka
      "com.typesafe.akka" %% "akka-actor" % akkaVersion,
      "com.typesafe.akka" %% "akka-contrib" % akkaVersion,
      "com.typesafe.akka" %% "akka-remote" % akkaVersion,
      "com.typesafe.akka" %% "akka-camel" % akkaVersion,
      "com.typesafe.akka" %% "akka-kernel" % akkaVersion,
      "com.typesafe.akka" %% "akka-persistence-experimental" % akkaVersion,
      "com.typesafe.akka" %% "akka-cluster" % akkaVersion,
      "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test",
      "com.typesafe.akka" %% "akka-multi-node-testkit" % akkaVersion % "test",
      "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
      //"com.typesafe.akka" %% "akka-typed-experimental" % "2.4.0-RC1",
      "com.typesafe.akka" %% "akka-stream-experimental" % "1.0",
      "com.typesafe.akka" %% "akka-http-core-experimental" % "1.0",
      "com.typesafe.akka" %% "akka-http-experimental" % "1.0",

      //"com.typesafe.akka" %% "akka-http-spray-json" % "1.0",
      //^ can't find how to use this yet
      //"io.spray" %% "spray-json" % sprayVersion,
      "io.spray" %% "spray-client" % sprayVersion,
      "io.spray" %% "spray-httpx" % sprayVersion,
      "io.spray" %% "spray-http" % sprayVersion,

      //"net.liftweb" %% "lift-json" % "3.0-M6",
      //"com.propensive" %% "rapture-json-json4s" % "1.1.0",
      //"com.owlike" % "genson" % "1.3",
      "com.owlike" %% "genson-scala" % "1.3",

      // queues
      "com.etaty.rediscala" %% "rediscala" % "1.4.0",
      "redis.clients" % "jedis" % "2.7.2",
      "io.scalac" %% "reactive-rabbit" % "1.0.1",
      // "ch.qos.logback" % "logback-classic" % "1.0.7",

      // camel
      "org.apache.camel" % "camel-rabbitmq" % camelVersion,
      "org.apache.camel" % "camel-aws" % camelVersion,
      // "org.apache.camel" % "camel-kafka" % camelVersion,
      ("org.apache.camel" % "camel-kafka" % camelVersion).exclude("org.apache.kafka", "kafka_2.10"),
      "org.apache.camel" % "camel-spring-redis" % camelVersion,

      // kafka
      // "org.apache.kafka" %% "kafka" % kafkaVersion,
      // ("org.apache.kafka" % "kafka-clients" % kafkaVersion).exclude("org.apache.kafka", "kafka_2.10"),
      "org.apache.kafka" % "kafka-clients" % kafkaVersion,
      // ("org.apache.kafka" % "kafka-examples" % kafkaVersion).exclude("org.apache.kafka", "kafka_2.10"),
      // ("org.apache.kafka" % "kafka-hadoop-producer" % kafkaVersion).exclude("org.apache.kafka", "kafka_2.10"),
      // ("org.apache.kafka" % "kafka-hadoop-consumer" % kafkaVersion).exclude("org.apache.kafka", "kafka_2.10"),
      "com.softwaremill" %% "reactive-kafka" % "0.7.0",

      // shapeless
      "com.chuusai" %% "shapeless" % "2.2.5",
      // scalaVersion := "2.11.7"
      // avoid SBT 0.13.6, or...
      // incOptions := incOptions.value.withNameHashing(false)


      "com.typesafe.akka" % "akka" % "2.2.0-RC2"
    )

  )


resolvers ++= Seq(
  Resolver.sonatypeRepo("releases"),
  Resolver.sonatypeRepo("snapshots"),
  //	"Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
  "rediscala" at "http://dl.bintray.com/etaty/maven"
)

mergeStrategy in assembly := {
  case m if m.toLowerCase.endsWith("manifest.mf")          => MergeStrategy.discard
  case m if m.toLowerCase.matches("meta-inf.*\\.sf$")      => MergeStrategy.discard
  case "log4j.properties"                                  => MergeStrategy.discard
  case m if m.toLowerCase.startsWith("meta-inf/services/") => MergeStrategy.filterDistinctLines
  case "reference.conf"                                    => MergeStrategy.concat
  case _                                                   => MergeStrategy.first
}

lazy val macroSub = (project in file("macros")).
  settings(commonSettings: _*).
  settings(
//    name := "akka-scraping",
//    mainClass in assembly := Some("org.tycho.scraping.AkkaScraping"),

    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-reflect" % scalaVersion.value
    )
    // other settings here
  )



