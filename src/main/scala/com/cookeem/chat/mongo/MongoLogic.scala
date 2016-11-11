package com.cookeem.chat.mongo

import java.util.Date

import com.cookeem.chat.common.CommonUtils._
import com.cookeem.chat.event._
import com.cookeem.chat.jwt.JwtOps._
import com.cookeem.chat.mongo.MongoOps._
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.bson._

import scala.concurrent.Future
/**
  * Created by cookeem on 16/10/28.
  */
object MongoLogic {
  val colUsersName = "users"
  val colSessionsName = "sessions"
  val colMessagesName = "messages"
  val colOnlinesName = "onlines"

  val usersCollection = cookimDB.map(_.collection[BSONCollection](colUsersName))
  val sessionsCollection = cookimDB.map(_.collection[BSONCollection](colSessionsName))
  val messagesCollection = cookimDB.map(_.collection[BSONCollection](colMessagesName))
  val onlinesCollection = cookimDB.map(_.collection[BSONCollection](colOnlinesName))

  implicit def sessionStatusHandler = Macros.handler[SessionStatus]
  implicit def userHandler = Macros.handler[User]
  implicit def userStatusHandler = Macros.handler[UserStatus]
  implicit def sessionHandler = Macros.handler[Session]
  implicit def fileInfoHandler = Macros.handler[FileInfo]
  implicit def messageHandler = Macros.handler[Message]
  implicit def onlineHandler = Macros.handler[Online]

  createUsersCollection()
  createSessionsCollection()
  createMessagesCollection()
  createOnlinesCollection()

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
      ("senduid", 1, false, 0),
      ("recvuid", 1, false, 0)
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

  //create online collection and index
  def createOnlinesCollection(): Future[String] = {
    val indexSettings = Array(
      ("uid", 1, true, 0),
      ("time", -1, false, 15 * 60)
    )
    createIndex(colOnlinesName, indexSettings)
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
        users <- findCollection[User](usersCollection, document("login" -> login), count = 1)
        (uid, errmsg) <- {
          if (users.nonEmpty) {
            errmsg = "user already exist"
            Future((users.head._id, errmsg))
          } else {
            val newUser = User("", login, nickname, sha1(password), gender, avatar)
            insertCollection[User](usersCollection, newUser)
          }
        }
      } yield {
        (uid, errmsg)
      }
    }
  }

  def getUserInfo(uid: String): Future[User] = {
    for {
      users <- findCollection[User](usersCollection, document("_id" -> uid), count = 1)
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
      users <- findCollection[User](usersCollection, document("login" -> login), count = 1)
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

  def logoutAction(userTokenStr: String): Future[UpdateResult] = {
    val userToken = verifyUserToken(userTokenStr)
    if (userToken.uid != "") {
      removeCollection(usersCollection, document("uid" -> userToken.uid))
    } else {
      Future(UpdateResult(n = 0, errmsg = "no privilege to logout"))
    }
  }

  //update user online status
  def updateOnline(uid: String): Future[String] = {
    val selector = document("uid" -> uid)
    for {
      onlines <- findCollection[Online](onlinesCollection, selector, count = 1)
      errmsg <- {
        if (onlines.nonEmpty) {
          // time expire after 15 minutes
          val onlineNew = Online("", uid, new Date())
          insertCollection[Online](onlinesCollection, onlineNew).map { case (id, errmsg) =>
            errmsg
          }
        } else {
          val update = document("time" -> new Date())
          updateCollection(onlinesCollection, selector, update).map { ur =>
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
      users <- findCollection[User](usersCollection, document("_id" -> uid, "friends" -> document("$ne" -> fuid)), count = 1)
      friends <- findCollection[User](usersCollection, document("_id" -> fuid), count = 1)
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
      users <- findCollection[User](usersCollection, document("_id" -> uid, "friends" -> document("$eq" -> fuid)), count = 1)
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
      users <- findCollection[User](usersCollection, document("_id" -> uid), count = 1)
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
          friends = findCollection[User](usersCollection, selector, count = -1)
        }
        friends
      }
    } yield {
      friends
    }
  }

  //create a new group session
  def createGroupSession(uid: String, sessiontype: Int, visabletype: Int, jointype: Int, name: String): Future[(String, String)] = {
    var errmsg = ""
    val selector = document("_id" -> uid)
    for {
      users <- findCollection[User](usersCollection, selector, count = 1)
      (sessionid, errmsg) <- {
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
          val newSession = Session("", senduid = uid, recvuid = "", sessiontype, visabletype, jointype, name)
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
      (sessionid, errmsg)
    }
  }

  //create private session if not exist or get private session
  def createPrivateSession(uid: String, ouid: String): Future[(String, String)] = {
    for {
      users <- findCollection[User](usersCollection, document("_id" -> uid), count = 1)
      ousers <- findCollection[User](usersCollection, document("_id" -> ouid), count = 1)
      (sessions, errmsgUserNotExist) <- {
        var sessions = List[Session]()
        var errmsg = ""
        var ret = Future(sessions, errmsg)
        if (users.nonEmpty && ousers.nonEmpty) {
          val selector = document(
            "$or" -> array(
              document("senduid" -> uid, "recvuid" -> ouid),
              document("senduid" -> ouid, "recvuid" -> uid)
            )
          )
          ret = findCollection[Session](sessionsCollection, selector, count = 1).map {s => (s, "")}
        } else {
          errmsg = "send user or recv user not exist"
          ret = Future(sessions, errmsg)
        }
        ret
      }
      (sessionid, errmsg) <- {
        var ret = Future("", errmsgUserNotExist)
        if (errmsgUserNotExist == "") {
          if (sessions.nonEmpty) {
            ret = Future(sessions.head._id, "")
          } else {
            val newSession = Session("", senduid = uid, recvuid = "", sessiontype = 0, visabletype = 0, jointype = 0, name = "")
            ret = insertCollection[Session](sessionsCollection, newSession)
            for {
              (sessionid, errmsg) <- ret
              uidJoin <- {
                if (sessionid != "") {
                  joinSession(uid, sessionid)
                } else {
                  Future(UpdateResult(0, "sessionid is empty"))
                }
              }
              ouidJoin <- {
                if (sessionid != "") {
                  joinSession(uid, sessionid)
                } else {
                  Future(UpdateResult(0, "sessionid is empty"))
                }
              }
            } yield {
            }
          }
        }
        ret
      }
    } yield {
      (sessionid, errmsg)
    }
  }

  //get session info and users who join this session
  def getSessionInfo(sessionid: String): Future[(Session, List[User])] = {
    for {
      sessions <- findCollection[Session](sessionsCollection, document("_id" -> sessionid), count = 1)
      users <- {
        var users = Future(List[User]())
        if (sessions.nonEmpty) {
          val uids = sessions.head.usersstatus.map(_.uid)
          users = findCollection[User](usersCollection, document("_id" -> document("$in" -> array(uids))), count = 1)
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
        sessions <- findCollection[Session](sessionsCollection, document("_id" -> sessionid, "senduid" -> uid), count = 1)
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
    val selector = document("_id" -> uid, "sessionsstatus.sessionid" -> document("$ne" -> sessionid))
    for {
      users <- findCollection[User](usersCollection, selector, count = 1)
      sessions <- findCollection[Session](sessionsCollection, document("_id" -> sessionid), count = 1)
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
              val docSessionStatus = document("sessionid" -> sessionid, "newcount" -> 0)
              val update1 = document("$push" -> document("sessionsstatus" -> docSessionStatus))
              updateCollection(usersCollection, document("_id" -> uid), update1)
            }
            ur2 <- {
              val docUserStatus = document("uid" -> uid, "online" -> false)
              val update2 = document("$push" -> document("usersstatus" -> docUserStatus))
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
    for {
      users <- findCollection[User](usersCollection, document("_id" -> uid, "sessionsstatus.sessionid" -> sessionid), count = 1)
      sessions <- findCollection[Session](sessionsCollection, document("_id" -> sessionid, "usersstatus.uid" -> uid), count = 1)
      ret <- {
        if (users.isEmpty || sessions.isEmpty) {
          val errmsg = "user not exists or not join the session"
          Future(UpdateResult(n = 0, errmsg = errmsg))
        } else {
          for {
            ur1 <- {
              val sessionstatus = users.head.sessionsstatus.filter(_.sessionid == sessionid).head
              val docSessionStatus = document("sessionid" -> sessionstatus.sessionid, "newcount" -> sessionstatus.newcount)
              val update1 = document("$pull" -> document("sessionsstatus" -> docSessionStatus))
              updateCollection(usersCollection, document("_id" -> uid), update1)
            }
            ur2 <- {
              val userstatus = sessions.head.usersstatus.filter(_.uid == uid).head
              val docUserStatus = document("uid" -> userstatus.uid, "online" -> userstatus.online)
              val update2 = document("$pull" -> document("usersstatus" -> docUserStatus))
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
      users <- findCollection[User](usersCollection, document("_id" -> uid), count = 1)
      sessions <- {
        var sessions = Future(List[Session]())
        if (users.nonEmpty) {
          val user = users.head
          val sessionids = user.sessionsstatus.map(_.sessionid)
          var ba = array()
          sessionids.foreach { sessionid =>
            ba = ba.merge(sessionid)
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
      users <- findCollection[User](usersCollection, document("_id" -> uid), count = 1)
      sessions <- {
        val sessionids = if (users.isEmpty) {
          List[String]()
        } else {
          val user = users.head
          user.sessionsstatus.map(_.sessionid)
        }
        var ba = array()
        sessionids.foreach { sessionid =>
          ba = ba.merge(sessionid)
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
          sessions = findCollection[Session](sessionsCollection, selector, count = -1)
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
      users <- findCollection[User](usersCollection, document("_id" -> senduid, "sessionsstatus.sessionid" -> sessionid), count = 1)
      sessions <- findCollection[Session](sessionsCollection, document("_id" -> sessionid, "usersstatus.uid" -> senduid), count = 1)
    } yield {
      if (users.nonEmpty && sessions.nonEmpty) {
        ""
      } else {
        "no privilege to send message in this session"
      }
    }
  }

  //create a new message
  def createMessage(uid: String, sessionid: String, msgtype: String, noticetype: String, message: String, fileinfo: FileInfo): Future[(String, String)] = {
    for {
      errmsg <- verifySession(uid, sessionid)
      (msgid, errmsg) <- {
        if (errmsg != "") {
          Future(("", errmsg))
        } else {
          val newMessage = Message("", uid, sessionid, msgtype, noticetype = noticetype, message = message, fileinfo = fileinfo)
          insertCollection[Message](messagesCollection, newMessage)
        }
      }
    } yield {
      (msgid, errmsg)
    }
  }

  def getMessageById(userTokenStr: String, sessionTokenStr: String, msgid: String): Future[(Message, User)] = {
    val UserSessionInfo(uid, nickname, avatar, sessionid) = verifyUserSessionToken(userTokenStr, sessionTokenStr)
    if (uid == "") {
      Future((null, null))
    } else {
      for {
        messages <- findCollection[Message](messagesCollection, document("_id" -> msgid, "sessionid" -> sessionid), count = 1)
        (message, users) <- {
          var message: Message = null
          if (messages.nonEmpty) {
            message = messages.head
            findCollection[User](usersCollection, document("_id" -> message.senduid)).map { user =>
              (message, user)
            }
          } else {
            Future((message, List[User]()))
          }
        }
      } yield {
        if (message != null && users.nonEmpty) {
          val user = users.head
          (message, user)
        } else {
          (null, null)
        }
      }
    }
  }

  def getSessionLastMessage(userTokenStr: String, sessionid: String): Future[(Session, Message, User)] = {
    val UserToken(uid, nickname, avatar) = verifyUserToken(userTokenStr)
    if (uid != "") {
      for {
        sessions <- findCollection[Session](sessionsCollection, document("_id" -> sessionid), count = 1)
        (session, messages) <- {
          var session: Session = null
          if (sessions.nonEmpty) {
            session = sessions.head
            findCollection[Message](messagesCollection, document("_id" -> session.lastmsgid)).map { messages =>
              (session, messages)
            }
          } else {
            Future(null, List[Message]())
          }
        }
        (session, message, users) <- {
          if (messages.nonEmpty) {
            val message = messages.head
            findCollection[User](usersCollection, document("_id" -> message.senduid), count = 1).map { users =>
              (session, message, users)
            }
          } else {
            Future(null, null, List[User]())
          }
        }
      } yield {
        if (users.nonEmpty) {
          val user = users.head
          (session, message, user)
        } else {
          (session, null, null)
        }
      }
    } else {
      Future(null, null, null)
    }
  }

  //list history messages
  def listHistoryMessages(uid: String, sessionid: String, page: Int = 1, count: Int = 10, sort: BSONDocument): Future[List[Message]] = {
    for {
      errmsg <- verifySession(uid, sessionid)
      messages <- {
        var messages = Future(List[Message]())
        if (errmsg == "") {
          messages = findCollection[Message](messagesCollection, document("sessionid" -> sessionid), count = count, page = page, sort = sort)
        }
        messages
      }
    } yield {
      messages
    }
  }

  //create user token, include uid, nickname, avatar
  def createUserToken(uid: String): Future[String] = {
    for {
      users <- findCollection[User](usersCollection, document("_id" -> uid), count = 1)
    } yield {
      var token = ""
      if (users.nonEmpty) {
        val user = users.head
        val payload = Map[String, Any](
          "uid" -> user._id,
          "nickname" -> user.nickname,
          "avatar" -> user.avatar
        )
        token = encodeJwt(payload)
      }
      token
    }
  }

  def verifyUserToken(token: String): UserToken = {
    var userToken = UserToken("", "", "")
    val mapUserToken = decodeJwt(token)
    if (mapUserToken.contains("uid") && mapUserToken.contains("nickname") && mapUserToken.contains("avatar")) {
      val uid = mapUserToken("uid").asInstanceOf[String]
      val nickname = mapUserToken("nickname").asInstanceOf[String]
      val avatar = mapUserToken("avatar").asInstanceOf[String]
      if (uid != "" && nickname != "" && avatar != "") {
        userToken = UserToken(uid, nickname, avatar)
      }
    }
    userToken
  }

  //create session token, include sessionid
  def createSessionToken(uid: String, sessionid: String): Future[String] = {
    for {
      errmsg <- verifySession(uid, sessionid)
    } yield {
      var token = ""
      if (errmsg == "") {
        val payload = Map[String, Any](
          "sessionid" -> sessionid
        )
        token = encodeJwt(payload)
      }
      token
    }
  }

  def verifySessionToken(token: String): SessionToken = {
    var sessionToken = SessionToken("")
    val mapSessionToken = decodeJwt(token)
    if (mapSessionToken.contains("sessionid")) {
      val sessionid = mapSessionToken("sessionid").asInstanceOf[String]
      if (sessionid != "") {
        sessionToken = SessionToken(sessionid)
      }
    }
    sessionToken
  }

  def verifyUserSessionToken(userTokenStr: String, sessionTokenStr: String): UserSessionInfo = {
    val userToken = verifyUserToken(userTokenStr)
    val sessionToken = verifySessionToken(sessionTokenStr)
    if (userToken.uid != "" && userToken.nickname != "" && userToken.avatar != "" && sessionToken.sessionid != "") {
      UserSessionInfo(userToken.uid, userToken.nickname, userToken.avatar, sessionToken.sessionid)
    } else {
      UserSessionInfo("", "", "", "")
    }
  }

}
