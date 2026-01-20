import sbtcrossproject.CrossPlugin.autoImport.{crossProject, CrossType}

val scalaVer = "2.13.16"

lazy val root = project.in(file("."))
  .aggregate(server, client, sharedJvm, sharedJs)
  .settings(
    name := "play-life",
    version := "0.1"
  )

// Shared code cross-compiled for JVM and JS
lazy val shared = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("shared"))
  .settings(
    scalaVersion := scalaVer,
    libraryDependencies ++= Seq(
      "org.scalatest" %%% "scalatest" % "3.2.19" % Test
    )
  )
  .jvmSettings(
    // JVM-specific settings
  )
  .jsSettings(
    // JS-specific settings
  )

lazy val sharedJvm = shared.jvm
lazy val sharedJs = shared.js

// Scala.js client
lazy val client = project.in(file("client"))
  .enablePlugins(ScalaJSPlugin)
  .dependsOn(sharedJs)
  .settings(
    scalaVersion := scalaVer,
    scalaJSUseMainModuleInitializer := false,
    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-dom" % "2.8.0"
    ),
    // Output to public/javascripts for Play to serve
    Compile / fastLinkJS / scalaJSLinkerOutputDirectory :=
      baseDirectory.value / ".." / "public" / "javascripts" / "scalajs",
    Compile / fullLinkJS / scalaJSLinkerOutputDirectory :=
      baseDirectory.value / ".." / "public" / "javascripts" / "scalajs"
  )

// Play server
lazy val server = project.in(file("server"))
  .enablePlugins(PlayScala)
  .dependsOn(sharedJvm)
  .settings(
    scalaVersion := scalaVer,
    libraryDependencies ++= Seq(
      guice,
      ws,
      "org.webjars" %% "webjars-play" % "3.0.1",
      "org.webjars" % "bootstrap" % "5.3.3",
      "org.webjars" % "jquery" % "3.7.1",
      "org.scala-lang.modules" %% "scala-parallel-collections" % "1.0.4",
      "org.scalatest" %% "scalatest" % "3.2.19" % Test,
      "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.1" % Test,
      "org.scalatestplus" %% "selenium-4-21" % "3.2.19.0" % Test,
      "com.google.guava" % "guava" % "33.4.0-jre"
    ),
    // Use root public directory for assets
    Assets / unmanagedResourceDirectories += baseDirectory.value / ".." / "public"
  )
