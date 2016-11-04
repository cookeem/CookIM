package com.cookeem.chat.mongo

import java.util.Date

import com.cookeem.chat.common.CommonUtils._
import com.cookeem.chat.mongo.MongoOps._
import reactivemongo.bson._

import scala.concurrent.Future
/**
  * Created by cookeem on 16/10/28.
  */
object MongoLogic {
  //create users collection and index
  def createUsersCollection(): Future[String] = {
    val indexSettings = Array(
      ("login", 1, true, 0)
    )
    createIndex(colUsersName, indexSettings)
  }

  //create sessions collection and index
  def createSessionsCollection(): Future[String] = {
    val indexSettings = Array(
      ("senduid", 1, false, 0)
    )
    createIndex(colSessionsName, indexSettings)
  }

  //create messages collection and index
  def createMessagesCollection(): Future[String] = {
    val indexSettings = Array(
      ("senduid", 1, false, 0),
      ("sessionid", 1, false, 0)
    )
    createIndex(colMessagesName, indexSettings)
  }

  //create inbox collection and index
  def createInboxCollection(): Future[String] = {
    val indexSettings = Array(
      ("recvuid", 1, false, 0),
      ("sessionid", 1, false, 0)
    )
    createIndex(colInboxName, indexSettings)
  }

  //create online collection and index
  def createOnlineCollection(): Future[String] = {
    val indexSettings = Array(
      ("uid", 1, true, 0),
      ("time", -1, false, 15 * 60)
    )
    createIndex(colOnlineName, indexSettings)
  }

  //register new user
  def registerUser(login: String, nickname: String, password: String, gender: Int, avatar: String): Future[(String, String)] = {
    var errmsg = ""
    if (!isEmail(login)) {
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
      Future(("", errmsg))
    } else {
      for {
        users <- findCollection[User](usersCollection, document("login" -> login), 1)
        ret <- {
          if (users.nonEmpty) {
            errmsg = "user already exist"
            Future((users.head._id, errmsg))
          } else {
            println("i")
            val newUser = User("", login, nickname, sha1(password), gender, avatar)
            insertCollection[User](usersCollection, newUser)
          }
        }
      } yield {
        ret
      }
    }
  }

  def getUserInfo(uid: String): Future[User] = {
    for {
      users <- findCollection[User](usersCollection, document("_id" -> uid), 1)
    } yield {
      users.headOption.orNull
    }
  }

  //update users info
  def updateUserInfo(uid: String, nickname: String = "", gender: Int = 0, avatar: String = ""): Future[UpdateResult] = {
    var errmsg = ""
    var update = document()
    var sets = document()
    if (nickname.getBytes.length >= 4) {
      sets = sets.merge(document("nickname" -> nickname))
    }
    if (gender == 0 || gender == 1 || gender == 2) {
      sets = sets.merge(document("gender" -> gender))
    }
    if (avatar.length >= 6) {
      sets = sets.merge(document("avatar" -> avatar))
    }
    if (sets == document()) {
      errmsg = "nothing to update"
    } else {
      update = document("$set" -> sets)
    }
    if (errmsg != "") {
      Future(UpdateResult(n = 0, errmsg = errmsg))
    } else {
      updateCollection(usersCollection, document("uid" -> uid), update)
    }
  }

  def loginAction(login: String, pwd: String): Future[String] = {
    for {
      users <- findCollection[User](usersCollection, document("login" -> login), 1)
    } yield {
      var uid = ""
      if (users.nonEmpty) {
        val user = users.head
        val pwdSha1 = user.password
        if (pwdSha1 != "" && sha1(pwd) == pwdSha1) {
          uid = user._id
          loginUpdate(uid)
        }
      }
      uid
    }
  }

  def logoutAction(uid: String): Future[UpdateResult] = {
    removeCollection(usersCollection, document("uid" -> uid))
  }

  //update user online status
  def updateOnline(uid: String): Future[String] = {
    val selector = document("uid" -> uid)
    for {
      onlines <- findCollection[Online](onlineCollection, selector, 1)
      errmsg <- {
        if (onlines.nonEmpty) {
          // time expire after 15 minutes
          val onlineNew = Online("", uid, new Date())
          insertCollection[Online](onlineCollection, onlineNew).map { case (id, errmsg) =>
            errmsg
          }
        } else {
          val update = document("time" -> new Date())
          updateCollection(onlineCollection, selector, update).map { ur =>
            ur.errmsg
          }
        }
      }
    } yield {
      errmsg
    }
  }

  //check and change password
  def changePwd(uid: String, oldPwd: String, newPwd: String): Future[UpdateResult] = {
    var errmsg = ""
    if (oldPwd.length < 6) {
      errmsg = "old password must at least 6 charactors"
    }
    if (newPwd.length < 6) {
      errmsg = "new password must at least 6 charactors"
    }
    if (newPwd == oldPwd) {
      errmsg = "new password and old password can not be same"
    }
    if (errmsg != "") {
      Future(UpdateResult(0, errmsg))
    } else {
      val selector = document("_id" -> uid, "password" -> sha1(oldPwd))
      val update = document(
        "$set" -> document("password" -> sha1(newPwd))
      )
      updateCollection(usersCollection, selector, update).map{ ur =>
        if (ur.n == 0) {
          errmsg = "user not exist or password not match"
          UpdateResult(0, errmsg)
        } else {
          ur
        }
      }
    }
  }

  //when user login, update the logincount and lastlogin and online info
  def loginUpdate(uid: String): Future[UpdateResult] = {
    for {
      onlineResult <- updateOnline(uid)
      loginResult <- {
        val selector = document("_id" -> uid)
        val update = document(
          "$inc" -> document("logincount" -> 1),
          "$set" -> document("lastlogin" -> System.currentTimeMillis())
        )
        updateCollection(usersCollection, selector, update)
      }
    } yield {
      loginResult
    }
  }

  //join new friend
  def joinFriend(uid: String, fuid: String): Future[UpdateResult] = {
    var errmsg = ""
    for {
      users <- findCollection[User](usersCollection, document("_id" -> uid, "friends" -> document("$ne" -> fuid)), 1)
      friends <- findCollection[User](usersCollection, document("_id" -> fuid), 1)
      updateResult <- {
        if (users.isEmpty) {
          errmsg = "user not exist or already your friend"
        }
        if (friends.isEmpty) {
          errmsg = "user friend not exists"
        }
        var ret = Future(UpdateResult(n = 0, errmsg = errmsg))
        if (errmsg == "") {
          val update = document("$push" -> document("friends" -> fuid))
          ret = updateCollection(usersCollection, document("_id" -> uid), update)
        }
        ret
      }
    } yield {
      updateResult
    }
  }

  //remove friend
  def removeFriend(uid: String, fuid: String): Future[UpdateResult] = {
    var errmsg = ""
    for {
      users <- findCollection[User](usersCollection, document("_id" -> uid, "friends" -> document("$eq" -> fuid)), 1)
      ret <- {
        if (users.isEmpty) {
          errmsg = "user not exists or friend not in your friends"
          Future(UpdateResult(n = 0, errmsg = errmsg))
        } else {
          val update = document("$pull" -> document("friends" -> fuid))
          updateCollection(usersCollection, document("_id" -> uid), update)
        }
      }
    } yield {
      ret
    }
  }

  def listFriend(uid: String): Future[List[User]] = {
    for {
      users <- findCollection[User](usersCollection, document("_id" -> uid), 1)
      friends <- {
        var friends = Future(List[User]())
        if (users.nonEmpty) {
          val user = users.head
          val fuids = user.friends
          val selector = document(
            "_id" -> document(
              "$in" -> fuids
            )
          )
          friends = findCollection[User](usersCollection, selector, -1)
        }
        friends
      }
    } yield {
      friends
    }
  }

  //create a new group session
  def createSession(uid: String, sessiontype: Int, visabletype: Int, jointype: Int, name: String): Future[(String, String)] = {
    var errmsg = ""
    val selector = document("_id" -> uid)
    for {
      users <- findCollection[User](usersCollection, selector, 1)
      ret <- {
        if (users.isEmpty) {
          errmsg = "user not exists"
          Future("", errmsg)
        } else if (!(sessiontype == 0 || sessiontype == 1)) {
          errmsg = "sessiontype error"
          Future("", errmsg)
        } else if (!(visabletype == 0 || visabletype == 1)) {
          errmsg = "visabletype error"
          Future("", errmsg)
        } else if (!(jointype == 0 || jointype == 1)) {
          errmsg = "jointype error"
          Future("", errmsg)
        } else {
          val newSession = Session("", uid, sessiontype, visabletype, jointype, name)
          val insRet = insertCollection[Session](sessionsCollection, newSession)
          for {
            (sessionid, errormsg) <- insRet
            retJoin <- {
              var retJoin = Future(UpdateResult(n = 0, errmsg = errormsg))
              if (errormsg == "") {
                retJoin = joinSession(uid, sessionid)
              }
              retJoin
            }
          } yield {
            retJoin
          }
          insRet
        }
      }
    } yield {
      ret
    }
  }

  //get session info and users who join this session
  def getSessionInfo(sessionid: String): Future[(Session, List[User])] = {
    for {
      sessions <- findCollection[Session](sessionsCollection, document("_id" -> sessionid), 1)
      users <- {
        var users = Future(List[User]())
        if (sessions.nonEmpty) {
          val uids = sessions.head.uids
          users = findCollection[User](usersCollection, document("_id" -> document("$in" -> array(uids))), 1)
        }
        users
      }
    } yield {
      (sessions.headOption.orNull, users)
    }
  }

  //update session info
  def updateSessionInfo(sessionid: String, uid: String, visabletype: Int, jointype: Int, name: String): Future[UpdateResult] = {
    var errmsg = ""
    var update = document()
    if (!(visabletype == 0 || visabletype == 1)) {
      errmsg = "visabletype error"
    } else if (!(jointype == 0 || jointype == 1)) {
      errmsg = "jointype error"
    } else if (name == "") {
      errmsg = "name can not be empty"
    } else {
      update = document(
        "$set" -> document(
          "visabletype" -> visabletype,
          "jointype" -> jointype,
          "name" -> name
        )
      )
    }
    var ret = Future(UpdateResult(n = 0, errmsg = errmsg))
    if (errmsg == "") {
      ret = for {
        sessions <- findCollection[Session](sessionsCollection, document("_id" -> sessionid, "senduid" -> uid), 1)
        updateResult <- {
          if (sessions.isEmpty) {
            Future(UpdateResult(n = 0, errmsg = "no privilege to update session info"))
          } else {
            updateCollection(sessionsCollection, document("_id" -> sessionid), update)
          }
        }
      } yield {
        updateResult
      }
      Future(UpdateResult(n = 0, errmsg = errmsg))
    }
    ret
  }


  //join new session
  def joinSession(uid: String, sessionid: String): Future[UpdateResult] = {
    var errmsg = ""
    val selector = document("_id" -> uid, "sessions" -> document("$ne" -> sessionid))
    for {
      users <- findCollection[User](usersCollection, selector, 1)
      sessions <- findCollection[Session](sessionsCollection, document("_id" -> sessionid), 1)
      updateResult <- {
        if (users.isEmpty) {
          errmsg = "user not exists or already join session"
        }
        if (sessions.isEmpty) {
          errmsg = "session not exists"
        }
        var ret = Future(UpdateResult(n = 0, errmsg = errmsg))
        if (errmsg == "") {
          ret = for {
            ur1 <- {
              val update1 = document("$push" -> document("sessions" -> sessionid))
              updateCollection(usersCollection, document("_id" -> uid), update1)
            }
            ur2 <- {
              val update2 = document("$push" -> document("uids" -> uid))
              updateCollection(sessionsCollection, document("_id" -> sessionid), update2)
            }
          } yield {
            ur2
          }
        }
        ret
      }
    } yield {
      updateResult
    }
  }

  //leave session
  def leaveSession(uid: String, sessionid: String): Future[UpdateResult] = {
    val selector = document("_id" -> uid, "sessions" -> document("$eq" -> sessionid))
    for {
      users <- findCollection[User](usersCollection, selector, 1)
      ret <- {
        if (users.isEmpty) {
          val errmsg = "user not exists or not join the session"
          Future(UpdateResult(n = 0, errmsg = errmsg))
        } else {
          for {
            ur1 <- {
              val update1 = document("$pull" -> document("sessions" -> sessionid))
              updateCollection(usersCollection, document("_id" -> uid), update1)
            }
            ur2 <- {
              val update2 = document("$pull" -> document("uids" -> uid))
              updateCollection(sessionsCollection, document("_id" -> sessionid), update2)
            }
          } yield {
            ur2
          }
        }
      }
    } yield {
      ret
    }
  }

  //list all session public and user not joined yet
  def listPublicSessions(uid: String): Future[List[Session]] = {
    for {
      users <- findCollection[User](usersCollection, document("_id" -> uid), 1)
      sessions <- {
        var sessions = Future(List[Session]())
        if (users.nonEmpty) {
          val user = users.head
          val sessionids = user.sessions
          var ba = array()
          sessionids.foreach { sessionid =>
            ba = ba.add(sessionid)
          }
          val selector = document(
            "visabletype" -> 1,   //visabletype（可见类型：0：不可见，1：公开可见）
            "jointype" -> 0,      //jointype（加入类型：0：所有人可以加入，1：群里用户邀请才能加入）
            "sessiontype" -> 1,   //sessiontype（会话类型：0：私聊，1：群聊）
            "_id" -> document(
              "$nin" -> ba
            )
          )
          if (ba.size > 0) {
            sessions = findCollection[Session](sessionsCollection, selector)
          }
        }
        sessions
      }
    } yield {
      sessions
    }
  }


  // list my sessions, showType: (0: private, 1:group , 2:all)
  def listJoinedSessions(uid: String, showType: Int): Future[List[Session]] = {
    for {
      users <- findCollection[User](usersCollection, document("_id" -> uid), 1)
      sessions <- {
        val sessionids = if (users.isEmpty) {
          List[String]()
        } else {
          val user = users.head
          user.sessions
        }
        var ba = array()
        sessionids.foreach { sessionid =>
          ba = ba.add(sessionid)
        }
        var selector = document(
          "_id" -> document(
            "$in" -> ba
          )
        )
        showType match {
          case 0 =>
            selector = selector.merge(document("sessiontype" -> 0))
          case 1 =>
            selector = selector.merge(document("sessiontype" -> 1))
          case _ =>
        }
        var sessions = Future(List[Session]())
        if (ba.size > 0) {
          sessions = findCollection[Session](sessionsCollection, selector)
        }
        sessions
      }
    } yield {
      sessions
    }
  }

  //verify user is in session
  def verifySession(senduid: String, sessionid: String): Future[String] = {
    for {
      users <- findCollection[User](usersCollection, document("_id" -> senduid, "sessions" -> sessionid), 1)
      sessions <- findCollection[Session](sessionsCollection, document("_id" -> sessionid, "uids" -> senduid), 1)
    } yield {
      if (users.nonEmpty && sessions.nonEmpty) {
        ""
      } else {
        "no privilege to send message in this session"
      }
    }
  }

  //create a new message
  def createMessage(uid: String, sessionid: String, msgtype: Int, content: String, fileinfo: FileInfo): Future[(String, String)] = {
    for {
      errmsg <- verifySession(uid, sessionid)
      ret <- {
        if (errmsg != "") {
          Future(("", errmsg))
        } else {
          val newMessage = Message("", uid, sessionid, msgtype, content, fileinfo)
          insertCollection[Message](messagesCollection, newMessage)
        }
      }
    } yield {
      ret
    }
  }

  //create a new inbox message
  def createInboxMessage(senduid: String, recvuid: String, sessionid: String, msgtype: Int, content: String, fileinfo: FileInfo): Future[(String, String)] = {
    for {
      errmsg <- verifySession(senduid, sessionid)
      ret <- {
        if (errmsg != "") {
          Future(("", errmsg))
        } else {
          val newInbox = Inbox("", recvuid, senduid, sessionid, msgtype, content, fileinfo)
          insertCollection[Inbox](messagesCollection, newInbox)
        }
      }
    } yield {
      ret
    }
  }

  //read inbox messages
  def readInboxMessage(recvuid: String, sessionid: String): Future[List[Message]] = {
    for {
      users <- findCollection[User](usersCollection, document("_id" -> recvuid, "sessions" -> sessionid), 1)
      messages <- {
        var messages = Future(List[Message]())
        if (users.nonEmpty) {
          val selector = document("recvuid" -> recvuid, "sessionid" -> sessionid)
          messages = findCollection[Message](inboxCollection, selector, sort = document("dateline" -> -1))
          removeCollection(inboxCollection, selector)
        }
        messages
      }
    } yield {
      messages
    }
  }



}
