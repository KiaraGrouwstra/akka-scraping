makePomConfiguration := makePomConfiguration.value.copy(file = new File("pom.xml"))

name := "spark-kafka"
mainClass in assembly := Some("org.tycho.scraping.SparkMain")

//scalaVersion := "2.10.4"	//"2.11.6" exists, but match Spark's
scalaVersion in ThisBuild := "2.11.7"

//does this help?
fork := true
offline := true

val sparkVersion = "1.4.0"
val kafkaVersion = "0.8.2.1"
val bijectVersion = "0.8.1"

// _2.11
libraryDependencies ++= Seq(
// spark
  "org.apache.spark" %% "spark-core" % sparkVersion,
	"org.apache.spark" %% "spark-streaming" % sparkVersion,
  // set the packages to provided when submitting the script as a job to a remote Spark server
  // "org.apache.spark" %% "spark-core" % sparkVersion % "provided",
	// "org.apache.spark" %% "spark-streaming" % sparkVersion % "provided",
	"org.apache.spark" %% "spark-streaming-kafka" % sparkVersion,
	"org.apache.spark" %% "spark-streaming-kafka-assembly" % sparkVersion,
	
//bijection
	"com.twitter" %% "bijection-core" % bijectVersion,
	"com.twitter" %% "bijection-avro" % bijectVersion,
//	"com.twitter" %% "bijection-protobuf" % bijectVersion,
//	"com.twitter" %% "bijection-thrift" % bijectVersion,
//	"com.twitter" %% "bijection-guava" % bijectVersion,
//	"com.twitter" %% "bijection-scrooge" % bijectVersion,
//	"com.twitter" %% "bijection-json" % bijectVersion,
//	"com.twitter" %% "bijection-util" % bijectVersion,
//	"com.twitter" %% "bijection-clojure" % bijectVersion,
//	"com.twitter" %% "bijection-netty" % bijectVersion,
//	"com.twitter" %% "bijection-hbase" % bijectVersion,
	
	"org.scalatest" %% "scalatest" % "2.2.4" % "test",
	//"commons-pool" % "commons-pool" % "20030825.183949",
	"org.apache.commons" % "commons-pool2" % "2.4.2",
	"org.projectlombok" % "lombok" % "1.16.4",
	//"org.apache.kafka" %% "kafka" % kafkaVersion
	"org.apache.kafka" % "kafka-clients" % kafkaVersion
)

mergeStrategy in assembly := {
	case m if m.toLowerCase.endsWith("manifest.mf")          => MergeStrategy.discard
	case m if m.toLowerCase.matches("meta-inf.*\\.sf$")      => MergeStrategy.discard
	case "log4j.properties"                                  => MergeStrategy.discard
	case m if m.toLowerCase.startsWith("meta-inf/services/") => MergeStrategy.filterDistinctLines
	case "reference.conf"                                    => MergeStrategy.concat
	case _                                                   => MergeStrategy.first
}
