package com.cookeem.chat.websocket

import akka.actor.ActorRef
import akka.cluster.pubsub.{DistributedPubSub, DistributedPubSubMediator}
import com.cookeem.chat.common.CommonUtils._
import com.cookeem.chat.event._

/**
  * Created by cookeem on 16/9/25.
  */
class ChatSessionActor(uid: String, nickname: String, avatar: String, sessionid: String) extends TraitPubSubActor {
  import DistributedPubSubMediator._
  // 为集群创建订阅发布中间件
  val mediator = DistributedPubSub(context.system).mediator
  // ChatSessionActor对应的来源actor
  var actorRef = ActorRef.noSender

  def receive: Receive = eventReceive orElse {
    //表示成功订阅
    case SubscribeAck(Subscribe(session, None, `self`)) =>
      mediator ! Publish(sessionid, ClusterText("", "System", "", s"User $nickname($uid) joined session $sessionid..."))
      consoleLog("SUCCESSFUL", s"$self $uid subscribe $session")
    //表示成功取消订阅
    case UnsubscribeAck(Unsubscribe(session, None, `self`)) =>
      actorRef = ActorRef.noSender
      mediator ! Publish(sessionid, ClusterText("", "System", "", s"User $nickname($uid) left session $sessionid..."))
      consoleLog("SUCCESSFUL", s"$self $uid unsubscribe $session")
    case UserOnline(ref) =>
      // 订阅roomId的消息内容, 设置对应的来源actorRef
      actorRef = ref
      mediator ! Subscribe(sessionid, self)
    case UserOffline =>
      // 取消订阅roomId的消息内容
      mediator ! Unsubscribe(sessionid, self)
    case UserText(suid, snickname, savatar, msg) =>
      // 当接收到来自websocket的消息，向roomId的订阅者发布消息
      mediator ! Publish(sessionid, ClusterText(suid, snickname, savatar, msg))
    case UserBinary(suid, snickname, savatar, sfilepath, sfilename, sfilesize, sfiletype) =>
      // 当接收到来自websocket的消息，向roomId的订阅者发布消息
      mediator ! Publish(sessionid, ClusterBinary(suid, snickname, savatar, sfilepath, sfilename, sfilesize, sfiletype))
    case ClusterText(suid, snickname, savatar, msg) if actorRef != ActorRef.noSender =>
      // 向websocket发送消息
      actorRef ! UserText(suid, snickname, savatar, msg)
    case ClusterBinary(suid, snickname, savatar, sfilepath, sfilename, sfilesize, sfiletype) if actorRef != ActorRef.noSender =>
      // 向websocket发送消息
      actorRef ! UserBinary(suid, snickname, savatar, sfilepath, sfilename, sfilesize, sfiletype)
  }
}
