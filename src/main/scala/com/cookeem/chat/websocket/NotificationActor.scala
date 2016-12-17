package com.cookeem.chat.websocket

import akka.cluster.pubsub.{DistributedPubSub, DistributedPubSubMediator}
import com.cookeem.chat.event._
import com.cookeem.chat.mongo.MongoLogic._

/**
  * Created by cookeem on 16/9/25.
  */
class NotificationActor extends TraitPubSubActor {
  import DistributedPubSubMediator._
  val mediator = DistributedPubSub(context.system).mediator

  def receive: Receive = eventReceive orElse {
    //push message to ChatSessionActor and PushSessionActor
    case WsTextDown(uid, nickname, avatar, sessionid, sessionName, sessionIcon, msgType, content, dateline) if uid != "" && nickname != "" && avatar != "" && sessionid != "" =>
      mediator ! Publish(sessionid, ClusterText(uid, nickname, avatar, sessionid, sessionName, sessionIcon, msgType, content, dateline))
      createMessage(uid, sessionid, msgType, content = content)
  }
}
