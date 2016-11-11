package com.cookeem.chat.websocket

import akka.actor.Actor
import akka.cluster.Cluster
import akka.cluster.ClusterEvent._
import com.cookeem.chat.common.CommonUtils._

/**
  * Created by cookeem on 16/9/25.
  */
trait TraitPubSubActor extends Actor {
  val cluster = Cluster(context.system)

  override def preStart(): Unit = {
    cluster.subscribe(self, initialStateMode = InitialStateAsEvents, classOf[MemberEvent], classOf[UnreachableMember], classOf[LeaderChanged])
  }

  override def postStop(): Unit = {
    cluster.unsubscribe(self)
    consoleLog("ERROR", s"*** ${context.system} context.system.terminate!!! ")
    context.system.terminate()
  }

  def eventReceive: Receive = {
    case MemberUp(member) =>
    //      println(s"*** Member is Up: ${self} ${member.address}")
    case UnreachableMember(member) =>
      cluster.down(member.address)
    //      println(s"*** Member Unreachable: ${self} ${member.address}")
    case MemberRemoved(member, previousStatus) =>
    //      println(s"*** Member is Removed: ${self} ${member.address} after $previousStatus")
    case MemberExited(member) =>
    //      println(s"*** Member is Exited: ${self} ${member.address}")
    case LeaderChanged(leader) =>
    //      println(s"*** Leader is Changed: ${self} ${leader}")
    case evt: MemberEvent => // ignore
    //      println(s"*** Memver event ${self} ${evt.member.status} ${evt.member.address}")
  }
}
