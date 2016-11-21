package com.cookeem.chat.websocket

import akka.NotUsed
import akka.http.scaladsl.model.ws._
import akka.stream._
import akka.stream.scaladsl._
import com.cookeem.chat.common.CommonUtils._
import com.cookeem.chat.restful.Controller._
import play.api.libs.json.Json

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by cookeem on 16/11/9.
  */
object TokenWebsocket {
  def createUserTokenWebsocket()(implicit ec: ExecutionContext, materializer: ActorMaterializer): Flow[Message, TextMessage, NotUsed] = {
    Flow[Message].collect {
      case tm: TextMessage =>
        tm.textStream.runFold("")(_ + _).map { msg =>
          var userTokenStr = ""
          try {
            val json = Json.parse(msg)
            json \ "message"
            userTokenStr = getJsonString(json, "userToken", "")
          } catch { case e: Throwable =>
            consoleLog("ERROR", s"parse websocket userToken message error: $e")
          }
          TextMessage(Source.fromFuture[String](createUserTokenCtl(userTokenStr).map { json => Json.stringify(json)}))
        }
      case bm: BinaryMessage =>
        val json = Json.obj(
          "errmsg" -> "error operation",
          "userToken" -> "",
          "uid" -> ""
        )
        Future(TextMessage(Source.single(Json.stringify(json))))
    }.buffer(1024 * 1024, OverflowStrategy.fail).mapAsync(6)(t => t)
  }

  def createSessionTokenWebsocket()(implicit ec: ExecutionContext, materializer: ActorMaterializer): Flow[Message, TextMessage, NotUsed] = {
    Flow[Message].collect {
      case tm: TextMessage =>
        tm.textStream.runFold("")(_ + _).map { msg =>
          var userTokenStr = ""
          var sessionid = ""
          try {
            val json = Json.parse(msg)
            userTokenStr = getJsonString(json, "userToken", "")
            sessionid = getJsonString(json, "sessionid", "")
          } catch { case e: Throwable =>
            consoleLog("ERROR", s"parse websocket sessionToken message error: $e")
          }
          TextMessage(Source.fromFuture[String](createSessionTokenCtl(userTokenStr, sessionid).map { json => Json.stringify(json)}))
        }
      case bm: BinaryMessage =>
        val json = Json.obj(
          "errmsg" -> "error operation",
          "sessionToken" -> ""
        )
        Future(TextMessage(Source.single(Json.stringify(json))))
    }.buffer(1024 * 1024, OverflowStrategy.fail).mapAsync(6)(t => t)
  }

}
