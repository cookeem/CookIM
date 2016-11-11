package com.cookeem.chat

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.cookeem.chat.common.CommonUtils._
import com.cookeem.chat.restful.Route
import com.typesafe.config.ConfigFactory
import org.apache.commons.cli.{DefaultParser, HelpFormatter, Options, Option => CliOption}

/**
  * Created by cookeem on 16/9/25.
  */
object CookIM extends App {
  val options = new Options()
  //单参数选项
  options.addOption(
    CliOption
      .builder("h")
      .longOpt("http-port")
      .desc("http service port")
      .hasArg()
      .required()
      .argName("HTTP-PORT")
      .build()
  )
  options.addOption(
    CliOption
      .builder("n")
      .longOpt("node-port")
      .desc("akka cluster node port")
      .hasArg()
      .required()
      .argName("NODE-PORT")
      .build()
  )
  try {
    val parser = new DefaultParser()
    val cmd = parser.parse(options, args)
    val httpPort = cmd.getOptionValue("h").toInt
    val nodePort = cmd.getOptionValue("n").toInt
    if (!(httpPort > 0 && nodePort > 0)) {
      throw CustomException("http-port and node-port should greater than 0")
    } else {
      val configCluster = config.withFallback(ConfigFactory.parseString(s"akka.remote.netty.tcp.port=$nodePort"))
      implicit val system = ActorSystem("chat-cluster", configCluster)
      implicit val materializer = ActorMaterializer()
      import system.dispatcher
      Http().bindAndHandle(Route.logRoute, "0.0.0.0", httpPort)
      consoleLog("INFO",s"Websocket chat server started! Access url: http://localhost:$httpPort/chat/websocket.html?username=cookeem&chatid=room01")
    }
  } catch {
    case e: Throwable =>
      val formatter = new HelpFormatter()
      consoleLog("ERROR", s"$e")
      formatter.printHelp("Start distributed chat cluster node.\n", options, true)
  }
}
