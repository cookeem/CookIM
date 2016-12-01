package com.cookeem.chat.event

import akka.actor.ActorRef
import akka.util.ByteString
import com.cookeem.chat.common.CommonUtils._
import com.cookeem.chat.mongo.FileInfo

/**
  * Created by cookeem on 16/11/2.
  */

//akka stream message type
sealed trait WsMessageUp {
  val uid: String
}
case class WsTextUp(uid: String, nickname: String, avatar: String, sessionid: String, sessionName: String, sessionIcon: String, msgType: String, content: String) extends WsMessageUp
case class WsBinaryUp(uid: String, nickname: String, avatar: String, sessionid: String, sessionName: String, sessionIcon: String, msgType: String, bs: ByteString, fileName: String, fileSize: Long, fileType: String) extends WsMessageUp

//akka stream message type
sealed trait WsMessageDown
case class
WsTextDown(uid: String, nickname: String, avatar: String, sessionid: String, sessionName: String, sessionIcon: String, msgType: String, content: String, dateline: String = timeToStr(System.currentTimeMillis())) extends WsMessageDown
case class WsBinaryDown(uid: String, nickname: String, avatar: String, sessionid: String, sessionName: String, sessionIcon: String, msgType: String, filePath: String, fileName: String, fileSize: Long, fileType: String, fileThumb: String, dateline: String = timeToStr(System.currentTimeMillis())) extends WsMessageDown

//akka stream message type
case class UserOnline(actor: ActorRef) extends WsMessageDown
case object UserOffline extends WsMessageDown

//akka cluster message type
case class ClusterText(uid: String, nickname: String, avatar: String, sessionid: String, sessionName: String, sessionIcon: String, msgType: String, content: String, dateline: String = timeToStr(System.currentTimeMillis())) extends WsMessageDown
case class ClusterBinary(uid: String, nickname: String, avatar: String, sessionid: String, sessionName: String, sessionIcon: String, msgType: String, filePath: String, fileName: String, fileSize: Long, fileType: String, fileThumb: String, dateline: String = timeToStr(System.currentTimeMillis())) extends WsMessageDown

//client message type
case class ChatMessage(uid: String, nickname: String, avatar: String, msgType: String, content: String, fileInfo: FileInfo, dateline: String = timeToStr(System.currentTimeMillis())) extends WsMessageDown
case class PushMessage(uid: String, nickname: String, avatar: String, sessionid: String, sessionName: String, sessionIcon: String, msgType: String, content: String, fileInfo: FileInfo, dateline: String = timeToStr(System.currentTimeMillis())) extends WsMessageDown

