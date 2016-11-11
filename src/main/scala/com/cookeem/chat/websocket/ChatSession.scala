package com.cookeem.chat.websocket

import java.io.{ByteArrayInputStream, File}
import java.text.SimpleDateFormat
import java.util.UUID

import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.model.ws._
import akka.stream._
import akka.stream.scaladsl._
import akka.util.ByteString
import com.cookeem.chat.common.CommonUtils._
import com.cookeem.chat.event._
import com.sksamuel.scrimage.Image
import com.sksamuel.scrimage.nio.PngWriter
import org.apache.commons.io.FileUtils
import play.api.libs.json.Json

import scala.concurrent.duration._

/**
  * Created by cookeem on 16/9/25.
  */
class ChatSession(uid: String, nickname: String, avatar: String, sessionid: String)(implicit actorSystem: ActorSystem, materializer: ActorMaterializer) {
  import actorSystem.dispatcher

  val chatSessionActor = actorSystem.actorOf(Props(classOf[ChatSessionActor], uid, nickname, avatar, sessionid))
  consoleLog("INFO", s"create new chatSessionActor: $chatSessionActor")

  val source = Source.actorRef[ChatEvent](bufferSize = Int.MaxValue, OverflowStrategy.fail)

  /* FlowShape
  {{
         +------------------------------------------------------------+
  In  ~> | ~> fromWebsocket ~> +-------------+                        |
         |                     |chatActorSink| ....                   |
         |    actorAsSource ~> +-------------+    . chatRoomActor     |
         |                                        . to broadcast      |
  Out <~ | <~ backToWebsocket <~ chatSource <......                   |
         +------------------------------------------------------------+
  }}
    actorAsSource表示有新的用户加入 UserJoined
    chatActorSink在用户退出的时候发送 UserLeft
    chatSource用于接收消息，并向websocket回送相关消息
  */

  def chatService(uid: String, nickname: String, avatar: String) = Flow.fromGraph(GraphDSL.create(source) { implicit builder =>
    chatSource => //把source作为参数传入Graph
      import GraphDSL.Implicits._
      //从websocket接收Message，并转化成ChatMessage

      val fromWebsocket = builder.add(
        Flow[Message].collect {
          case tm: TextMessage =>
            tm.textStream.runFold("")(_ + _).map(msg => UserText(uid, nickname, avatar, msg))
          case bm: BinaryMessage =>
            bm.dataStream.runFold(ByteString.empty)(_ ++ _).map { bs =>
              val splitor = "<#fileInfo#>"
              val (bsFileInfo, bsFileString) = bs.splitAt(bs.indexOfSlice(splitor))
              val strFileInfo = bsFileInfo.filter(byte => byte != 0).utf8String
              val jsonFileInfo = Json.parse(strFileInfo)
              val filename = getJsonString(jsonFileInfo, "filename")
              val filesize = getJsonLong(jsonFileInfo, "filesize")
              val filetype = getJsonString(jsonFileInfo, "filetype")
              val path1 = new SimpleDateFormat("yyyyMM").format(System.currentTimeMillis())
              val path2 = new SimpleDateFormat("dd").format(System.currentTimeMillis())
              val path = s"upload/$path1/$path2"
              val dir = new File(path)
              if (!dir.exists()) {
                dir.mkdirs()
              }
              val filenameNew = UUID.randomUUID().toString
              val filepath = s"$path/$filenameNew"
              val bsFile = bsFileString.drop(splitor.length)
              val bytes = bsFile.toArray
              FileUtils.writeByteArrayToFile(new File(filepath), bytes)
              //对图像进行压缩处理
              implicit val writer = PngWriter.NoCompression
              val bytesImage = Image.fromStream(new ByteArrayInputStream(bytes)).bound(200, 200).bytes
              FileUtils.writeByteArrayToFile(new File(s"$filepath.thumb.png"), bytesImage)
              UserBinary(uid, nickname, avatar, filepath, filename, filesize, filetype)
            }
        }.buffer(1024 * 1024, OverflowStrategy.fail).mapAsync(6)(t => t)
      )
      //把ChatMessage转化成Message，并输出到websocket
      val backToWebsocket = builder.add(
        Flow[ChatEvent].collect {
          case UserText(suid, snickname, savatar, text) => TextMessage(s"$snickname($suid) $savatar: $text")
          case UserBinary(suid, snickname, savatar, sfilepath, sfilename, sfilesize, sfiletype) => TextMessage(s"$snickname($suid) $savatar: $sfilepath, $sfilename, $sfilesize, $sfiletype")
        }
      )
      //把消息发送到chatRoomActor，chatRoomActor收到消息后会进行广播, 假如流结束的时候，向chatRoomActor发送UserLeft消息
      val chatActorSink = Sink.actorRef[ChatEvent](chatSessionActor, UserOffline)
      //聚合管道
      val merge = builder.add(Merge[ChatEvent](2))
      //进行流的物料化，当有新的流创建的时候，向该流中发送UserJoined(actor)消息
      val actorAsSource = builder.materializedValue.map(actor => UserOnline(actor))
      //聚合websocket的消息来源
      fromWebsocket ~> merge.in(0)
      //当有新的流创建的是否，发送UserJoined(user, actor)消息到聚合merge
      actorAsSource ~> merge.in(1)
      //把聚合merge消息发送到chatRoomActor，注意chatActorSink会广播消息到各个chatroom的chatSource
      merge ~> chatActorSink
      //chatSource收到广播消息之后，把消息发送给backToWebsocket
      chatSource ~> backToWebsocket
      //暴露端口：fromWebsocket.in, backToWebsocket.out
      FlowShape(fromWebsocket.in, backToWebsocket.out)
  }).keepAlive(45.seconds, () => TextMessage("keepalive"))
  //websocket默认会在60秒后超时,自动发送keepAlive消息,可以配置akka.http.server.idle-timeout超时时间

}
