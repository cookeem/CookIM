package com.cookeem.chat

import java.net.InetAddress

import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.cookeem.chat.common.CommonUtils._
import com.cookeem.chat.restful.Route
import com.cookeem.chat.websocket.NotificationActor
import com.typesafe.config.ConfigFactory
import org.apache.commons.cli.{DefaultParser, HelpFormatter, Options, Option => CliOption}

/**
  * Created by cookeem on 16/9/25.
  */
object CookIM extends App {
  val options = new Options()
  options.addOption(
    CliOption
      .builder("n")
      .longOpt("nat")
      .desc("is nat network or in docker")
      .hasArg(false)
      .build()
  )
  options.addOption(
    CliOption
      .builder("h")
      .longOpt("host-name")
      .desc("current web service external host name")
      .hasArg()
      .required()
      .argName("HOST-NAME")
      .build()
  )
  options.addOption(
    CliOption
      .builder("w")
      .longOpt("web-port")
      .desc("web service port")
      .hasArg()
      .required()
      .argName("WEB-PORT")
      .build()
  )
  options.addOption(
    CliOption
      .builder("a")
      .longOpt("akka-port")
      .desc("akka cluster node port")
      .hasArg()
      .required()
      .argName("AKKA-PORT")
      .build()
  )
  options.addOption(
    CliOption
      .builder("s")
      .longOpt("seed-nodes")
      .desc("akka cluster seed nodes, seperate with comma, example: localhost:2551,localhost:2552")
      .hasArg()
      .required()
      .argName("SEED-NODES")
      .build()
  )
  try {
    val parser = new DefaultParser()
    val cmd = parser.parse(options, args)
    val nat = cmd.hasOption("n")
    val hostName = cmd.getOptionValue("h")
    val webPort = cmd.getOptionValue("w").toInt
    val akkaPort = cmd.getOptionValue("a").toInt
    val seedNodes = cmd.getOptionValue("s")
    if (!(webPort > 0 && akkaPort > 0)) {
      throw CustomException("web-port and akka-port should greater than 0")
    } else if (hostName == "" || seedNodes == "") {
      throw CustomException("host-name and seed-nodes should not be empty")
    } else {
      val seedNodesStr = seedNodes.split(",").map(s => s""" "akka.tcp://chat-cluster@$s" """).mkString(",")
      val inetAddress = InetAddress.getLocalHost
      var configCluster = config
        .withFallback(ConfigFactory.parseString(s"akka.cluster.seed-nodes=[$seedNodesStr]"))
      if (!nat) {
        configCluster = configCluster
          .withFallback(ConfigFactory.parseString(s"akka.remote.netty.tcp.hostname=$hostName"))
          .withFallback(ConfigFactory.parseString(s"akka.remote.netty.tcp.port=$akkaPort"))
      } else {
        //very important in docker nat!
        //must set akka.remote.netty.tcp.bind-hostname
        //notice! akka.remote.netty.tcp.bind-port must set to akkaPort!!
        val bindHostName = inetAddress.getHostName
        configCluster = configCluster
          .withFallback(ConfigFactory.parseString(s"akka.remote.netty.tcp.hostname=$hostName"))
          .withFallback(ConfigFactory.parseString(s"akka.remote.netty.tcp.port=0"))
          .withFallback(ConfigFactory.parseString(s"akka.remote.netty.tcp.bind-hostname=$bindHostName"))
          .withFallback(ConfigFactory.parseString(s"akka.remote.netty.tcp.bind-port=$akkaPort"))
      }
      implicit val system = ActorSystem("chat-cluster", configCluster)
      implicit val materializer = ActorMaterializer()
      import system.dispatcher
      implicit val notificationActor = system.actorOf(Props(classOf[NotificationActor]))
      Http().bindAndHandle(Route.logRoute, "0.0.0.0", webPort)
      consoleLog("INFO",s"CookIM server started! Access url: http://$hostName:$webPort/")
    }
  } catch {
    case e: Throwable =>
      val formatter = new HelpFormatter()
      consoleLog("ERROR", s"$e")
      formatter.printHelp("Start distributed chat cluster node.\n", options, true)
  }
}
