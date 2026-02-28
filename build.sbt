import sbtcrossproject.CrossPlugin.autoImport.{CrossType, crossProject}
import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport._
import sbtcrossproject.CrossProject
import scalajscrossproject.ScalaJSCrossPlugin.autoImport._
import com.typesafe.sbt.packager.archetypes.JavaAppPackaging

ThisBuild / scalaVersion := Versions.scala3
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / semanticdbEnabled := true
ThisBuild / semanticdbVersion := scalafixSemanticdb.revision
ThisBuild / scalacOptions ++= Seq(
  "-feature",
  "-deprecation",
  "-encoding",
  "utf-8",
  "-explaintypes",
  "-Wunused:all"
)

Global / onChangedBuildSource := ReloadOnSourceChanges
scalafmtOnCompile := true
scalafixOnCompile := false

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
    libraryDependencies ++=
      ModuleDependencies.sharedCross.map { dep =>
        dep.organization %%% dep.artifact % dep.version
      }
  )

lazy val sharedJVM = shared.jvm
lazy val sharedJS = shared.js

lazy val backend = (project in file("backend"))
  .enablePlugins(JavaAppPackaging)
  .dependsOn(sharedJVM)
  .settings(
    name := "notes-backend",
    Compile / mainClass := Some("notes.backend.Main"),
    libraryDependencies ++=
      ModuleDependencies.backendJvmScala.map { dep =>
        dep.organization %% dep.artifact % dep.version
      } ++
        ModuleDependencies.backendJvmJava.map { dep =>
          dep.organization % dep.artifact % dep.version
        } :+
        (Libraries.Logging.logbackClassic.organization % Libraries.Logging.logbackClassic.artifact % Libraries.Logging.logbackClassic.version) :+
        (Libraries.Testing.munit.organization %% Libraries.Testing.munit.artifact % Libraries.Testing.munit.version % Test)
  )

lazy val frontend = (project in file("frontend"))
  .enablePlugins(ScalaJSPlugin)
  .dependsOn(sharedJS)
  .settings(
    name := "notes-frontend",
    scalaJSUseMainModuleInitializer := true,
    libraryDependencies ++=
      ModuleDependencies.frontendJs.map { dep =>
        dep.organization %%% dep.artifact % dep.version
      } :+
        (Libraries.Testing.munit.organization %%% Libraries.Testing.munit.artifact % Libraries.Testing.munit.version % Test)
  )
