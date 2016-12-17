package com.cookeem.chat.websocket

import akka.actor.ActorRef
import akka.cluster.pubsub.{DistributedPubSub, DistributedPubSubMediator}
import com.cookeem.chat.mongo.MongoLogic._
import com.cookeem.chat.event._
import com.cookeem.chat.mongo.SessionToken

import scala.concurrent.Future

/**
  * Created by cookeem on 16/9/25.
  */
class PushSessionActor extends TraitPubSubActor {
  val system = context.system
  import system.dispatcher

  import DistributedPubSubMediator._
  val mediator = DistributedPubSub(context.system).mediator

  //actorRef is stream's actorRef
  var actorRef = ActorRef.noSender

  //chat session actor related info
  var uid = ""
  var futureSessionTokens = Future(List[SessionToken]())

  def receive: Receive = eventReceive orElse {
    case SubscribeAck(Subscribe(suid, None, `self`)) if uid != "" =>
    //no need to publish user join session, so leave empty here

    case UnsubscribeAck(Unsubscribe(ssessionid, None, `self`)) =>
      //when user left, send actorRef to noSender
      actorRef = ActorRef.noSender

    case UserOnline(ref) =>
      //when websocket stream create it will send UserOnline to akka cluster
      //update the actorRef to websocket stream actor reference
      actorRef = ref

    case UserOffline if uid != "" =>
      //when websocket stream close it will send UserOffline to akka cluster
      //unsubscribe all user joined sessions
      futureSessionTokens.map { sessionTokens =>
        sessionTokens.foreach { sessionToken =>
          mediator ! Unsubscribe(sessionToken.sessionid, self)
        }
      }

    //user request push service, then subscribe user joined sessions
    case WsTextDown(suid, snickname, savatar, ssessionid, ssessionName, ssessionIcon, msgType, content, dateline) if suid != "" && snickname != "" && savatar != "" && msgType == "push" =>
      //when user online
      //send accept back to websocket stream
      actorRef ! WsTextDown(suid, snickname, savatar, ssessionid, ssessionName, ssessionIcon, "accept", s"User $snickname subscribe all session push accepted", dateline)
      getUserInfo(suid).foreach { user =>
        if (user != null) {
          uid = user._id
          //unsubscribe all user joined sessions
          futureSessionTokens.map { sessionTokens =>
            sessionTokens.foreach { sessionToken =>
              mediator ! Unsubscribe(sessionToken.sessionid, self)
            }
          }
          futureSessionTokens = Future.sequence(
            user.sessionsStatus.map{ sessionstatus =>
              getSessionNameIcon(uid, sessionstatus.sessionid)
            }
          )
          //subscribe all user joined sessions
          futureSessionTokens.map { sessionTokens =>
            sessionTokens.foreach { sessionToken =>
              mediator ! Subscribe(sessionToken.sessionid, self)
            }
          }
        }
      }

    case ClusterText(suid, snickname, savatar, ssessionid, ssessionName, ssessionIcon, msgType, content, dateline) if actorRef != ActorRef.noSender =>
      //when receive cluster push message
      //send back to websocket stream
      if (msgType != "online" && msgType != "offline") {
        futureSessionTokens.foreach { sessionTokens =>
          sessionTokens.filter(_.sessionid == ssessionid).foreach { sessionToken =>
            actorRef ! WsTextDown(suid, snickname, savatar, sessionToken.sessionid, sessionToken.sessionName, sessionToken.sessionIcon, msgType, content, dateline)
          }
        }
      }

    case ClusterBinary(suid, snickname, savatar, ssessionid, ssessionName, ssessionIcon, msgType, fileName, fileType, fileid, thumbid, dateline) if actorRef != ActorRef.noSender =>
      //when receive cluster push message
      //send back to websocket stream
      futureSessionTokens.foreach { sessionTokens =>
        sessionTokens.filter(_.sessionid == ssessionid).foreach { sessionToken =>
          actorRef ! WsBinaryDown(suid, snickname, savatar, sessionToken.sessionid, sessionToken.sessionName, sessionToken.sessionIcon, msgType, fileName, fileType, fileid, thumbid, dateline)
        }
      }
  }
}
