package com.cookeem.chat.websocket

import java.io.{ByteArrayInputStream, File}
import java.text.SimpleDateFormat
import java.util.UUID

import akka.NotUsed
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.stream._
import akka.stream.scaladsl._
import com.cookeem.chat.common.CommonUtils._
import com.cookeem.chat.event._
import com.sksamuel.scrimage.Image
import com.sksamuel.scrimage.nio.PngWriter
import org.apache.commons.io.FileUtils

import scala.concurrent.duration._

/**
  * Created by cookeem on 16/9/25.
  */
class ChatSession(uid: String, nickname: String, avatar: String, sessionid: String)(implicit actorSystem: ActorSystem, materializer: ActorMaterializer) {

  val chatSessionActor = actorSystem.actorOf(Props(classOf[ChatSessionActor], uid, nickname, avatar, sessionid))
  consoleLog("INFO", s"create new chatSessionActor: $chatSessionActor")

  val source: Source[WsMessageDown, ActorRef] = Source.actorRef[WsMessageDown](bufferSize = Int.MaxValue, OverflowStrategy.fail)

  /* FlowShape
  {{
         +--------------------------------------------------------------+
  In  ~> | ~> fromFilterSuccess ~> +-------------+                      |
         |                         |chatActorSink| ....                 |
         |        actorAsSource ~> +-------------+    . chatRoomActor   |
         |                                            . to broadcast    |
  Out <~ | <~ backToFlow  <~ chatSource................                 |
         +--------------------------------------------------------------+
  }}
    actorAsSource means when stream create send UserOnline
    chatActorSink means when stream close send UserOffline
    chatSource receive message from chatRoomActor
  */

  def chatService = Flow.fromGraph(GraphDSL.create(source) { implicit builder =>
    chatSource =>
      import GraphDSL.Implicits._
      val fromFilterSuccess: FlowShape[WsMessageUp, WsMessageDown] = builder.add(
        Flow[WsMessageUp].collect {
          case WsTextUp(suid, snickname, savatar, ssessionid, smsgType, scontent) =>
            WsTextDown(suid, snickname, savatar, ssessionid, smsgType, scontent)
          case WsBinaryUp(suid, snickname, savatar, ssessionid, smsgType, sbs, sfileName, sfileSize, sfileType) =>
            val path1 = new SimpleDateFormat("yyyyMM").format(System.currentTimeMillis())
            val path2 = new SimpleDateFormat("dd").format(System.currentTimeMillis())
            val path = s"upload/$path1/$path2"
            val dir = new File(path)
            if (!dir.exists()) {
              dir.mkdirs()
            }
            var fileNameNew = UUID.randomUUID().toString
            if (sfileType == "image/jpeg" || sfileType == "image/gif" || sfileType == "image/png") {
              fileNameNew = s"$fileNameNew.${sfileType.replace("image/", "")}"
            }
            val filePath = s"$path/$fileNameNew"
            val bytes = sbs.toArray
            FileUtils.writeByteArrayToFile(new File(filePath), bytes)
            var fileThumb = ""
            try {
              if (sfileType == "image/jpeg" || sfileType == "image/gif" || sfileType == "image/png") {
                fileThumb = s"$filePath.thumb.png"
                //resize image
                implicit val writer = PngWriter.NoCompression
                val bytesImage = Image.fromStream(new ByteArrayInputStream(bytes)).bound(200, 200).bytes
                FileUtils.writeByteArrayToFile(new File(fileThumb), bytesImage)
              }
            } catch { case e: Throwable =>
              consoleLog("ERROR", s"chat upload image error: $e")
            }
            WsBinaryDown(suid, snickname, savatar, ssessionid, smsgType, filePath, sfileName, sfileSize, sfileType, fileThumb)
        }
      )
      val chatActorSink: Sink[WsMessageDown, NotUsed] = Sink.actorRef[WsMessageDown](chatSessionActor, UserOffline)
      val merge: UniformFanInShape[WsMessageDown, WsMessageDown] = builder.add(Merge[WsMessageDown](2))
      val actorAsSource = builder.materializedValue.map(actor => UserOnline(actor))
      val backToFlow: FlowShape[WsMessageDown, WsMessageDown] = builder.add(Flow[WsMessageDown])
      fromFilterSuccess ~> merge.in(0)
      actorAsSource ~> merge.in(1)
      merge ~> chatActorSink
      chatSource ~> backToFlow
      FlowShape(fromFilterSuccess.in, chatSource.out)
  }).keepAlive(45.seconds, () => WsTextDown("", "", "", "", "keepalive", ""))
  // websocket default timeout after 60 second, to prevent timeout send keepalive message
  // you can config akka.http.server.idle-timeout to set timeout duration

}
