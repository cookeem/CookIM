package com.cookeem.chat.websocket

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.model.ws.TextMessage.Strict
import akka.http.scaladsl.model.ws._
import akka.stream._
import akka.stream.scaladsl._
import akka.util.ByteString
import com.cookeem.chat.common.CommonUtils._
import com.cookeem.chat.event._
import com.cookeem.chat.mongo.MongoLogic._
import com.cookeem.chat.mongo.UserSessionInfo
import com.cookeem.chat.restful.Controller._
import play.api.libs.json.Json

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by cookeem on 16/11/9.
  */
object ChatSessionGraph {
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

  def fromWs()(implicit ec: ExecutionContext, materializer: ActorMaterializer): Flow[Message, WsMessageUp, NotUsed] = Flow[Message].collect {
    case tm: TextMessage =>
      tm.textStream.runFold("")(_ + _).map { jsonStr =>
        var userTokenStr = ""
        var sessionTokenStr = ""
        var msgType = ""
        var content = ""
        try {
          val json = Json.parse(jsonStr)
          userTokenStr = getJsonString(json, "userToken", "")
          sessionTokenStr = getJsonString(json, "sessionToken", "")
          msgType = getJsonString(json, "msgType", "")
          content = getJsonString(json, "content", "")
        } catch { case e: Throwable =>
          consoleLog("ERROR", s"parse websocket text message error: $e")
        }
        val UserSessionInfo(uid, nickname, avatar, sessionid) = verifyUserSessionToken(userTokenStr, sessionTokenStr)
        WsTextUp(uid, nickname, avatar, sessionid, msgType, content)
      }
    case bm: BinaryMessage =>
      bm.dataStream.runFold(ByteString.empty)(_ ++ _).map { bs =>
        val splitor = "<#BinaryInfo#>"
        val (bsJson, bsBin) = bs.splitAt(bs.indexOfSlice(splitor))
        val jsonStr = bsJson.utf8String
        val bsFile = bsBin.drop(splitor.length)
        var userTokenStr = ""
        var sessionTokenStr = ""
        var msgType = ""
        var fileName = ""
        var fileSize = 0L
        var fileType = ""
        try {
          val json = Json.parse(jsonStr)
          userTokenStr = getJsonString(json, "userToken", "")
          sessionTokenStr = getJsonString(json, "sessionToken", "")
          msgType = getJsonString(json, "msgType", "")
          fileName = getJsonString(json, "fileName", "")
          fileSize = getJsonLong(json, "fileSize", 0L)
          fileType = getJsonString(json, "fileType", "")
        } catch { case e: Throwable =>
          consoleLog("ERROR", s"parse websocket binary message error: $e")
        }
        val UserSessionInfo(uid, nickname, avatar, sessionid) = verifyUserSessionToken(userTokenStr, sessionTokenStr)
        WsBinaryUp(uid, nickname, avatar, sessionid, msgType, bsFile, fileName, fileSize, fileType)
      }
  }.buffer(1024 * 1024, OverflowStrategy.fail).mapAsync(6)(t => t)

  /*
   *
   *  fromWs ~> flowFromWs ~> broadcast ~> filterFailure(drop failure here)  ~> flowUpToDownFailure ~> merge ~> backToWs
   *
   *                          broadcast ~> filterSuccess(create ChatSession) ~> flowUpToDownSuccess ~> merge
   *
   */

  implicit val wsTextDownWrites = Json.writes[WsTextDown]
  implicit val wsBinaryDownWrites = Json.writes[WsBinaryDown]

  def chatGraph()(implicit ec: ExecutionContext, actorSystem: ActorSystem, materializer: ActorMaterializer): Flow[Message, Strict, NotUsed] = Flow.fromGraph(GraphDSL.create() { implicit builder =>
    import GraphDSL.Implicits._

    //todo chatSession must create before use
    //every connection create one ChatSession
    var chatSession: ChatSession = null

    val flowFromWs: FlowShape[Message, WsMessageUp] = builder.add(fromWs())
    val broadcast: UniformFanOutShape[WsMessageUp, WsMessageUp] = builder.add(Broadcast[WsMessageUp](2))

    val filterFailure: FlowShape[WsMessageUp, WsMessageUp] = builder.add(Flow[WsMessageUp].filter(_.uid == ""))
    val flowUpToDownFailure: FlowShape[WsMessageUp, WsTextDown] = builder.add(
      Flow[WsMessageUp].map(_ => WsTextDown("", "", "", "", "reject", "no privilege to send message"))
    )

    val filterSuccess: FlowShape[WsMessageUp, WsMessageUp] = builder.add(
      Flow[WsMessageUp].filter(_.uid != "").collect {
        case WsTextUp(uid, nickname, avatar, sessionid, msgType, content) =>
          // if chatSession not create then create it, only create for once
          if (chatSession == null) {
            chatSession = new ChatSession(uid, nickname, avatar, sessionid)
          }
          WsTextUp(uid, nickname, avatar, sessionid, msgType, content)
        case WsBinaryUp(uid, nickname, avatar, sessionid, msgType, bs, fileName, fileSize, fileType) =>
          // if chatSession not create then create it, only create for once
          if (chatSession == null) {
            chatSession = new ChatSession(uid, nickname, avatar, sessionid)
          }
          WsBinaryUp(uid, nickname, avatar, sessionid, msgType, bs, fileName, fileSize, fileType)
      }
    )
    val flowUpToDownSuccess: FlowShape[WsMessageUp, WsMessageDown] = builder.add({
      chatSession.chatService
    })

    val merge: UniformFanInShape[WsMessageDown, WsMessageDown] = builder.add(Merge[WsMessageDown](2))
    val backToWs: FlowShape[WsMessageDown, Strict] = builder.add(
      Flow[WsMessageDown].collect {
        case wsTextDown: WsTextDown =>
          TextMessage(Json.stringify(Json.toJson(wsTextDown)))
        case wsBinaryDown: WsBinaryDown =>
          TextMessage(Json.stringify(Json.toJson(wsBinaryDown)))
      }
    )
    flowFromWs ~> broadcast
    broadcast ~> filterFailure ~> flowUpToDownFailure ~> merge ~> backToWs
    broadcast ~> filterSuccess ~> flowUpToDownSuccess ~> merge

    FlowShape(flowFromWs.in, backToWs.out)
  })

}
