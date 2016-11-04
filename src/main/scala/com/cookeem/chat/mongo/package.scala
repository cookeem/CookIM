package com.cookeem.chat

import java.util.Date

/**
  * Created by cookeem on 16/11/1.
  */
package object mongo {

  trait BaseMongoObj { var _id: String }

  case class User(var _id: String, login: String, nickname: String, password: String, gender: Int, avatar: String, lastlogin: Long = 0, logincount: Int = 0, sessions: List[String] = List(), friends: List[String] = List(), dateline: Long = System.currentTimeMillis()) extends BaseMongoObj

  case class Session(var _id: String, senduid: String, sessiontype: Int, visabletype: Int, jointype: Int, name: String, uids: List[String] = List(), dateline: Long = System.currentTimeMillis()) extends BaseMongoObj

  case class FileInfo(var _id: String, filepath: String, filename: String, filetype: String, filemd5: String, size: Long, dateline: Long = System.currentTimeMillis()) extends BaseMongoObj

  case class Message(var _id: String, senduid: String, sessionid: String, msgtype: Int, content: String, fileInfo: FileInfo, dateline: Long = System.currentTimeMillis()) extends BaseMongoObj

  case class Inbox(var _id: String, recvuid: String, senduid: String, sessionid: String, msgtype: Int, content: String, fileInfo: FileInfo, dateline: Long = System.currentTimeMillis()) extends BaseMongoObj

  case class Online(var _id: String, uid: String, dateline: Date = new Date()) extends BaseMongoObj

  case class UpdateResult(n: Int, errmsg: String)
  
}
