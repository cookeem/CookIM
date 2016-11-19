package com.cookeem.chat.event

import akka.actor.ActorRef
import akka.util.ByteString

import com.cookeem.chat.common.CommonUtils._

/**
  * Created by cookeem on 16/11/2.
  */

sealed trait WsMessageUp {
  val uid: String
}
case class WsTextUp(uid: String, nickname: String, avatar: String, sessionid: String, msgType: String, content: String) extends WsMessageUp
case class WsBinaryUp(uid: String, nickname: String, avatar: String, sessionid: String, msgType: String, bs: ByteString, fileName: String, fileSize: Long, fileType: String) extends WsMessageUp

sealed trait WsMessageDown
case class WsTextDown(uid: String, nickname: String, avatar: String, sessionid: String, msgType: String, content: String, dateline: String = timeToStr(System.currentTimeMillis())) extends WsMessageDown
case class WsBinaryDown(uid: String, nickname: String, avatar: String, sessionid: String, msgType: String, filePath: String, fileName: String, fileSize: Long, fileType: String, fileThumb: String, dateline: String = timeToStr(System.currentTimeMillis())) extends WsMessageDown

case class ClusterText(uid: String, nickname: String, avatar: String, sessionid: String, msgType: String, content: String, dateline: String = timeToStr(System.currentTimeMillis())) extends WsMessageDown
case class ClusterBinary(uid: String, nickname: String, avatar: String, sessionid: String, msgType: String, filePath: String, fileName: String, fileSize: Long, fileType: String, fileThumb: String, dateline: String = timeToStr(System.currentTimeMillis())) extends WsMessageDown

case class UserOnline(actor: ActorRef) extends WsMessageDown
case object UserOffline extends WsMessageDown
