// Comment to get more information during initialization
// logLevel := Level.Debug
logLevel := Level.Info

// The Typesafe repository
resolvers += "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/"

addSbtPlugin("com.typesafe.sbteclipse" % "sbteclipse-plugin" % "4.0.0")

addSbtPlugin("org.scala-sbt.plugins" % "sbt-onejar" % "0.8")
