name := "akka-scraping"
scalaVersion := "2.11.7"
mainClass in assembly := Some("org.tycho.scraping.AkkaScraping")

val akkaVersion = "2.3.12"
val camelVersion = "2.15.2"	// Camel-Akka: 2.13.4

resolvers += "rediscala" at "http://dl.bintray.com/etaty/maven"

// % "provided"
libraryDependencies ++= Seq(
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
	"com.etaty.rediscala" %% "rediscala" % "1.4.0",
	"redis.clients" % "jedis" % "2.7.2",
	"io.scalac" %% "reactive-rabbit" % "1.0.1",
	// "ch.qos.logback" % "logback-classic" % "1.0.7",
	"org.apache.camel" % "camel-rabbitmq" % camelVersion,
	"org.apache.camel" % "camel-aws" % camelVersion,
	"org.apache.camel" % "camel-kafka" % camelVersion,
	"org.apache.camel" % "camel-spring-redis" % camelVersion,
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
