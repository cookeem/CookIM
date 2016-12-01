package com.cookeem.chat

import java.util.Date

/**
  * Created by cookeem on 16/11/1.
  */
package object mongo {
  //mongoDB schema
  trait BaseMongoObj { var _id: String }
  case class User(var _id: String, login: String, nickname: String, password: String, gender: Int, avatar: String, lastLogin: Long = 0, loginCount: Int = 0, sessionsStatus: List[SessionStatus] = List(), friends: List[String] = List(), dateline: Long = System.currentTimeMillis()) extends BaseMongoObj
  case class SessionStatus(sessionid: String, newCount: Int)
  case class Session(var _id: String, createuid: String, ouid: String, var sessionName: String, var sessionIcon: String, sessionType: Int, publicType: Int, usersStatus: List[UserStatus] = List(), lastMsgid: String = "", lastUpdate: Long = System.currentTimeMillis(), dateline: Long = System.currentTimeMillis()) extends BaseMongoObj
  case class UserStatus(uid: String, online: Boolean)
  case class Message(var _id: String, uid: String, sessionid: String, msgType: String, content: String = "", fileInfo: FileInfo, dateline: Long = System.currentTimeMillis()) extends BaseMongoObj
  case class FileInfo(filePath: String = "", fileName: String = "", fileSize: Long = 0L, fileType: String = "", fileThumb: String = "")
  case class Online(var _id: String, uid: String, dateline: Date = new Date()) extends BaseMongoObj
  case class Notification(var _id: String, noticeType: String, senduid: String, recvuid: String, sessionid: String, isRead: Int = 0, dateline: Long = System.currentTimeMillis()) extends BaseMongoObj

  //mongoDB update result
  case class UpdateResult(n: Int, errmsg: String)

  //user and session token info
  case class UserToken(uid: String, nickname: String, avatar: String)
  case class SessionToken(sessionid: String, sessionName: String, sessionIcon: String)
  case class UserSessionInfo(uid: String, nickname: String, avatar: String, sessionid: String, sessionName: String, sessionIcon: String)

}
