package com.cookeem.chat.websocket

import akka.actor.ActorRef
import akka.cluster.pubsub.{DistributedPubSub, DistributedPubSubMediator}
import com.cookeem.chat.common.CommonUtils._
import com.cookeem.chat.event._
import com.cookeem.chat.mongo.MongoLogic._

/**
  * Created by cookeem on 16/9/25.
  */
class ChatSessionActor extends TraitPubSubActor {
  val system = context.system
  import system.dispatcher

  import DistributedPubSubMediator._
  val mediator = DistributedPubSub(context.system).mediator

  //actorRef is stream's actorRef
  var actorRef = ActorRef.noSender

  //chat session actor related info
  var uid = ""
  var nickname = ""
  var avatar = ""
  var sessionid = ""
  var sessionName = ""
  var sessionIcon = ""

  def receive: Receive = eventReceive orElse {
    case SubscribeAck(Subscribe(ssessionid, None, `self`)) if sessionid != "" =>
      //publish user join session
      mediator ! Publish(sessionid, ClusterText(uid, nickname, avatar, sessionid, sessionName, sessionIcon, "online", s"User $nickname online session"))
      userOnlineOffline(uid, sessionid, isOnline = true)
      createMessage(uid, sessionid, "online", s"User $nickname online session")
      consoleLog("SUCCESSFUL", s"User $nickname online session $sessionid")

    case UnsubscribeAck(Unsubscribe(ssessionid, None, `self`)) =>
      //publish user left session
      actorRef = ActorRef.noSender
      mediator ! Publish(sessionid, ClusterText(uid, nickname, avatar, sessionid, sessionName, sessionIcon, "offline", s"User $nickname offline session"))
      userOnlineOffline(uid, sessionid, isOnline = false)
      createMessage(uid, sessionid, "offline", s"User $nickname offline session")
      consoleLog("SUCCESSFUL", s"User $nickname offline session $sessionid")

    case UserOnline(ref) =>
      //when websocket stream create it will send UserOnline to akka cluster
      //update the actorRef to websocket stream actor reference
      actorRef = ref

    case UserOffline if sessionid != "" =>
      //when websocket stream close it will send UserOffline to akka cluster
      //unsubscribe current session
      mediator ! Unsubscribe(sessionid, self)

    case WsTextDown(suid, snickname, savatar, ssessionid, ssessionName, ssessionIcon, msgType, content, dateline) if ssessionid != "" =>
      if (msgType == "online") {
        //user online a session
        uid = suid
        nickname = snickname
        avatar = savatar
        sessionid = ssessionid
        sessionName = ssessionName
        sessionIcon = ssessionIcon
        mediator ! Subscribe(sessionid, self)
      } else {
        //user send text message
        uid = suid
        nickname = snickname
        avatar = savatar
        sessionid = ssessionid
        sessionName = ssessionName
        sessionIcon = ssessionIcon
        mediator ! Publish(sessionid, ClusterText(uid, nickname, avatar, sessionid, sessionName, sessionIcon, msgType, content, dateline))
        createMessage(uid, sessionid, msgType, content = content)
      }

    case WsBinaryDown(suid, snickname, savatar, ssessionid, ssessionName, ssessionIcon, msgType, filePath, fileName, fileSize, fileType, fileThumb, dateline) if ssessionid != "" =>
      //user send binary message
      uid = suid
      nickname = snickname
      avatar = savatar
      sessionid = ssessionid
      sessionName = ssessionName
      sessionIcon = ssessionIcon
      mediator ! Publish(sessionid, ClusterBinary(uid, nickname, avatar, sessionid, sessionName, sessionIcon, msgType, filePath, fileName, fileSize, fileType, fileThumb, dateline))
      createMessage(uid, sessionid, msgType, filePath = filePath, fileName = fileName, fileSize = fileSize, fileType = fileType, fileThumb = fileThumb)

    case ClusterText(suid, snickname, savatar, ssessionid, ssessionName, ssessionIcon, msgType, content, dateline) if actorRef != ActorRef.noSender =>
      //when receive cluster push message
      //send back to websocket stream
      getSessionNameIcon(suid, ssessionid).map { sessionToken =>
        actorRef ! WsTextDown(suid, snickname, savatar, sessionToken.sessionid, sessionToken.sessionName, sessionToken.sessionIcon, msgType, content, dateline)
      }

    case ClusterBinary(suid, snickname, savatar, ssessionid, ssessionName, ssessionIcon, msgType, filePath, fileName, fileSize, fileType, fileThumb, dateline) if actorRef != ActorRef.noSender =>
      //when receive cluster push message
      //send back to websocket stream
      getSessionNameIcon(suid, ssessionid).map { sessionToken =>
        actorRef ! WsBinaryDown(suid, snickname, savatar, sessionToken.sessionid, sessionToken.sessionName, sessionToken.sessionIcon, msgType, filePath, fileName, fileSize, fileType, fileThumb, dateline)
      }

  }
}
