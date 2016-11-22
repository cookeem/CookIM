package com.cookeem.chat.websocket

import akka.actor.ActorRef
import akka.cluster.pubsub.{DistributedPubSub, DistributedPubSubMediator}
import com.cookeem.chat.mongo.MongoLogic._
import com.cookeem.chat.event._

/**
  * Created by cookeem on 16/9/25.
  */
class PushSessionActor extends TraitPubSubActor {
  import DistributedPubSubMediator._
  val mediator = DistributedPubSub(context.system).mediator
  // ChatSession related chatSource
  var actorRef = ActorRef.noSender

  var uid = ""
  var sessionids = List[String]()

  def receive: Receive = eventReceive orElse {
    case SubscribeAck(Subscribe(suid, None, `self`)) if uid != "" =>

    case UnsubscribeAck(Unsubscribe(ssessionid, None, `self`)) =>
      actorRef = ActorRef.noSender

    case UserOnline(ref) =>
      actorRef = ref

    case UserOffline if uid != "" =>
      sessionids.foreach { sessionid =>
        mediator ! Unsubscribe(sessionid, self)
      }

    //user request push service, then subscribe user joined sessions
    case WsTextDown(suid, snickname, savatar, ssessionid, ssessionname, ssessionicon, msgType, content, dateline) if suid != "" && snickname != "" && savatar != "" && msgType == "push" =>
      getUserInfo(suid).foreach { user =>
        if (user != null) {
          uid = user._id
          sessionids.foreach { sessionid =>
            mediator ! Unsubscribe(sessionid, self)
          }
          sessionids = user.sessionsstatus.map(_.sessionid)
          sessionids.foreach { sessionid =>
            mediator ! Subscribe(sessionid, self)
          }
        }
      }

    case ClusterText(suid, snickname, savatar, ssessionid, ssessionname, ssessionicon, msgType, content, dateline) if actorRef != ActorRef.noSender =>
      actorRef ! WsTextDown(suid, snickname, savatar, ssessionid, ssessionname, ssessionicon, msgType, content, dateline)

    case ClusterBinary(suid, snickname, savatar, ssessionid, ssessionname, ssessionicon, msgType, filePath, fileName, fileSize, fileType, fileThumb, dateline) if actorRef != ActorRef.noSender =>
      actorRef ! WsBinaryDown(suid, snickname, savatar, ssessionid, ssessionname, ssessionicon, msgType, filePath, fileName, fileSize, fileType, fileThumb, dateline)
  }
}
