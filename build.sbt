
name := "sakas3"
version := "0.1"
scalaVersion := "2.12.4"

// 基本配置
scalacOptions := Seq(
  "-language:_",
  "-unchecked",
  "-deprecation",
  "-feature"
)

val versions = new {
  val scala = "2.12.4"
  val akkaVersion = "2.5.13"
  val akkaHttpVersion = "10.1.1"
  val scalaTestFullVersion = "3.0.3"
  val junitFullVersion = "4.12"
  val scalaMockVersion = "3.6.0"
  val mockitoVersion = "2.8.47"
}

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % versions.akkaVersion,
  "com.typesafe.akka" %% "akka-stream" % versions.akkaVersion,
  "com.typesafe.akka" %% "akka-http" % versions.akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-spray-json" % versions.akkaHttpVersion,

  // unit test
  "junit" % "junit" % versions.junitFullVersion % Test,
  "com.typesafe.akka" %% "akka-testkit" % versions.akkaVersion % Test,
  "org.scalatest" %% "scalatest" % versions.scalaTestFullVersion,
  "org.scalamock" %% "scalamock-scalatest-support" % versions.scalaMockVersion,
  "org.mockito" % "mockito-core" % versions.mockitoVersion % Test
)

libraryDependencies ~= {
  _.map(_.exclude("org.slf4j", "slf4j-simple"))
}
