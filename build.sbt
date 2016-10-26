name := "CookIM"

version := "0.1.0-SNAPSHOT"

scalaVersion := "2.11.8"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

libraryDependencies ++= {
  val akkaV = "2.4.11"
  Seq(
    "com.typesafe.akka" %%  "akka-actor"  % akkaV,
    "com.typesafe.akka" %% "akka-remote" % akkaV,
    "com.typesafe.akka" %% "akka-cluster" % akkaV,
    "com.typesafe.akka" %% "akka-cluster-tools" % akkaV,
    "com.typesafe.akka" %% "akka-http-core" % akkaV,
    "com.typesafe.akka" %% "akka-stream" % akkaV,
    "com.typesafe.akka" %% "akka-http-experimental" % akkaV,
    "com.typesafe.play" %% "play-json" % "2.5.8",
    "org.slf4j" % "slf4j-simple" % "1.7.10",
    "org.scala-lang.modules" %% "scala-async" % "0.9.5",
    "com.sksamuel.scrimage" %% "scrimage-core" % "2.1.1",
    "com.sksamuel.scrimage" %% "scrimage-io-extra" % "2.1.1",
    "redis.clients" % "jedis" % "2.8.1",  //redis for java
    "com.softwaremill.akka-http-session" %% "core" % "0.2.6",
    "com.esotericsoftware" % "kryo" % "3.0.3",
    "com.github.romix.akka" %% "akka-kryo-serialization" % "0.4.1",
    "commons-cli" % "commons-cli" % "1.3.1",
    "org.reactivemongo" %% "reactivemongo" % "0.11.14",
    "org.reactivemongo" %% "reactivemongo-play-json" % "0.11.14"
 )
}

//sbt使用代理
// javaOptions in console ++= Seq(
//  "-Dhttp.proxyHost=cmproxy-sgs.gmcc.net",
//  "-Dhttp.proxyPort=8081"
// )
// javaOptions in run ++= Seq(
//  "-Dhttp.proxyHost=cmproxy-sgs.gmcc.net",
//  "-Dhttp.proxyPort=8081"
// )
