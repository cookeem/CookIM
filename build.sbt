name := "CookIM"

version := "0.1.0-SNAPSHOT"

scalaVersion := "2.11.8"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

libraryDependencies ++= {
  val akkaV = "2.4.14"
  val akkaHttpV = "10.0.0"
  Seq(
    "com.typesafe.akka" %%  "akka-actor"  % akkaV,
    "com.typesafe.akka" %% "akka-remote" % akkaV,
    "com.typesafe.akka" %% "akka-cluster" % akkaV,
    "com.typesafe.akka" %% "akka-cluster-tools" % akkaV,
    "com.typesafe.akka" %% "akka-stream" % akkaV,
    "com.typesafe.akka" %% "akka-http-core" % akkaHttpV,
    "com.typesafe.akka" %% "akka-http" % akkaHttpV,
    "com.typesafe.akka" %% "akka-testkit" % akkaV,
    "com.typesafe.akka" %% "akka-stream-testkit" % akkaV,
    "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpV,
    "org.scalactic" %% "scalactic" % "3.0.1",
    "org.scalatest" %% "scalatest" % "3.0.1" % "test",
    "com.typesafe.play" %% "play-json" % "2.5.8",
    "org.slf4j" % "slf4j-simple" % "1.7.10",
    "com.sksamuel.scrimage" %% "scrimage-core" % "2.1.1",
    "com.sksamuel.scrimage" %% "scrimage-io-extra" % "2.1.1",
    "com.esotericsoftware" % "kryo" % "3.0.3",
    "com.github.romix.akka" %% "akka-kryo-serialization" % "0.4.1",
    "commons-cli" % "commons-cli" % "1.3.1",
    "io.jsonwebtoken" % "jjwt" % "0.7.0",
    "org.reactivemongo" %% "reactivemongo" % "0.12.0",
    "org.reactivemongo" %% "reactivemongo-play-json" % "0.12.0"
 )
}

////sbt使用代理
//javaOptions in console ++= Seq(
//  "-Dhttp.proxyHost=cmproxy-sgs.gmcc.net",
//  "-Dhttp.proxyPort=8081"
//)
//javaOptions in run ++= Seq(
//  "-Dhttp.proxyHost=cmproxy-sgs.gmcc.net",
//  "-Dhttp.proxyPort=8081"
//)
