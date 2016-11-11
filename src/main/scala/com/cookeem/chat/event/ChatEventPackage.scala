package com.cookeem.chat.event

import akka.actor.ActorRef
import akka.util.ByteString

/**
  * Created by cookeem on 16/11/2.
  */
sealed trait ChatEvent
case class UserOnline(actor: ActorRef) extends ChatEvent
case object UserOffline extends ChatEvent

case class UserText(uid: String, nickname: String, avatar: String, message: String) extends ChatEvent
case class UserBinary(uid: String, nickname: String, avatar: String, filepath: String, filename: String, filesize: Long, filetype: String) extends ChatEvent
case class ClusterText(uid: String, nickname: String, avatar: String, message: String) extends ChatEvent
case class ClusterBinary(uid: String, nickname: String, avatar: String, filepath: String, filename: String, filesize: Long, filetype: String) extends ChatEvent

case class UserToken(uid: String, nickname: String, avatar: String)
case class SessionToken(sessionid: String)
case class UserSessionInfo(uid: String, nickname: String, avatar: String, sessionid: String)

