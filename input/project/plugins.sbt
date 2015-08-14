// Comment to get more information during initialization
//logLevel := Level.Debug
logLevel := Level.Info

// The Typesafe repository
resolvers += "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/"

// Use the Play sbt plugin for Play projects
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.4.2")

addSbtPlugin("com.typesafe.sbt" % "sbt-jshint" % "1.0.3")

addSbtPlugin("com.typesafe.sbt" % "sbt-less" % "1.0.4")

addSbtPlugin("com.typesafe.sbt" % "sbt-rjs" % "1.0.7")

addSbtPlugin("com.typesafe.sbt" % "sbt-digest" % "1.1.0")

addSbtPlugin("com.typesafe.sbt" % "sbt-gzip" % "1.0.0")

addSbtPlugin("com.typesafe.sbt" % "sbt-coffeescript" % "1.0.0")

addSbtPlugin("com.jamesward" % "play-auto-refresh" % "0.0.13")

addSbtPlugin("com.typesafe.sbteclipse" % "sbteclipse-plugin" % "4.0.0")

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.13.0")

//addSbtPlugin("org.apache.maven.plugins" % "maven-assembly-plugin" % "2.5.5")

//addSbtPlugin("net.alchim31.maven" % "scala-maven-plugin" % "3.2.1")

//addSbtPlugin("org.apache.maven.plugins" % "maven-compiler-plugin" % "2.0.2")

