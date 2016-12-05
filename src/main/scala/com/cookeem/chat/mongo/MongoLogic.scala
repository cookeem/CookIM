package com.cookeem.chat.mongo

import java.util.Date

import akka.actor.ActorRef
import com.cookeem.chat.common.CommonUtils._
import com.cookeem.chat.event.WsTextDown
import com.cookeem.chat.jwt.JwtOps._
import com.cookeem.chat.mongo.MongoOps._
import play.api.libs.json.JsObject
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.bson._
import reactivemongo.play.json.BSONFormats

import scala.concurrent.Future
/**
  * Created by cookeem on 16/10/28.
  */
object MongoLogic {
  val colUsersName = "users"
  val colSessionsName = "sessions"
  val colMessagesName = "messages"
  val colOnlinesName = "onlines"
  val colNotificationsName = "notifications"

  val usersCollection = cookimDB.map(_.collection[BSONCollection](colUsersName))
  val sessionsCollection = cookimDB.map(_.collection[BSONCollection](colSessionsName))
  val messagesCollection = cookimDB.map(_.collection[BSONCollection](colMessagesName))
  val onlinesCollection = cookimDB.map(_.collection[BSONCollection](colOnlinesName))
  val notificationsCollection = cookimDB.map(_.collection[BSONCollection](colNotificationsName))

  implicit def sessionStatusHandler = Macros.handler[SessionStatus]
  implicit def userHandler = Macros.handler[User]
  implicit def userStatusHandler = Macros.handler[UserStatus]
  implicit def sessionHandler = Macros.handler[Session]
  implicit def fileInfoHandler = Macros.handler[FileInfo]
  implicit def messageHandler = Macros.handler[Message]
  implicit def onlineHandler = Macros.handler[Online]
  implicit def notificationHandler = Macros.handler[Notification]

  //create users collection and index
  def createUsersCollection(): Future[String] = {
    val indexSettings = Array(
      //colName, sort, unique, expire
      ("login", 1, true, 0),
      ("nickname", 1, false, 0)
    )
    createIndex(colUsersName, indexSettings)
  }

  //create sessions collection and index
  def createSessionsCollection(): Future[String] = {
    val indexSettings = Array(
      //colName, sort, unique, expire
      ("createuid", 1, false, 0),
      ("ouid", 1, false, 0),
      ("lastUpdate", -1, false, 0)
    )
    createIndex(colSessionsName, indexSettings)
  }

  //create messages collection and index
  def createMessagesCollection(): Future[String] = {
    val indexSettings = Array(
      //colName, sort, unique, expire
      ("uid", 1, false, 0),
      ("sessionid", 1, false, 0),
      ("dateline", -1, false, 0)
    )
    createIndex(colMessagesName, indexSettings)
  }

  //create onlines collection and index
  def createOnlinesCollection(): Future[String] = {
    val indexSettings = Array(
      //colName, sort, unique, expire
      ("uid", 1, true, 0),
      ("dateline", -1, false, 15 * 60)
    )
    createIndex(colOnlinesName, indexSettings)
  }

  //create notifications collection and index
  def createNotificationsCollection(): Future[String] = {
    val indexSettings = Array(
      //colName, sort, unique, expire
      ("recvuid", 1, false, 0),
      ("dateline", -1, false, 0)
    )
    createIndex(colNotificationsName, indexSettings)
  }

  //register new user
  def registerUser(login: String, nickname: String, password: String, gender: Int, avatar: String): Future[(String, String, String)] = {
    var errmsg = ""
    val token = ""
    if (!isEmail(login)) {
      errmsg = "login must be email"
    } else if (nickname.getBytes.length < 4) {
      errmsg = "nickname must at least 4 charactors"
    } else if (password.length < 6) {
      errmsg = "password must at least 6 charactors"
    } else if (!(gender == 1 || gender == 2)) {
      errmsg = "gender must be boy or girl"
    } else if (avatar.length < 6) {
      errmsg = "avatar must at least 6 charactors"
    }
    if (errmsg != "") {
      Future(("", token, errmsg))
    } else {
      for {
        user <- findCollectionOne[User](usersCollection, document("login" -> login))
        (uid, token, errmsg) <- {
          if (user != null) {
            errmsg = "user already exist"
            Future((user._id, token, errmsg))
          } else {
            val newUser = User("", login, nickname, sha1(password), gender, avatar)
            insertCollection[User](usersCollection, newUser).map { case (iuid, ierrmsg) =>
              if (iuid != "") {
                loginUpdate(iuid)
                createUserToken(iuid).map { token => (iuid, token, ierrmsg) }
              } else {
                Future((iuid, token, ierrmsg))
              }
            }.flatMap(f => f)
          }
        }
      } yield {
        (uid, token, errmsg)
      }
    }
  }

  def getUserInfo(uid: String): Future[User] = {
    findCollectionOne[User](usersCollection, document("_id" -> uid))
  }

  //update users info
  def updateUserInfo(uid: String, nickname: String = "", gender: Int = 0, avatar: String = ""): Future[UpdateResult] = {
    var errmsg = ""
    var update = document()
    var sets = document()
    if (nickname.getBytes.length >= 4) {
      sets = sets.merge(document("nickname" -> nickname))
    }
    if (gender == 1 || gender == 2) {
      sets = sets.merge(document("gender" -> gender))
    }
    if (avatar.startsWith("images/")) {
      val avatarDefault = gender match {
        case 1 => "images/avatar/boy.jpg"
        case 2 => "images/avatar/girl.jpg"
        case _ => "images/avatar/unknown.jpg"
      }
      sets = sets.merge(document("avatar" -> avatarDefault))
    } else if (avatar.length >= 16) {
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
      updateCollection(usersCollection, document("_id" -> uid), update)
    }
  }

  def loginAction(login: String, pwd: String): Future[(String, String)] = {
    for {
      user <- findCollectionOne[User](usersCollection, document("login" -> login))
      (uid, token) <- {
        var uid = ""
        if (user != null) {
          val pwdSha1 = user.password
          if (pwdSha1 != "" && sha1(pwd) == pwdSha1) {
            uid = user._id
            loginUpdate(uid)
          }
        }
        if (uid != "") {
          createUserToken(uid).map { token => (uid, token) }
        } else {
          Future("", "")
        }
      }
    } yield {
      (uid, token)
    }
  }

  def logoutAction(userTokenStr: String): Future[UpdateResult] = {
    val userToken = verifyUserToken(userTokenStr)
    if (userToken.uid != "") {
      removeCollection(onlinesCollection, document("uid" -> userToken.uid))
    } else {
      Future(UpdateResult(n = 0, errmsg = "no privilege to logout"))
    }
  }

  //update user online status
  def updateOnline(uid: String): Future[String] = {
    val selector = document("uid" -> uid)
    for {
      online <- findCollectionOne[Online](onlinesCollection, selector)
      errmsg <- {
        if (online == null) {
          // time expire after 15 minutes
          val onlineNew = Online("", uid, new Date())
          insertCollection[Online](onlinesCollection, onlineNew).map { case (id, errmsg) =>
            errmsg
          }
        } else {
          val update = document("$set" -> document("dateline" -> new Date()))
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
  def changePwd(uid: String, oldPwd: String, newPwd: String, renewPwd: String): Future[UpdateResult] = {
    var errmsg = ""
    if (oldPwd.length < 6) {
      errmsg = "old password must at least 6 charactors"
    } else if (newPwd.length < 6) {
      errmsg = "new password must at least 6 charactors"
    } else if (newPwd != renewPwd) {
      errmsg = "new password and repeat password must be same"
    } else if (newPwd == oldPwd) {
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

  //when user login, update the loginCount and online info
  def loginUpdate(uid: String): Future[UpdateResult] = {
    for {
      onlineResult <- updateOnline(uid)
      loginResult <- {
        val selector = document("_id" -> uid)
        val update = document(
          "$inc" -> document("loginCount" -> 1)
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
      user <- findCollectionOne[User](usersCollection, document("_id" -> uid, "friends" -> document("$ne" -> fuid)))
      friend <- findCollectionOne[User](usersCollection, document("_id" -> fuid))
      updateResult <- {
        if (user == null) {
          errmsg = "user not exist or already your friend"
        }
        if (friend == null) {
          errmsg = "user friend not exists"
        }
        var ret = Future(UpdateResult(n = 0, errmsg = errmsg))
        if (errmsg == "") {
          val update = document("$push" -> document("friends" -> fuid))
          ret = for {
            notificationRet <- createNotification("joinFriend", uid, fuid, "")
            updateResult <- updateCollection(usersCollection, document("_id" -> uid), update)
          } yield {
            updateResult
          }
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
      user <- findCollectionOne[User](usersCollection, document("_id" -> uid, "friends" -> document("$eq" -> fuid)))
      ret <- {
        if (user == null) {
          errmsg = "user not exists or friend not in your friends"
          Future(UpdateResult(n = 0, errmsg = errmsg))
        } else {
          val update = document("$pull" -> document("friends" -> fuid))
          for {
            notificationRet <- createNotification("removeFriend", uid, fuid, "")
            ret <- updateCollection(usersCollection, document("_id" -> uid), update)
          } yield {
            ret
          }
        }
      }
    } yield {
      ret
    }
  }

  def listFriends(uid: String): Future[List[User]] = {
    for {
      user <- findCollectionOne[User](usersCollection, document("_id" -> uid))
      friends <- {
        var friends = Future(List[User]())
        if (user != null) {
          val fuids = user.friends
          val selector = document(
            "_id" -> document(
              "$in" -> fuids
            )
          )
          val sort = document("nickname" -> 1)
          friends = findCollection[User](usersCollection, selector)
        }
        friends
      }
    } yield {
      friends
    }
  }

  //create a new group session
  def createGroupSession(uid: String, sessionName: String, sessionIcon: String, publicType: Int)(implicit notificationActor: ActorRef): Future[(String, String)] = {
    var errmsg = ""
    val selector = document("_id" -> uid)
    val sessionType = 1
    for {
      user <- findCollectionOne[User](usersCollection, selector)
      (sessionid, errmsg) <- {
        if (user == null) {
          errmsg = "user not exists"
          Future("", errmsg)
        } else if (sessionName.length < 3) {
          errmsg = "session desc must at least 3 character"
          Future("", errmsg)
        } else if (!(publicType == 0 || publicType == 1)) {
          errmsg = "publicType error"
          Future("", errmsg)
        } else if (sessionIcon.length < 1) {
          errmsg = "please select chat icon"
          Future("", errmsg)
        } else {
          val newSession = Session("", createuid = uid, ouid = "", sessionName = sessionName, sessionIcon = sessionIcon, sessionType = sessionType, publicType = publicType)
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

  //get edit group session info
  def getEditGroupSessionInfo(uid: String, sessionid: String): Future[Session] = {
    findCollectionOne[Session](sessionsCollection, document("_id" -> sessionid, "createuid" -> uid))
  }

  //invite friend to session
  def inviteFriend(uid: String, fuid: String, sessionid: String)(implicit notificationActor: ActorRef): Future[(String, UpdateResult)] = {
    for {
      user <- findCollectionOne[User](usersCollection, document("_id" -> uid))
      fuser <- findCollectionOne[User](usersCollection, document("_id" -> fuid))
      joinResult <- {
        var errmsg = ""
        if (user == null || fuser == null) {
          errmsg = "user or friend not exist"
        }
        if (errmsg != "") {
          Future(fuser.nickname, UpdateResult(n = 0, errmsg = errmsg))
        } else {
          joinSession(fuid, sessionid).map{ updateResult =>
            if (updateResult.errmsg == "") {
              createNotification("inviteSession", uid, fuid, sessionid)
            }
            (fuser.nickname, updateResult)
          }
        }
      }
    } yield {
      joinResult
    }
  }

  def inviteFriendsToGroupSession(uid: String, fuids: List[String], sessionid: String)(implicit notificationActor: ActorRef): Future[List[(String, UpdateResult)]] = {
    Future.sequence(
      fuids.map { fuid =>
        inviteFriend(uid, fuid, sessionid)
      }
    )
  }

  //edit group session info
  def editGroupSession(uid: String, sessionid: String, sessionName: String, sessionIcon: String, publicType: Int): Future[String] = {
    var errmsg = ""
    val sessionType = 1
    for {
      user <- findCollectionOne[User](usersCollection, document("_id" -> uid))
      session <- findCollectionOne[Session](sessionsCollection, document("_id" -> sessionid))
      errmsg <- {
        if (user == null || session == null) {
          errmsg = "user or session not exists"
          Future(errmsg)
        } else if (session.createuid != uid) {
          errmsg = "you have no privilege to edit session info"
          Future(errmsg)
        } else if (sessionName.length < 3) {
          errmsg = "session desc must at least 3 character"
          Future(errmsg)
        } else if (!(publicType == 0 || publicType == 1)) {
          errmsg = "publicType error"
          Future(errmsg)
        } else {
          var sessionIconNew = session.sessionIcon
          if (sessionIcon.length > 0) {
            sessionIconNew = sessionIcon
          }
          val update = document(
            "$set" -> document(
              "sessionName" -> sessionName,
              "sessionIcon" -> sessionIconNew,
              "publicType" -> publicType
            )
          )
          updateCollection(sessionsCollection, document("_id" -> sessionid), update).map(_.errmsg)
        }
      }
    } yield {
      errmsg
    }
  }

  //create private session if not exist or get private session
  def createPrivateSession(uid: String, ouid: String)(implicit notificationActor: ActorRef): Future[(String, String)] = {
    for {
      user <- findCollectionOne[User](usersCollection, document("_id" -> uid))
      ouser <- findCollectionOne[User](usersCollection, document("_id" -> ouid))
      (session, errmsgUserNotExist) <- {
        var errmsg = ""
        var ret = Future[(Session, String)](null, errmsg)
        if (user != null && ouser != null) {
          val selector = document(
            "$or" -> array(
              document("createuid" -> uid, "ouid" -> ouid),
              document("createuid" -> ouid, "ouid" -> uid)
            )
          )
          ret = findCollectionOne[Session](sessionsCollection, selector).map {s => (s, "")}
        } else {
          errmsg = "send user or recv user not exist"
          ret = Future(null, errmsg)
        }
        ret
      }
      (sessionid, errmsg) <- {
        var ret = Future("", errmsgUserNotExist)
        if (errmsgUserNotExist == "") {
          if (session != null) {
            ret = Future(session._id, "")
          } else {
            val newSession = Session("", createuid = uid, ouid = ouid, sessionName = "", sessionIcon = "", sessionType = 0, publicType = 0)
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
                  joinSession(ouid, sessionid)
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
  def getJoinedUsers(sessionid: String): Future[(Session, List[User])] = {
    for {
      session <- findCollectionOne[Session](sessionsCollection, document("_id" -> sessionid))
      users <- {
        var users = Future(List[User]())
        if (session != null) {
          val uids = session.usersStatus.map(_.uid)
          val selector = document("_id" -> document("$in" -> uids))
          users = findCollection[User](usersCollection, selector)
        }
        users
      }
    } yield {
      (session, users)
    }
  }

  //join new session
  def joinSession(uid: String, sessionid: String)(implicit notificationActor: ActorRef): Future[UpdateResult] = {
    var errmsg = ""
    for {
      user <- findCollectionOne[User](usersCollection, document("_id" -> uid, "sessionsStatus.sessionid" -> document("$ne" -> sessionid)))
      session <- findCollectionOne[Session](sessionsCollection, document("_id" -> sessionid))
      updateResult <- {
        if (user == null) {
          errmsg = "user not exists or already join session"
        }
        if (session == null) {
          errmsg = "session not exists"
        }
        var ret = Future(UpdateResult(n = 0, errmsg = errmsg))
        if (errmsg == "") {
          ret = for {
            ur1 <- {
              val docSessionStatus = document("sessionid" -> sessionid, "newCount" -> 0)
              val update1 = document("$push" -> document("sessionsStatus" -> docSessionStatus))
              updateCollection(usersCollection, document("_id" -> uid), update1)
            }
            ur2 <- {
              val docUserStatus = document("uid" -> uid, "online" -> false)
              val update2 = document("$push" -> document("usersStatus" -> docUserStatus))
              updateCollection(sessionsCollection, document("_id" -> sessionid), update2)
            }
          } yield {
            val nickname = user.nickname
            val avatar = user.avatar
            val sessionName = session.sessionName
            val sessionIcon = session.sessionIcon
            val msgType = "join"
            val content = s"$nickname join session $sessionName"
            val dateline = timeToStr(System.currentTimeMillis())
            notificationActor ! WsTextDown(uid, nickname, avatar, sessionid, sessionName, sessionIcon, msgType, content, dateline)
            ur2
          }
        }
        ret
      }
    } yield {
      updateResult
    }
  }

  //join a group session
  def joinGroupSession(uid: String, sessionid: String)(implicit notificationActor: ActorRef): Future[UpdateResult] = {
    for {
      session <- findCollectionOne[Session](sessionsCollection, document("_id" -> sessionid))
      updateResult <- {
        var errmsg = ""
        if (session == null) {
          errmsg = "session not exist"
        } else {
          if (session.sessionType == 0) {
            errmsg = "not join a group session"
          }
        }
        if (errmsg == "") {
          joinSession(uid, sessionid)
        } else {
          Future(UpdateResult(n = 0, errmsg = errmsg))
        }
      }
    } yield {
      updateResult
    }
  }

  //leave session
  def leaveSession(uid: String, sessionid: String)(implicit notificationActor: ActorRef): Future[UpdateResult] = {
    for {
      user <- findCollectionOne[User](usersCollection, document("_id" -> uid, "sessionsStatus.sessionid" -> sessionid))
      session <- findCollectionOne[Session](sessionsCollection, document("_id" -> sessionid, "usersStatus.uid" -> uid))
      ret <- {
        if (user == null || session == null) {
          val errmsg = "user not exists or not join the session"
          Future(UpdateResult(n = 0, errmsg = errmsg))
        } else {
          for {
            ur1 <- {
              val sessionstatus = user.sessionsStatus.filter(_.sessionid == sessionid).head
              val docSessionStatus = document("sessionid" -> sessionstatus.sessionid, "newCount" -> sessionstatus.newCount)
              val update1 = document("$pull" -> document("sessionsStatus" -> docSessionStatus))
              updateCollection(usersCollection, document("_id" -> uid), update1)
            }
            ur2 <- {
              val userstatus = session.usersStatus.filter(_.uid == uid).head
              val docUserStatus = document("uid" -> userstatus.uid, "online" -> userstatus.online)
              val update2 = document("$pull" -> document("usersStatus" -> docUserStatus))
              updateCollection(sessionsCollection, document("_id" -> sessionid), update2)
            }
          } yield {
            val nickname = user.nickname
            val avatar = user.avatar
            val sessionName = session.sessionName
            val sessionIcon = session.sessionIcon
            val msgType = "leave"
            val content = s"$nickname leave session $sessionName"
            val dateline = timeToStr(System.currentTimeMillis())
            notificationActor ! WsTextDown(uid, nickname, avatar, sessionid, sessionName, sessionIcon, msgType, content, dateline)
            ur2
          }
        }
      }
    } yield {
      ret
    }
  }

  def leaveGroupSession(uid: String, sessionid: String)(implicit notificationActor: ActorRef) = {
    for {
      session <- findCollectionOne[Session](sessionsCollection, document("_id" -> sessionid))
      updateResult <- {
        var errmsg = ""
        if (session == null) {
          errmsg = "session not exist"
        } else {
          if (session.sessionType == 0) {
            errmsg = "not a group session"
          } else if (session.createuid == uid) {
            errmsg = "creator can not leave your own session"
          }
        }
        if (errmsg == "") {
          leaveSession(uid, sessionid)
        } else {
          Future(UpdateResult(n = 0, errmsg = errmsg))
        }
      }
    } yield {
      updateResult
    }
  }

  //list public and joined session
  def listSessions(uid: String, isPublic: Boolean): Future[List[(Session, SessionStatus)]] = {
    for {
      user <- findCollectionOne[User](usersCollection, document("_id" -> uid))
      sessionInfoList <- {
        if (user != null) {
          if (isPublic) {
            val sessionids = user.sessionsStatus.map(_.sessionid)
            var ba = array()
            sessionids.foreach { sessionid =>
              ba = ba.merge(sessionid)
            }
            val selector = document(
              "publicType" -> 1,
              "sessionType" -> 1,
              "_id" -> document(
                "$nin" -> ba
              )
            )
            val sort = document("lastUpdate" -> -1)
            findCollection[Session](sessionsCollection, selector, sort = sort).map { sessions =>
              sessions.map { session =>
                val sessionStatus = user.sessionsStatus.find(_.sessionid == session._id).getOrElse(SessionStatus("", 0))
                (session, sessionStatus)
              }
            }
          } else {
            Future.sequence(
              user.sessionsStatus.map { sessionStatus =>
                findCollectionOne[Session](sessionsCollection, document("_id" -> sessionStatus.sessionid)).map { session =>
                  (session, sessionStatus)
                }
              }
            ).map { sessions => sessions.sortBy{ case (session, sessionStatus) => session.lastUpdate * -1}}
          }
        } else {
          Future(List[(Session, SessionStatus)]())
        }
      }
      sessions <- {
        Future.sequence(
          sessionInfoList.map { case (session, sessionStatus) =>
            getSessionNameIcon(uid, session._id).map { sessionToken =>
              session.sessionName = sessionToken.sessionName
              session.sessionIcon = sessionToken.sessionIcon
              (session, sessionStatus)
            }
          }
        )
      }
    } yield {
      sessions
    }
  }

  def listJoinedSessions(uid: String): Future[List[(Session, SessionStatus)]] = {
    for {
      user <- findCollectionOne[User](usersCollection, document("_id" -> uid))
      sessionInfoList <- {
        if (user != null) {
          Future.sequence(
            user.sessionsStatus.map { sessionStatus =>
              findCollectionOne[Session](sessionsCollection, document("_id" -> sessionStatus.sessionid)).map { session =>
                getSessionNameIcon(uid, session._id).map { sessionToken =>
                  session.sessionName = sessionToken.sessionName
                  session.sessionIcon = sessionToken.sessionIcon
                  (session, sessionStatus)
                }
              }.flatMap(t => t)
            }
          ).map{ sessions =>
            sessions.sortBy{ case (session, sessionStatus) => session.lastUpdate * -1 }
          }
        } else {
          Future(List[(Session, SessionStatus)]())
        }
      }
    } yield {
      sessionInfoList
    }
  }

  def getNewNotificationCount(uid: String): Future[(Int, String)] = {
    for {
      user <- findCollectionOne[User](usersCollection, document("_id" -> uid))
      (rsCount, errmsg) <- {
        if (user != null) {
          countCollection(notificationsCollection, document("recvuid" -> uid, "isRead" -> 0)).map { rsCount =>
            (rsCount, "")
          }
        } else {
          Future(0, "user not exists")
        }
      }
    } yield {
      (rsCount, errmsg)
    }
  }

  //verify user is in session
  def verifySession(senduid: String, sessionid: String): Future[String] = {
    for {
      user <- findCollectionOne[User](usersCollection, document("_id" -> senduid, "sessionsStatus.sessionid" -> sessionid))
      session <- findCollectionOne[Session](sessionsCollection, document("_id" -> sessionid, "usersStatus.uid" -> senduid))
    } yield {
      if (user != null && session != null) {
        ""
      } else {
        "no privilege in this session"
      }
    }
  }

  //create a new message
  def createMessage(uid: String, sessionid: String, msgType: String, content: String = "", filePath: String = "", fileName: String = "", fileSize: Long = 0L, fileType: String = "", fileThumb: String = ""): Future[(String, String)] = {
    val fileInfo = FileInfo(filePath, fileName, fileSize, fileType, fileThumb)
    val message = Message("", uid, sessionid, msgType, content, fileInfo)
    for {
      (msgid, errmsg) <- insertCollection[Message](messagesCollection, message)
      session <- {
        if (msgid != "") {
          findCollectionOne[Session](sessionsCollection, document("_id" -> sessionid))
        } else {
          Future(null)
        }
      }
      updateLastMsgId <- {
        if (session != null) {
          val selector = document("_id" -> sessionid)
          val update = document("$set" ->
            document(
              "lastMsgid" -> msgid,
              "lastUpdate" -> System.currentTimeMillis()
            )
          )
          updateCollection(sessionsCollection, selector, update)
        } else {
          Future(UpdateResult(n = 0, errmsg = "nothing to update"))
        }
      }
      updateNewCounts <- {
        if (session != null) {
          Future.sequence(
            //update not online users newCount
            session.usersStatus.filterNot(_.online).map { userstatus =>
              //update userstatus nest array
              val selector = document(
                "_id" -> userstatus.uid,
                "sessionsStatus.sessionid" -> sessionid
              )
              val update = document(
                "$inc" -> document(
                  "sessionsStatus.$.newCount" -> 1
                )
              )
              updateCollection(usersCollection, selector, update)
            }
          )
        } else {
          Future(List[UpdateResult]())
        }
      }
    } yield {
      (msgid, errmsg)
    }
  }

  def createNotification(noticeType: String, senduid: String, recvuid: String, sessionid: String): Future[(String, String)] = {
    var errmsg = ""
    if (senduid == "" || recvuid == "") {
      errmsg = "senduid or recvuid is empty"
    } else if (noticeType != "joinFriend" && noticeType != "removeFriend" && noticeType != "inviteSession") {
      errmsg = "noticeType error"
    } else if (noticeType == "inviteSession" && sessionid == "") {
      errmsg = "inviteSession must provide sessionid"
    }
    if (errmsg != "") {
      Future("", errmsg)
    } else {
      val notificationNew = Notification("", noticeType, senduid, recvuid, sessionid)
      insertCollection[Notification](notificationsCollection, notificationNew)
    }
  }

  def listNotifications(uid: String, page: Int = 10, count: Int = 1) = {
    val selector = document("recvuid" -> uid)
    val sort = document("dateline" -> -1)
    for {
      notifications <- findCollection[Notification](notificationsCollection, selector, sort = sort, page = page, count = count)
      results <- {
        Future.sequence(
          notifications.map { notification =>
            val senduserFuture = findCollectionOne[User](usersCollection, document("_id" -> notification.senduid))
            var sessionFuture: Future[Session] = Future(null)
            if (notification.sessionid != "") {
              sessionFuture = findCollectionOne[Session](sessionsCollection, document("_id" -> notification.sessionid))
            }
            for {
              updateResult <- updateCollection(notificationsCollection, document("_id" -> notification._id), document("$set" -> document("isRead" -> 1)))
              senduser <- senduserFuture
              session <- sessionFuture
            } yield {
              (notification, senduser, session)
            }
          }
        )
      }
    } yield {
      results
    }
  }

  def userOnlineOffline(uid: String, sessionid: String, isOnline: Boolean): Future[UpdateResult] = {
    val selector = document(
      "_id" -> sessionid,
      "usersStatus.uid" -> uid
    )
    val update = document(
      "$set" -> document(
        "usersStatus.$.online" -> isOnline
      )
    )
    updateCollection(sessionsCollection, selector, update)
  }

  def getSessionLastMessage(userTokenStr: String, sessionid: String): Future[(Session, Message, User)] = {
    val UserToken(uid, nickname, avatar) = verifyUserToken(userTokenStr)
    if (uid != "") {
      for {
        session <- findCollectionOne[Session](sessionsCollection, document("_id" -> sessionid))
        message <- {
          if (session != null) {
            findCollectionOne[Message](messagesCollection, document("_id" -> session.lastMsgid))
          } else {
            null
          }
        }
        user <- {
          if (message != null) {
            findCollectionOne[User](usersCollection, document("_id" -> message.uid))
          } else {
            Future(null)
          }
        }
      } yield {
        (session, message, user)
      }
    } else {
      Future(null, null, null)
    }
  }

  //list history messages
  def listHistoryMessages(uid: String, sessionid: String, page: Int = 1, count: Int = 10, sort: BSONDocument): Future[(String, List[(Message, User)])] = {
    for {
      errmsg <- verifySession(uid, sessionid)
      messages <- {
        var messages = Future(List[Message]())
        if (errmsg == "") {
          messages = findCollection[Message](messagesCollection, document("sessionid" -> sessionid), sort = sort, page = page, count = count)
        }
        messages
      }
      updateNewCount <- {
        if (messages.nonEmpty) {
          val selector = document(
            "_id" -> uid,
            "sessionsStatus.sessionid" -> sessionid
          )
          val update = document(
            "$set" -> document(
              "sessionsStatus.$.newCount" -> 0
            )
          )
          updateCollection(usersCollection, selector, update)
        } else {
          Future(UpdateResult(n = 0, errmsg = "nothing to update"))
        }
      }
      listMessageUser <- {
        Future.sequence(
          messages.map { message =>
            findCollectionOne[User](usersCollection, document("_id" -> message.uid)).map { user =>
              (message, user)
            }
          }
        )
      }
    } yield {
      (errmsg, listMessageUser)
    }
  }

  //create user token, include uid, nickname, avatar
  def createUserToken(uid: String): Future[String] = {
    for {
      user <- findCollectionOne[User](usersCollection, document("_id" -> uid))
      onlineUpdate <- {
        if (user != null) {
          updateOnline(uid)
        } else {
          Future("online not update")
        }
      }
      updateLastLogin <- {
        if (user != null) {
          updateCollection(
            usersCollection,
            document("_id" -> uid),
            document("$set" -> document("lastLogin" -> System.currentTimeMillis()))
          )
        } else {
          Future(UpdateResult(n = 0, errmsg = "nothing to update"))
        }
      }
    } yield {
      var token = ""
      if (user != null) {
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
      sessionToken <- {
        if (errmsg == "") {
          findCollectionOne[Session](sessionsCollection, document("_id" -> sessionid)).map { session =>
            if (session != null) {
              SessionToken(sessionid, session.sessionName, session.sessionIcon)
            } else {
              SessionToken("", "", "")
            }
          }
        } else {
          Future(SessionToken("", "", ""))
        }
      }
    } yield {
      var token = ""
      if (sessionToken.sessionid != "") {
        val payload = Map[String, Any](
          "sessionid" -> sessionToken.sessionid,
          "sessionName" -> sessionToken.sessionName,
          "sessionIcon" -> sessionToken.sessionIcon
        )
        token = encodeJwt(payload)
      }
      token
    }
  }

  def verifySessionToken(token: String): SessionToken = {
    var sessionToken = SessionToken("", "", "")
    val mapSessionToken = decodeJwt(token)
    if (mapSessionToken.contains("sessionid")) {
      val sessionid = mapSessionToken("sessionid").asInstanceOf[String]
      val sessionName = mapSessionToken("sessionName").asInstanceOf[String]
      val sessionIcon = mapSessionToken("sessionIcon").asInstanceOf[String]
      if (sessionid != "") {
        sessionToken = SessionToken(sessionid, sessionName, sessionIcon)
      }
    }
    sessionToken
  }

  def verifyUserSessionToken(userTokenStr: String, sessionTokenStr: String): UserSessionInfo = {
    val userToken = verifyUserToken(userTokenStr)
    val sessionToken = verifySessionToken(sessionTokenStr)
    if (userToken.uid != "" && userToken.nickname != "" && userToken.avatar != "" && sessionToken.sessionid != "") {
      UserSessionInfo(userToken.uid, userToken.nickname, userToken.avatar, sessionToken.sessionid, sessionToken.sessionName, sessionToken.sessionIcon)
    } else {
      UserSessionInfo("", "", "", "", "", "")
    }
  }

  def getSessionNameIcon(uid: String, sessionid: String): Future[SessionToken] = {
    for {
      session <- findCollectionOne[Session](sessionsCollection, document("_id" -> sessionid))
      sessionToken <- {
        var futureSessionToken = Future(SessionToken("", "", ""))
        if (session != null) {
          if (session.sessionType == 1) {
            //group session
            futureSessionToken = Future(SessionToken(session._id, session.sessionName, session.sessionIcon))
          } else {
            //private session
            if (session.usersStatus.nonEmpty) {
              val ouid = session.usersStatus.filter(_.uid != uid).map(_.uid).head
              futureSessionToken = findCollectionOne[User](usersCollection, document("_id" -> ouid)).map { ouser =>
                if (ouser != null) {
                  SessionToken(session._id, ouser.nickname, ouser.avatar)
                } else {
                  SessionToken("", "", "")
                }
              }
            }
          }
        }
        futureSessionToken
      }
    } yield {
      sessionToken
    }
  }

  def getSessionHeader(uid: String, sessionid: String): Future[(Session, SessionToken)] = {
    for {
      session <- findCollectionOne[Session](sessionsCollection, document("_id" -> sessionid))
      sessionToken <- getSessionNameIcon(uid, sessionid)
    } yield {
      (session, sessionToken)
    }
  }

  def getSessionMenu(uid: String, sessionid: String): Future[(Session, Boolean, Boolean)] = {
    for {
      session <- findCollectionOne[Session](sessionsCollection, document("_id" -> sessionid))
      user <- findCollectionOne[User](usersCollection, document("_id" -> uid))
    } yield {
      if (session != null && user != null) {
        val joined = session.usersStatus.map(_.uid).contains(uid)
        val editable = session.createuid == uid
        (session, joined, editable)
      } else {
        (null, false, false)
      }
    }
  }

  def getUserMenu(uid: String, ouid: String): Future[(User, Boolean)] = {
    for {
      user <- findCollectionOne[User](usersCollection, document("_id" -> uid))
      ouser <- findCollectionOne[User](usersCollection, document("_id" -> ouid))
    } yield {
      if (user != null && ouser != null) {
        val isFriend = user.friends.contains(ouid)
        (ouser, isFriend)
      } else {
        (null, false)
      }
    }
  }

  def generateNewGroupSession(uid: String, friends: List[String]): Future[(String, List[String])] = {
    val uids = (uid +: friends).take(4)
    for {
      users <- findCollection[User](usersCollection, document("_id" -> document("$in" -> uids)))
    } yield {
      val sessionName = users.map(_.nickname).mkString(", ").take(30)
      val sessionIcons = users.map(_.avatar)
      (sessionName, sessionIcons)
    }
  }

}
