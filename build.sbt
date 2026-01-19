name := """play-life"""

version := "0.1"

scalaVersion := "2.13.16"

libraryDependencies ++= Seq(
  guice,
  "org.webjars" %% "webjars-play" % "3.0.1",
  "org.webjars" % "bootstrap" % "5.3.3",
  "org.webjars" % "jquery" % "3.7.1",
  "org.scalatest" %% "scalatest" % "3.2.19" % Test,
  "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.1" % Test,
  "org.scalatestplus" %% "selenium-4-21" % "3.2.19.0" % Test,
  "com.google.guava" % "guava" % "33.4.0-jre"
)

enablePlugins(PlayScala)
