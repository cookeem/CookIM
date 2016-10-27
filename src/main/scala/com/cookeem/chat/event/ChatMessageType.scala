package com.cookeem.chat.event

import akka.actor.ActorRef
import akka.util.ByteString

/**
  * Created by cookeem on 16/9/25.
  */
sealed trait ChatEvent
case class UserJoined(actor: ActorRef) extends ChatEvent
case object UserLeft extends ChatEvent
case class ChatMessage(username: String, message: String) extends ChatEvent
case class ChatBinary(username: String, bs: ByteString) extends ChatEvent
case class ChatMessagePub(username: String, message: String) extends ChatEvent
case class ChatBinaryPub(username: String, bs: ByteString) extends ChatEvent

