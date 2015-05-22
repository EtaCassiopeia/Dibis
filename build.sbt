name := """Dibis"""  //Distributed Bloom Filter Inquiry Service

version := "1.0"

scalaVersion := "2.10.5"

//scalacOptions ++= Seq("-Xlint","-Xfatal-warnings","-deprecation","-unchecked","-feature","-language:implicitConversions","-language:postfixOps")


libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-remote" % "2.3.10",
  "org.apache.curator" % "curator-framework" % "2.7.1" exclude("log4j","log4j") force(),
  "org.apache.curator" % "curator-recipes" % "2.7.1" exclude("log4j","log4j") force(),
  "org.slf4j" % "log4j-over-slf4j" % "1.7.7",
  "com.typesafe" % "config" % "1.2.1"

)

fork in run := true

enablePlugins(JavaAppPackaging, JDebPackaging)

enablePlugins(SbtNativePackager)

maintainer := "Mohsen Zainalpour<zainalpour@yahoo.com>"
packageSummary := "A Distributed Bloom Filter"
packageDescription := "A Distributed Bloom Filter"


    