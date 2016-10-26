package com.cookeem.chat

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.PathMatchers
import akka.stream.ActorMaterializer
import com.cookeem.chat.common.CommonUtils._
import com.typesafe.config.ConfigFactory
import org.apache.commons.cli.{DefaultParser, HelpFormatter, Options, Option => CliOption}

/**
  * Created by cookeem on 16/9/25.
  */
object DistributedWebChat extends App {
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

      val route = get {
        pathPrefix("ws-chat" / Segment) { chatId =>
          path(PathMatchers.Segment) { username =>
            val chatSession = new ChatSession(username, chatId)
            handleWebSocketMessages(chatSession.chatService(username))
          }
        } ~ pathSingleSlash {
          getFromFile("www/index.html")
        } ~ pathPrefix("") {
          getFromDirectory("www")
        }
      }

      Http().bindAndHandle(route, "0.0.0.0", httpPort)
      consoleLog("INFO",s"Websocket chat server started! Access url: http://localhost:$httpPort/?username=cookeem&chatid=room01")
    }
  } catch {
    case e: Throwable =>
      val formatter = new HelpFormatter()
      consoleLog("ERROR", s"${e.getClass}, ${e.getMessage}, ${e.getCause}")
      formatter.printHelp("Start distributed chat cluster node.\n", options, true)
  }
}
