package com.cookeem.chat.mongo

import com.cookeem.chat.common.CommonUtils._
import com.cookeem.chat.mongo.MongoOps._
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.bson._

import scala.concurrent.{ExecutionContextExecutor, Future}
/**
  * Created by cookeem on 16/10/28.
  */
object MongoLogic {
  case class ListBson(lb: List[BSONDocument])

  val usersCollection = cookimDB.map(_.collection[BSONCollection](colUsersName))
  val sessionsCollection = cookimDB.map(_.collection[BSONCollection](colSessionsName))
  val messagesCollection = cookimDB.map(_.collection[BSONCollection](colMessagesName))

  //create users collection and index
  def createUsersCollection()(implicit ec: ExecutionContextExecutor): Future[String] = {
    val indexSettings = Array(
      ("login", 1, true)
    )
    createIndex(colUsersName, indexSettings)
  }

  //create sessions collection and index
  def createSessionsCollection()(implicit ec: ExecutionContextExecutor): Future[String] = {
    val indexSettings = Array(
      ("creatoruid", 1, false)
    )
    createIndex(colSessionsName, indexSettings)
  }

  //create messages collection and index
  def createMessagesCollection()(implicit ec: ExecutionContextExecutor): Future[String] = {
    val indexSettings = Array(
      ("uid", 1, false),
      ("sessionid", 1, false)
    )
    createIndex(colMessagesName, indexSettings)
  }

  //register new user
  def registerUser(login: String, nickname: String, password: String, gender: Int, avatar: String): Future[(BSONValue, Boolean, String)] = {
    var isNew = false
    var errmsg = ""
    if (isEmail(login)) {
      errmsg = "login must be email"
    } else if (nickname.getBytes.length < 4) {
      errmsg = "nickname must at least 4 charactors"
    } else if (password.length < 6) {
      errmsg = "password must at least 6 charactors"
    } else if (!(gender == 0 || gender == 1 || gender == 2)) {
      errmsg = "gender must be 0 or 1 or 2"
    } else if (avatar.length < 6) {
      errmsg = "avatar must at least 6 charactors"
    }
    if (errmsg != "") {
      Future((BSONNull, isNew, errmsg))
    } else {
      for {
        uids <- findCollection(usersCollection, BSONDocument("login" -> login), 1)
        ret <- {
          if (uids.nonEmpty) {
            isNew = false
            Future((uids.head.get("_id").getOrElse(BSONNull), isNew, errmsg))
          } else {
            isNew = true
            val dateline = System.currentTimeMillis()
            val selector = BSONDocument("login" -> login)
            val doc = BSONDocument(
              "login" -> login,
              "nickname" -> nickname,
              "password" -> sha1(password),
              "dateline" -> dateline,
              "lastlogin" -> 0,
              "logincount" -> 0,
              "gender" -> gender,
              "avatar" -> avatar,
              "sessions" -> BSONArray(),
              "friends" -> BSONArray()
            )
            upsertCollection(usersCollection, selector, doc, upsert = true).map(f => (f, isNew, errmsg))
          }
        }
      } yield {
        ret
      }
    }
  }

  //update users info
  def updateUser(login: String, nickname: String = "", gender: Int = 0, avatar: String = ""): Future[UpdateResult] = {
    var errmsg = ""
    var update = BSONDocument()
    if (isEmail(login)) {
      errmsg = "login must be email"
    } else {
      var sets = BSONDocument()
      if (nickname.getBytes.length >= 4) {
        sets = sets.merge(BSONDocument("nickname" -> nickname))
      }
      if (gender == 0 || gender == 1 || gender == 2) {
        sets = sets.merge(BSONDocument("gender" -> gender))
      }
      if (avatar.length >= 6) {
        sets = sets.merge(BSONDocument("avatar" -> avatar))
      }
      if (sets == BSONDocument()) {
        errmsg = "nothing to update"
      } else {
        update = BSONDocument("$set" -> sets)
      }
    }
    if (errmsg != "") {
      Future(UpdateResult(ok = false, nModified = 0, errmsg = errmsg, ids = Seq[BSONValue]()))
    } else {
      updateCollection(usersCollection, BSONDocument("login" -> login), update, upsert = false, multi = false)
    }
  }

  //check and change password
  def changePwd(uid: BSONObjectID, oldPwd: String, newPwd: String): Future[UpdateResult] = {
    val selector = BSONDocument("_id" -> uid, "password" -> sha1(oldPwd))
    val update = BSONDocument(
      "$set" -> BSONDocument("password" -> sha1(newPwd))
    )
    updateCollection(usersCollection, selector, update, upsert = false, multi = false)
  }

  //when user login, update the logincount and lastlogin
  def loginUpdate(uid: BSONObjectID): Future[UpdateResult] = {
    val selector = BSONDocument("_id" -> uid)
    val update = BSONDocument(
      "$inc" -> BSONDocument("logincount" -> 1),
      "$set" -> BSONDocument("lastlogin" -> System.currentTimeMillis())
    )
    updateCollection(usersCollection, selector, update, upsert = false, multi = false)
  }

  //join new friend
  def joinFriend(uid: BSONObjectID, fuid: BSONObjectID): Future[UpdateResult] = {
    var errmsg = ""
    val selector = BSONDocument("_id" -> uid, "friends" -> BSONDocument("$ne" -> fuid))

    for {
      uids <- findCollection(usersCollection, selector, 1)
      uidExist <- {
        if (uids.isEmpty) {
          errmsg = "uid not exists or fuid already friends"
          Future(errmsg)
        } else {
          findCollection(usersCollection, BSONDocument("_id" -> fuid), 1)
        }
      }
      updateResult <- {
        var ret = Future(UpdateResult(ok = false, nModified = 0, errmsg = errmsg, ids = Seq[BSONValue]()))
        uidExist match {
          case errormsg: String =>
            ret = Future(UpdateResult(ok = false, nModified = 0, errmsg = errormsg, ids = Seq[BSONValue]()))
          case ListBson(fuids) =>
            if (fuids.isEmpty) {
              errmsg = "fuid not exists"
              ret = Future(UpdateResult(ok = false, nModified = 0, errmsg = errmsg, ids = Seq[BSONValue]()))
            } else {
              val update = BSONDocument("$push" -> BSONDocument("friends" -> fuid))
              ret = updateCollection(usersCollection, BSONDocument("_id" -> uid), update, upsert = false, multi = false)
            }
        }
        ret
      }
    } yield {
      updateResult
    }
  }

  //remove friend
  def removeFriend(uid: BSONObjectID, fuid: BSONObjectID): Future[UpdateResult] = {
    var errmsg = ""
    val selector = BSONDocument("_id" -> uid, "friends" -> BSONDocument("$eq" -> fuid))

    for {
      uids <- findCollection(usersCollection, selector, 1)
      ret <- {
        if (uids.isEmpty) {
          errmsg = "uid not exists or fuid not friends"
          Future(UpdateResult(ok = false, nModified = 0, errmsg = errmsg, ids = Seq[BSONValue]()))
        } else {
          val update = BSONDocument("$pull" -> BSONDocument("friends" -> fuid))
          updateCollection(usersCollection, BSONDocument("_id" -> uid), update, upsert = false, multi = false)
        }
      }
    } yield {
      ret
    }
  }

  //join new session
  def joinSession(uid: BSONObjectID, sessionid: BSONObjectID): Future[UpdateResult] = {
    var errmsg = ""
    val selector = BSONDocument("_id" -> uid, "sessions" -> BSONDocument("$ne" -> sessionid))

    for {
      uids <- findCollection(usersCollection, selector, 1)
      //check uid is exist or not
      uidExist <- {
        if (uids.isEmpty) {
          errmsg = "uid not exists or already join session"
          Future(errmsg)
        } else {
          findCollection(sessionsCollection, BSONDocument("_id" -> sessionid), 1)
        }
      }
      updateResult <- {
        var ret = Future(UpdateResult(ok = false, nModified = 0, errmsg = errmsg, ids = Seq[BSONValue]()))
        uidExist match {
          case errormsg: String =>
            ret = Future(UpdateResult(ok = false, nModified = 0, errmsg = errormsg, ids = Seq[BSONValue]()))
          case x: Array[BSONDocument] =>
            ret = Future(UpdateResult(ok = false, nModified = 0, errmsg = errmsg, ids = Seq[BSONValue]()))
          case ListBson(sessionids) =>
            if (sessionids.isEmpty) {
              errmsg = "session not exists"
              ret = Future(UpdateResult(ok = false, nModified = 0, errmsg = errmsg, ids = Seq[BSONValue]()))
            } else {
              val update = BSONDocument("$push" -> BSONDocument("sessions" -> sessionid))
              ret = updateCollection(usersCollection, BSONDocument("_id" -> uid), update, upsert = false, multi = false)
              val update2 = BSONDocument("$push" -> BSONDocument("uids" -> uid))
              ret = updateCollection(sessionsCollection, BSONDocument("_id" -> sessionid), update2, upsert = false, multi = false)
            }
        }
        ret
      }
    } yield {
      updateResult
    }
  }

  //leave session
  def leaveSession(uid: BSONObjectID, sessionid: BSONObjectID): Future[UpdateResult] = {
    val selector = BSONDocument("_id" -> uid, "sessions" -> BSONDocument("$eq" -> sessionid))

    for {
      uids <- findCollection(usersCollection, selector, 1)
      ret <- {
        if (uids.isEmpty) {
          val errmsg = "uid not exists or not join the session"
          Future(UpdateResult(ok = false, nModified = 0, errmsg = errmsg, ids = Seq[BSONValue]()))
        } else {
          val update = BSONDocument("$pull" -> BSONDocument("sessions" -> sessionid))
          updateCollection(usersCollection, BSONDocument("_id" -> uid), update, upsert = false, multi = false)
          val update2 = BSONDocument("$pull" -> BSONDocument("uids" -> uid))
          updateCollection(sessionsCollection, BSONDocument("_id" -> sessionid), update2, upsert = false, multi = false)
        }
      }
    } yield {
      ret
    }
  }

  //create a new session
  def createSession(uid: BSONObjectID, sessiontype: Int, visabletype: Int, jointype: Int, name: String): Future[(BSONValue, String)] = {
    var errmsg = ""
    val selector = BSONDocument("_id" -> uid)

    for {
      uids <- findCollection(usersCollection, selector, 1)
      ret <- {
        if (uids.isEmpty) {
          errmsg = "uid not exists"
          Future(BSONNull, errmsg)
        } else {
          val doc = BSONDocument(
            "creatoruid" -> uid,
            "sessiontype" -> sessiontype,
            "visabletype" -> visabletype,
            "jointype" -> jointype,
            "name" -> name,
            "dateline" -> System.currentTimeMillis(),
            "uids" -> BSONArray()
          )
          insertCollection(sessionsCollection, doc).map { id =>
            if (id != BSONNull) {
              (id, errmsg)
            } else {
              (BSONNull, "insert error")
            }
          }
        }
      }
    } yield {
      ret
    }
  }

  //verify user is in session
  def verifySession(uid: BSONObjectID, sessionid: BSONObjectID): Future[String] = {
    for {
      uids <- findCollection(usersCollection, BSONDocument("_id" -> uid, "sessions" -> sessionid), 1)
      sessionids <- findCollection(sessionsCollection, BSONDocument("_id" -> sessionid, "uids" -> uid), 1)
      errmsg <- Future{
        if (uids.nonEmpty && sessionids.nonEmpty) {
          ""
        } else {
          "no privilege in this session"
        }
      }
    } yield {
      errmsg
    }
  }

  //create a new message
  def createMessage(uid: BSONObjectID, sessionid: BSONObjectID, msgtype: Int, content: String, fileinfo: BSONDocument): Future[(BSONValue, String)] = {
    for {
      errmsg <- verifySession(uid, sessionid)
      ret <- {
        if (errmsg != "") {
          Future((BSONNull, errmsg))
        } else {
          val doc = BSONDocument(
            "uid" -> uid,
            "sessionid" -> sessionid,
            "msgtype" -> msgtype,
            "content" -> content,
            "fileinfo" -> fileinfo,
            "dateline" -> System.currentTimeMillis()
          )
          insertCollection(messagesCollection, doc).map { id => (id, errmsg)}
        }
      }
    } yield {
      ret
    }
  }



}
