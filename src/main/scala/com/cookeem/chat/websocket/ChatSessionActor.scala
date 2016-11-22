package com.cookeem.chat.websocket

import akka.actor.ActorRef
import akka.cluster.pubsub.{DistributedPubSub, DistributedPubSubMediator}
import com.cookeem.chat.common.CommonUtils._
import com.cookeem.chat.event._

/**
  * Created by cookeem on 16/9/25.
  */
class ChatSessionActor extends TraitPubSubActor {
  import DistributedPubSubMediator._
  val mediator = DistributedPubSub(context.system).mediator
  // ChatSession related chatSource
  var actorRef = ActorRef.noSender

  var uid = ""
  var nickname = ""
  var avatar = ""
  var sessionid = ""
  var sessionname = ""
  var sessionicon = ""

  def receive: Receive = eventReceive orElse {
    case SubscribeAck(Subscribe(ssessionid, None, `self`)) if sessionid != "" =>
      mediator ! Publish(sessionid, ClusterText("", "", "", sessionid, sessionname, sessionicon, "system", s"User $nickname online session"))
      consoleLog("SUCCESSFUL", s"User $nickname online session $sessionid")

    case UnsubscribeAck(Unsubscribe(ssessionid, None, `self`)) =>
      actorRef = ActorRef.noSender
      mediator ! Publish(sessionid, ClusterText("", "", "", sessionid, sessionname, sessionicon, "system", s"User $nickname left session"))
      consoleLog("SUCCESSFUL", s"User $nickname left session $sessionid")

    case UserOnline(ref) =>
      actorRef = ref

    case UserOffline if sessionid != "" =>
      mediator ! Unsubscribe(sessionid, self)

    case WsTextDown(suid, snickname, savatar, ssessionid, ssessionname, ssessionicon, msgType, content, dateline) if ssessionid != "" =>
      if (msgType == "online") {
        uid = suid
        nickname = snickname
        avatar = savatar
        sessionid = ssessionid
        sessionname = ssessionname
        sessionicon = ssessionicon
        mediator ! Subscribe(sessionid, self)
      } else {
        uid = suid
        nickname = snickname
        avatar = savatar
        sessionid = ssessionid
        sessionname = ssessionname
        sessionicon = ssessionicon
        mediator ! Publish(sessionid, ClusterText(uid, nickname, avatar, sessionid, sessionname, sessionicon, msgType, content, dateline))
      }

    case WsBinaryDown(suid, snickname, savatar, ssessionid, ssessionname, ssessionicon, msgType, filePath, fileName, fileSize, fileType, fileThumb, dateline) if ssessionid != "" =>
      uid = suid
      nickname = snickname
      avatar = savatar
      sessionid = ssessionid
      sessionname = ssessionname
      sessionicon = ssessionicon
      mediator ! Publish(sessionid, ClusterBinary(uid, nickname, avatar, sessionid, sessionname, sessionicon, msgType, filePath, fileName, fileSize, fileType, fileThumb, dateline))

    case ClusterText(suid, snickname, savatar, ssessionid, ssessionname, ssessionicon, msgType, content, dateline) if actorRef != ActorRef.noSender =>
      actorRef ! WsTextDown(suid, snickname, savatar, ssessionid, ssessionname, ssessionicon, msgType, content, dateline)

    case ClusterBinary(suid, snickname, savatar, ssessionid, ssessionname, ssessionicon, msgType, filePath, fileName, fileSize, fileType, fileThumb, dateline) if actorRef != ActorRef.noSender =>
      actorRef ! WsBinaryDown(suid, snickname, savatar, ssessionid, ssessionname, ssessionicon, msgType, filePath, fileName, fileSize, fileType, fileThumb, dateline)
  }
}
