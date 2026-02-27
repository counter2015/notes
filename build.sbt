import sbtcrossproject.CrossPlugin.autoImport.{CrossType, crossProject}
import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport._
import sbtcrossproject.CrossProject
import scalajscrossproject.ScalaJSCrossPlugin.autoImport._

val scala3Version = "3.8.2"

ThisBuild / scalaVersion := scala3Version
ThisBuild / version := "0.1.0-SNAPSHOT"

lazy val root = (project in file("."))
  .aggregate(sharedJVM, sharedJS, backend, frontend)
  .settings(
    name := "notes",
    publish / skip := true
  )

lazy val shared: CrossProject = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("shared"))
  .settings(
    name := "notes-shared",
    libraryDependencies ++= Seq(
      "io.circe" %%% "circe-core" % "0.14.15",
      "io.circe" %%% "circe-generic" % "0.14.15"
    )
  )

lazy val sharedJVM = shared.jvm
lazy val sharedJS = shared.js

lazy val backend = (project in file("backend"))
  .dependsOn(sharedJVM)
  .settings(
    name := "notes-backend",
    libraryDependencies += "org.scalameta" %% "munit" % "1.2.3" % Test
  )

lazy val frontend = (project in file("frontend"))
  .enablePlugins(ScalaJSPlugin)
  .dependsOn(sharedJS)
  .settings(
    name := "notes-frontend",
    scalaJSUseMainModuleInitializer := true,
    libraryDependencies += "org.scalameta" %%% "munit" % "1.2.3" % Test
  )
