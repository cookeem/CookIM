package com.cookeem.chat

import java.io.{ByteArrayInputStream, File}
import java.nio.file.Paths
import java.security.MessageDigest

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

import scala.concurrent.duration._

/**
  * Created by cookeem on 16/9/25.
  */
class ChatSession(username: String, roomId: String)(implicit actorSystem: ActorSystem, materializer: ActorMaterializer) {
  import actorSystem.dispatcher

  val chatSessionActor = actorSystem.actorOf(Props(classOf[ChatSessionActor], username, roomId))
  consoleLog("INFO", s"create new chatSessionActor: $chatSessionActor")

  val source = Source.actorRef[ChatEvent](bufferSize = Int.MaxValue, OverflowStrategy.fail)

  /* FlowShape
  {{
         +------------------------------------------------------------+
  In  ~> | ~> fromWebsocket ~> +-------------+                        |
         |                     |chatActorSink| ....                   |
         |    actorAsSource ~> +-------------+    . chatRoomActor     |
         |                                        . to broadcast      |
  Out ~> | <~ backToWebsocket <~ chatSource <......                   |
         +------------------------------------------------------------+
  }}
    actorAsSource表示有新的用户加入 UserJoined
    chatActorSink在用户退出的时候发送 UserLeft
    chatSource用于接收消息，并向websocket回送相关消息
  */

  def chatService(user: String) = Flow.fromGraph(GraphDSL.create(source) { implicit builder =>
    chatSource => //把source作为参数传入Graph
      import GraphDSL.Implicits._
      //从websocket接收Message，并转化成ChatMessage

      val fromWebsocket = builder.add(
        Flow[Message].collect {
          case tm: TextMessage =>
            tm.textStream.runFold("")(_ + _).map(msg => ChatMessage(user, msg))
          case bm: BinaryMessage =>
            bm.dataStream.alsoTo(FileIO.toPath(Paths.get("test1.png"))).runFold(ByteString.empty)(_ ++ _).map { bs =>
              val bytes = bs.toArray
              FileUtils.writeByteArrayToFile(new File("src.png"), bytes)
              val strMd5 = md5(bytes)
              //对图像进行压缩处理
              implicit val writer = PngWriter.NoCompression
              val bytesImage = Image.fromStream(new ByteArrayInputStream(bytes)).bound(200, 200).bytes
              FileUtils.writeByteArrayToFile(new File("src.thumb.png"), bytesImage)
              ChatBinary(user, ByteString(bytesImage))
            }
        }.buffer(1024 * 1024, OverflowStrategy.fail).mapAsync(6)(t => t)
      )
      //把ChatMessage转化成Message，并输出到websocket
      val backToWebsocket = builder.add(
        Flow[ChatEvent].collect {
          case ChatMessage(author, text) => TextMessage(s"[$author]: $text")
          case ChatBinary(author, bs) => BinaryMessage(bs)
        }
      )
      //把消息发送到chatRoomActor，chatRoomActor收到消息后会进行广播, 假如流结束的时候，向chatRoomActor发送UserLeft消息
      val chatActorSink = Sink.actorRef[ChatEvent](chatSessionActor, UserLeft)
      //聚合管道
      val merge = builder.add(Merge[ChatEvent](2))
      //进行流的物料化，当有新的流创建的时候，向该流中发送UserJoined(user, actor)消息
      val actorAsSource = builder.materializedValue.map(actor => UserJoined(actor))
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
