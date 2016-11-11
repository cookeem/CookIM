package com.cookeem.chat

import java.util.Date

/**
  * Created by cookeem on 16/11/1.
  */
package object mongo {

  trait BaseMongoObj { var _id: String }

  case class User(var _id: String, login: String, nickname: String, password: String, gender: Int, avatar: String, lastlogin: Long = 0, logincount: Int = 0, sessionsstatus: List[SessionStatus] = List(), friends: List[String] = List(), dateline: Long = System.currentTimeMillis()) extends BaseMongoObj

  case class SessionStatus(sessionid: String, newcount: Int)

  case class Session(var _id: String, senduid: String, recvuid: String, sessiontype: Int, visabletype: Int, jointype: Int, name: String, usersstatus: List[UserStatus] = List(), lastmsgid: String = "", dateline: Long = System.currentTimeMillis()) extends BaseMongoObj

  case class UserStatus(uid: String, online: Boolean)

  case class Message(var _id: String, senduid: String, sessionid: String, msgtype: String, noticetype: String = "", message: String = "", fileinfo: FileInfo, dateline: Long = System.currentTimeMillis()) extends BaseMongoObj

  case class FileInfo(filepath: String, filename: String, size: Long, filetype: String, dateline: Long = System.currentTimeMillis())

  case class Online(var _id: String, uid: String, dateline: Date = new Date()) extends BaseMongoObj

  case class UpdateResult(n: Int, errmsg: String)

}
