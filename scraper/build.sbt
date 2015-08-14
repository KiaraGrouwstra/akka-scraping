name := "akka-scraping"
scalaVersion := "2.11.7"
mainClass in assembly := Some("org.tycho.scraping.AkkaScraping")

offline := true

val akkaVersion = "2.3.12"
val camelVersion = "2.15.2"	// Camel-Akka: 2.13.4
val kafkaVersion = "0.8.2.1"

resolvers ++= Seq(
  "rediscala" at "http://dl.bintray.com/etaty/maven",
  Resolver.sonatypeRepo("releases"),
  Resolver.sonatypeRepo("snapshots")
)

// % "provided"
libraryDependencies ++= Seq(
	// akka
	"com.typesafe.akka" %% "akka-actor" % akkaVersion,
	"com.typesafe.akka" %% "akka-remote" % akkaVersion,
	"com.typesafe.akka" %% "akka-camel" % akkaVersion,
	"com.typesafe.akka" %% "akka-kernel" % akkaVersion,
	"com.typesafe.akka" %% "akka-persistence-experimental" % akkaVersion,
	"com.typesafe.akka" %% "akka-cluster" % akkaVersion,
	"com.typesafe.akka" %% "akka-multi-node-testkit" % akkaVersion,
	"com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
	"com.typesafe.akka" %% "akka-stream-experimental" % "1.0",
	"com.typesafe.akka" %% "akka-http-core-experimental" % "1.0",
	"com.typesafe.akka" %% "akka-http-experimental" % "1.0",

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

mergeStrategy in assembly := {
	case m if m.toLowerCase.endsWith("manifest.mf")          => MergeStrategy.discard
	case m if m.toLowerCase.matches("meta-inf.*\\.sf$")      => MergeStrategy.discard
	case "log4j.properties"                                  => MergeStrategy.discard
	case m if m.toLowerCase.startsWith("meta-inf/services/") => MergeStrategy.filterDistinctLines
	case "reference.conf"                                    => MergeStrategy.concat
	case _                                                   => MergeStrategy.first
}
