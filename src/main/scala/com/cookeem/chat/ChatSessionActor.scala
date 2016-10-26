package com.cookeem.chat

import akka.actor.ActorRef
import akka.cluster.pubsub.{DistributedPubSub, DistributedPubSubMediator}
import com.cookeem.chat.ChatMessageType._
import com.cookeem.chat.common.CommonUtils._

/**
  * Created by cookeem on 16/9/25.
  */
class ChatSessionActor(username: String, roomId: String) extends TraitPubSubActor {
  import DistributedPubSubMediator._
  // 为集群创建订阅发布中间件
  val mediator = DistributedPubSub(context.system).mediator
  // ChatSessionActor对应的来源actor
  var actorRef = ActorRef.noSender

  def receive: Receive = eventReceive orElse {
    //表示成功订阅
    case SubscribeAck(Subscribe(room, None, `self`)) =>
      mediator ! Publish(roomId, ChatMessagePub("System", s"User $username joined room $roomId..."))
      consoleLog("SUCCESSFUL", s"$self $username subscribe $room")
    //表示成功取消订阅
    case UnsubscribeAck(Unsubscribe(room, None, `self`)) =>
      actorRef = ActorRef.noSender
      mediator ! Publish(roomId, ChatMessagePub("System", s"User $username left room $roomId..."))
      consoleLog("SUCCESSFUL", s"$self $username unsubscribe $room")
    case UserJoined(ref) =>
      // 订阅roomId的消息内容, 设置对应的来源actorRef
      actorRef = ref
      mediator ! Subscribe(roomId, self)
    case UserLeft =>
      // 取消订阅roomId的消息内容
      mediator ! Unsubscribe(roomId, self)
    case ChatMessage(user, msg) =>
      // 当接收到来自websocket的消息，向roomId的订阅者发布消息
      mediator ! Publish(roomId, ChatMessagePub(user, msg))
    case ChatBinary(user, bs) =>
      // 当接收到来自websocket的消息，向roomId的订阅者发布消息
      mediator ! Publish(roomId, ChatBinaryPub(user, bs))
    case ChatMessagePub(user, msg) if actorRef != ActorRef.noSender =>
      // 向websocket发送消息
      actorRef ! ChatMessage(user, msg)
    case ChatBinaryPub(user, bs) if actorRef != ActorRef.noSender =>
      // 向websocket发送消息
      actorRef ! ChatBinary(user, bs)
  }
}
