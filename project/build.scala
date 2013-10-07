import sbt._
import Keys._

object MagimixBuild extends Build {

  lazy val project = Project(
    id = "magimix",
    base = file(".")
  )
  .settings(
    scalaVersion := "2.10.3",
    libraryDependencies ++= Seq(
      "org.apache.jena" % "jena-core" % "2.11.0",
      "org.jsoup" % "jsoup" % "1.7.2",
      "org.json4s" %% "json4s-native" % "3.2.5",
      "net.databinder.dispatch" %% "dispatch-core" % "0.11.0",
      "org.scalatra" %% "scalatra" % "2.2.1",
      "com.typesafe" % "config" % "1.0.2",
      "org.eclipse.jetty" % "jetty-webapp" % "8.1.8.v20121106" % "compile",
      "org.eclipse.jetty.orbit" % "javax.servlet" % "3.0.0.v201112011016" % "compile,provided" artifacts (Artifact("javax.servlet", "jar", "jar"))
    )
  )

}
