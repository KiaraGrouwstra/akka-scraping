// Comment to get more information during initialization
logLevel := Level.Warn

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

// addSbtPlugin("net.kindleit" %% "play2-scalate-compiler" % "0.1-SNAPSHOT")

//addSbtPlugin("com.typesafe.sbt" % "sbt-js-engine" % "1.1.4-SNAPSHOT")

//addSbtPlugin("com.github.mmizutani" % "sbt-play-gulp" % "0.0.1")

// addSbtPlugin("se.woodenstake" %% "sbt-gulp-task" % "0.1")

// lazy val sbtAutoprefixer = uri("git://github.com/matthewrennie/sbt-autoprefixer")
// lazy val root = project.in(file(".")).enablePlugins(SbtWeb)
// lazy val root = project.in(file(".")).dependsOn(sbtAutoprefixer)
// pipelineStages in Assets := Seq(autoprefixer)
// // AutoprefixerKeys.inlineSourceMap in Assets := true
// // includeFilter in autoprefixer := GlobFilter("*.css"),
