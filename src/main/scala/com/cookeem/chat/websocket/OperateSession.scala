package com.cookeem.chat.websocket

import akka.NotUsed
import akka.http.scaladsl.model.ws._
import akka.stream.OverflowStrategy
import akka.stream.scaladsl._
import com.cookeem.chat.mongo.MongoLogic._

import scala.concurrent.Future
import scala.concurrent.duration._
/**
  * Created by cookeem on 16/11/9.
  */
object OperateSession {
  def rejectWebsocket(): Flow[Message, TextMessage, NotUsed] = {
    Flow[Message].mapConcat {
      case tm: TextMessage =>
        TextMessage(Source.single("Error ") ++ tm.textStream ++ Source.single("!")) :: Nil
      case bm: BinaryMessage =>
        TextMessage(Source.single("Error ") ++ Source.single("!")) :: Nil
    }
  }

  def createUserTokenWebsocket(uid: String): Flow[Message, TextMessage, NotUsed] = {
    Flow[Message].mapConcat {
      case tm: TextMessage =>
        TextMessage(Source.single("")) :: Nil
      case bm: BinaryMessage =>
        TextMessage(Source.single("")) :: Nil
    }
  }.keepAlive(15.seconds, () => TextMessage(Source.fromFuture[String](createUserToken(uid))))

  def createSessionTokenWebsocket(uid: String, sessionid: String): Flow[Message, TextMessage, NotUsed] = {
    Flow[Message].mapConcat {
      case tm: TextMessage =>
        TextMessage(Source.single("")) :: Nil
      case bm: BinaryMessage =>
        TextMessage(Source.single("")) :: Nil
    }
  }.keepAlive(15.seconds, () => TextMessage(Source.fromFuture[String](createSessionToken(uid, sessionid))))

}
