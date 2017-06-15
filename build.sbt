name := "CookIM"

version := "0.2.1-SNAPSHOT"

scalaVersion := "2.11.8"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

libraryDependencies ++= {
  val akkaV = "2.5.2"
  val akkaHttpV = "10.0.7"
  val reactivemongoV = "0.12.3"
  Seq(
    "com.typesafe.akka" %% "akka-actor"  % akkaV,
    // "com.typesafe.akka" %% "akka-remote" % akkaV,
    "com.typesafe.akka" %% "akka-cluster" % akkaV,
    "com.typesafe.akka" %% "akka-cluster-tools" % akkaV,
    "com.typesafe.akka" %% "akka-testkit" % akkaV % Test,
    "com.typesafe.akka" %% "akka-stream" % akkaV,
    "com.typesafe.akka" %% "akka-stream-testkit" % akkaV % Test,
    // "com.typesafe.akka" %% "akka-http-core" % akkaHttpV,
    "com.typesafe.akka" %% "akka-http" % akkaHttpV,
    "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpV % Test,
//     "org.scalactic" %% "scalactic" % "3.0.1",
//     "org.scalatest" %% "scalatest" % "3.0.1" % "test",
    "com.typesafe.play" %% "play-json" % "2.5.15",
    "org.slf4j" % "slf4j-simple" % "1.7.25",
    "com.sksamuel.scrimage" %% "scrimage-core" % "2.1.8",
    "com.sksamuel.scrimage" %% "scrimage-io-extra" % "2.1.8",
    "com.esotericsoftware" % "kryo" % "4.0.0",
    "com.github.romix.akka" %% "akka-kryo-serialization" % "0.5.0",
    "commons-cli" % "commons-cli" % "1.4",
    "io.jsonwebtoken" % "jjwt" % "0.7.0",
    "org.reactivemongo" %% "reactivemongo" % reactivemongoV,
    "org.reactivemongo" %% "reactivemongo-play-json" % reactivemongoV
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
