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
  val mediator = DistributedPubSub(context.system).mediator
  // ChatSession related chatSource
  var actorRef = ActorRef.noSender

  def receive: Receive = eventReceive orElse {
    case SubscribeAck(Subscribe(session, None, `self`)) =>
      mediator ! Publish(sessionid, ClusterText("", "", "", sessionid, "system", s"User $nickname join session"))
      consoleLog("SUCCESSFUL", s"$self $uid subscribe $session")

    case UnsubscribeAck(Unsubscribe(session, None, `self`)) =>
      actorRef = ActorRef.noSender
      mediator ! Publish(sessionid, ClusterText("", "", "", sessionid, "system", s"User $nickname left session"))
      consoleLog("SUCCESSFUL", s"$self $uid unsubscribe $session")

    case UserOnline(ref) =>
      actorRef = ref
      mediator ! Subscribe(sessionid, self)

    case UserOffline =>
      mediator ! Unsubscribe(sessionid, self)

    case WsTextDown(suid, snickname, savatar, ssessionid, smsgType, scontent, sdateline) =>
      mediator ! Publish(sessionid, ClusterText(suid, snickname, savatar, ssessionid, smsgType, scontent, sdateline))

    case WsBinaryDown(suid, snickname, savatar, ssessionid, smsgType, sfilePath, sfileName, sfileSize, sfileType, sfileThumb, sdateline) =>
      mediator ! Publish(sessionid, ClusterBinary(suid, snickname, savatar, ssessionid, smsgType, sfilePath, sfileName, sfileSize, sfileType, sfileThumb, sdateline))

    case ClusterText(suid, snickname, savatar, ssessionid, smsgType, scontent, sdateline) if actorRef != ActorRef.noSender =>
      actorRef ! WsTextDown(suid, snickname, savatar, ssessionid, smsgType, scontent, sdateline)

    case ClusterBinary(suid, snickname, savatar, ssessionid, smsgType, sfilePath, sfileName, sfileSize, sfileType, sfileThumb, sdateline) if actorRef != ActorRef.noSender =>
      actorRef ! WsBinaryDown(suid, snickname, savatar, ssessionid, smsgType, sfilePath, sfileName, sfileSize, sfileType, sfileThumb, sdateline)
  }
}
