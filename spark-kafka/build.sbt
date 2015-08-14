name := "spark-kafka"
scalaVersion := "2.10.4"	//"2.11.6" exists, but match Spark's
mainClass in assembly := Some("org.tycho.fun.SparkKafka")
//mainClass in assembly := Some("SparkKafka")

// _2.11
libraryDependencies ++= Seq(
	"org.apache.spark" % "spark-core_2.10" % "1.4.0" % "provided",
	"org.apache.spark" % "spark-streaming_2.10" % "1.4.0" % "provided",
	"org.apache.spark" % "spark-streaming-kafka_2.10" % "1.4.0",
	"org.apache.spark" % "spark-streaming-kafka-assembly_2.10" % "1.4.0",
	//"org.apache.kafka" % "kafka_2.10" % "0.8.2.1"
	"org.apache.kafka" % "kafka-clients" % "0.8.2.1"
)

mergeStrategy in assembly := {
	case m if m.toLowerCase.endsWith("manifest.mf")          => MergeStrategy.discard
	case m if m.toLowerCase.matches("meta-inf.*\\.sf$")      => MergeStrategy.discard
	case "log4j.properties"                                  => MergeStrategy.discard
	case m if m.toLowerCase.startsWith("meta-inf/services/") => MergeStrategy.filterDistinctLines
	case "reference.conf"                                    => MergeStrategy.concat
	case _                                                   => MergeStrategy.first
}
