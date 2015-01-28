name := "AirPlaneActor"

version := "1.0"

scalaVersion := "2.11.5"

libraryDependencies += "com.typesafe.akka" % "akka-actor" % "2.3.8"

//val akkaVersion = "2.4-SNAPSHOT"


libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest_2.11" %  "2.2.1" % "test",
  "com.typesafe.akka" %% "akka-testkit" % "2.1.0",
  "com.typesafe.akka" %% "akka-actor" % "2.1.0"
)

//name := "AirPlaneActor"
//
//version := "0.1"
//
//scalaVersion := "2.10.0-M7"
//
//resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases"
//
//libraryDependencies ++= Seq(
//  "org.scalatest" % "scalatest_2.10.0-M7" % "1.9-2.10.0-M7-B1" % "test",
//  "com.typesafe.akka" % "akka-actor_2.10.0-M7" % "2.1-M2",
//  "com.typesafe.akka" % "akka-testkit_2.10.0-M7" % "2.1-M2"
//)
