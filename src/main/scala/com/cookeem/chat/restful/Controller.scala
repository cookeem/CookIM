package com.cookeem.chat.restful

import java.io.File
import java.text.SimpleDateFormat
import java.util.UUID

import akka.actor.ActorRef
import com.cookeem.chat.common.CommonUtils.{timeToStr, _}
import com.cookeem.chat.event.ChatMessage
import com.cookeem.chat.mongo.FileInfo
import com.cookeem.chat.mongo.MongoLogic.{getSessionMenu, _}
import com.sksamuel.scrimage.{Color, Image}
import com.sksamuel.scrimage.nio.PngWriter
import org.apache.commons.io.FileUtils
import play.api.libs.json._
import reactivemongo.bson._

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by cookeem on 16/11/2.
  */
object Controller {
  def registerUserCtl(login: String, nickname: String, password: String, repassword: String, gender: Int)(implicit ec: ExecutionContext): Future[JsObject] = {
    if (password != repassword) {
      Future {
        Json.obj(
          "uid" -> "",
          "errmsg" -> s"password and repassword must be same",
          "successmsg" -> "",
          "userToken" -> ""
        )
      }
    } else {
      val avatar = gender match {
        case 1 => "images/avatar/boy.jpg"
        case 2 => "images/avatar/girl.jpg"
        case _ => "images/avatar/unknown.jpg"
      }
      registerUser(login, nickname, password, gender, avatar).map { case (uid, userTokenStr, errmsg) =>
        var successmsg = ""
        if (uid != "") {
          successmsg = "register user success, thank you for join us"
        }
        Json.obj(
          "uid" -> uid,
          "errmsg" -> errmsg,
          "successmsg" -> successmsg,
          "userToken" -> userTokenStr
        )
      }
    }
  }

  def createUserTokenCtl(userTokenStr: String)(implicit ec: ExecutionContext): Future[JsObject] = {
    val userToken = verifyUserToken(userTokenStr)
    if (userToken.uid == "") {
      Future(
        Json.obj(
          "errmsg" -> "no privilege to create user token",
          "uid" -> "",
          "userToken" -> ""
        )
      )
    } else {
      val uid = userToken.uid
      createUserToken(uid).map { newUserTokenStr =>
        if (newUserTokenStr == "") {
          Json.obj(
            "errmsg" -> "no privilege to create user token",
            "uid" -> "",
            "userToken" -> ""
          )
        } else {
          Json.obj(
            "errmsg" -> "",
            "uid" -> uid,
            "userToken" -> newUserTokenStr
          )
        }
      }
    }
  }

  def createSessionTokenCtl(userTokenStr: String, sessionid: String)(implicit ec: ExecutionContext): Future[JsObject] = {
    val userToken = verifyUserToken(userTokenStr)
    if (userToken.uid == "") {
      Future(
        Json.obj(
          "errmsg" -> "no privilege to create session token",
          "sessionToken" -> ""
        )
      )
    } else {
      val uid = userToken.uid
      createSessionToken(uid, sessionid).map { sessionTokenStr =>
        if (sessionTokenStr == "") {
          Json.obj(
            "errmsg" -> "no privilege to create session token",
            "sessionToken" -> ""
          )
        } else {
          Json.obj(
            "errmsg" -> "",
            "sessionToken" -> sessionTokenStr
          )
        }
      }
    }
  }

  def verifyUserTokenCtl(userTokenStr: String)(implicit ec: ExecutionContext): Future[JsObject] = {
    val userToken = verifyUserToken(userTokenStr)
    Future(
      Json.obj(
        "uid" -> userToken.uid,
        "nickname" -> userToken.nickname,
        "avatar" -> userToken.avatar
      )
    )
  }

  def loginCtl(login: String, password: String)(implicit ec: ExecutionContext): Future[JsObject] = {
    var errmsg = ""
    if (password.length < 6) {
      Future {
        Json.obj(
          "uid" -> "",
          "errmsg" -> s"password must at least 6 characters",
          "successmsg" -> "",
          "userToken" -> ""
        )
      }
    } else {
      loginAction(login, password).map { case (uid, userTokenStr) =>
        var successmsg = ""
        if (uid != "") {
          successmsg = "login in success"
        } else {
          errmsg = "user not exist or password not match"
        }
        Json.obj(
          "uid" -> uid,
          "errmsg" -> errmsg,
          "successmsg" -> successmsg,
          "userToken" -> userTokenStr
        )
      }
    }
  }

  def logoutCtl(userTokenStr: String)(implicit ec: ExecutionContext): Future[JsObject] = {
    logoutAction(userTokenStr).map { updateResult =>
      if (updateResult.errmsg != "") {
        Json.obj(
          "errmsg" -> updateResult.errmsg,
          "successmsg" -> ""
        )
      } else {
        Json.obj(
          "errmsg" -> updateResult.errmsg,
          "successmsg" -> "logout success"
        )
      }
    }
  }

  def updateUserInfoCtl(userTokenStr: String, nickname: String = "", gender: Int = 0, avatar: String = "")(implicit ec: ExecutionContext): Future[JsObject] = {
    val userToken = verifyUserToken(userTokenStr)
    if (userToken.uid == "") {
      Future(
        Json.obj(
          "errmsg" -> "no privilege to update user info",
          "successmsg" -> ""
        )
      )
    } else {
      val uid = userToken.uid
      updateUserInfo(uid, nickname, gender, avatar).map { updateResult =>
        if (updateResult.errmsg != "") {
          Json.obj(
            "errmsg" -> updateResult.errmsg,
            "successmsg" -> ""
          )
        } else {
          Json.obj(
            "errmsg" -> "",
            "successmsg" -> "update user info success"
          )
        }
      }
    }
  }

  def changePwdCtl(userTokenStr: String, oldPwd: String, newPwd: String, renewPwd: String)(implicit ec: ExecutionContext): Future[JsObject] = {
    val userToken = verifyUserToken(userTokenStr)
    if (userToken.uid == "") {
      Future(
        Json.obj(
          "errmsg" -> "no privilege to update user info",
          "successmsg" -> ""
        )
      )
    } else {
      val uid = userToken.uid
      changePwd(uid, oldPwd, newPwd, renewPwd).map { updateResult =>
        if (updateResult.errmsg != "") {
          Json.obj(
            "errmsg" -> updateResult.errmsg,
            "successmsg" -> ""
          )
        } else {
          Json.obj(
            "errmsg" -> "",
            "successmsg" -> "change password success"
          )
        }
      }
    }
  }

  def getUserInfoCtl(userTokenStr: String, uid: String)(implicit ec: ExecutionContext): Future[JsObject] = {
    getUserInfo(uid).map { user =>
      if (user == null) {
        Json.obj(
          "errmsg" -> "user not exist",
          "successmsg" -> "",
          "userInfo" -> JsNull
        )
      } else {
        val userToken = verifyUserToken(userTokenStr)
        var login = ""
        if (uid == userToken.uid) {
          login = user.login
        }
        Json.obj(
          "errmsg" -> "",
          "successmsg" -> "get user info success",
          "userInfo" -> Json.obj(
            "uid" -> user._id,
            "nickname" -> user.nickname,
            "avatar" -> user.avatar,
            "gender" -> user.gender,
            "login" -> login,
            "lastLogin" -> timeToStr(user.lastLogin),
            "loginCount" -> user.loginCount
          )
        )
      }
    }
  }

  def createGroupSessionCtl(userTokenStr: String, sessionName: String, sessionIcon: String, publicType: Int)(implicit ec: ExecutionContext, notificationActor: ActorRef): Future[JsObject] = {
    val userToken = verifyUserToken(userTokenStr)
    if (userToken.uid == "") {
      Future(
        Json.obj(
          "sessionid" -> "",
          "errmsg" -> "no privilege to create group session",
          "successmsg" -> ""
        )
      )
    } else {
      val uid = userToken.uid
      createGroupSession(uid, sessionName, sessionIcon, publicType).map { case (sessionid, errmsg) =>
        if (errmsg != "") {
          Json.obj(
            "sessionid" -> sessionid,
            "errmsg" -> errmsg,
            "successmsg" -> ""
          )
        } else {
          Json.obj(
            "sessionid" -> sessionid,
            "errmsg" -> errmsg,
            "successmsg" -> "create group session success"
          )
        }
      }
    }
  }

  def getEditGroupSessionInfoCtl(userTokenStr: String, sessionid: String)(implicit ec: ExecutionContext): Future[JsObject] = {
    val userToken = verifyUserToken(userTokenStr)
    if (userToken.uid == "") {
      Future(
        Json.obj(
          "errmsg" -> "no privilege to get group session info",
          "session" -> JsNull
        )
      )
    } else {
      val uid = userToken.uid
      getEditGroupSessionInfo(uid, sessionid).map { session =>
        if (session != null) {
          Json.obj(
            "errmsg" -> "",
            "session" -> Json.obj(
              "sessionName" -> session.sessionName,
              "sessionIcon" -> session.sessionIcon,
              "publicType" -> session.publicType
            )
          )
        } else {
          Json.obj(
            "errmsg" -> "no privilege to get group session info",
            "session" -> JsNull
          )
        }
      }
    }
  }

  def editGroupSessionCtl(userTokenStr: String, sessionid: String, sessionName: String, sessionIcon: String, publicType: Int)(implicit ec: ExecutionContext): Future[JsObject] = {
    val userToken = verifyUserToken(userTokenStr)
    if (userToken.uid == "") {
      Future(
        Json.obj(
          "errmsg" -> "no privilege to edit group session",
          "successmsg" -> ""
        )
      )
    } else {
      val uid = userToken.uid
      editGroupSession(uid, sessionid, sessionName, sessionIcon, publicType).map { errmsg =>
        if (errmsg != "") {
          Json.obj(
            "errmsg" -> errmsg,
            "successmsg" -> ""
          )
        } else {
          Json.obj(
            "errmsg" -> errmsg,
            "successmsg" -> "edit group session success"
          )
        }
      }
    }
  }

  def listSessionsCtl(userTokenStr: String, isPublic: Boolean)(implicit ec: ExecutionContext): Future[JsObject] = {
    val userToken = verifyUserToken(userTokenStr)
    if (userToken.uid == "") {
      Future(
        Json.obj(
          "errmsg" -> "no privilege to list sessions",
          "sessions" -> JsArray()
        )
      )
    } else {
      val uid = userToken.uid
      listSessions(uid, isPublic).map { sessionInfoList =>
        Future.sequence(
          sessionInfoList.map { case (session, sessionStatus) =>
            getSessionLastMessage(userTokenStr, session._id).map { case (sessionLast, messageLast, userLast) =>
              var jsonMessage: JsValue = JsNull
              if (messageLast != null && userLast != null) {
                var content = messageLast.content
                if (messageLast.fileInfo.fileThumb != "") {
                  content = "send a [PHOTO]"
                } else if (messageLast.fileInfo.filePath != "") {
                  content = "send a [FILE]"
                }
                jsonMessage = Json.obj(
                  "uid" -> userLast._id,
                  "nickname" -> userLast.nickname,
                  "avatar" -> userLast.avatar,
                  "msgType" -> messageLast.msgType,
                  "content" -> content,
                  "dateline" -> timeToStr(messageLast.dateline)
                )
              }
              Json.obj(
                "sessionid" -> session._id,
                "createuid" -> session.createuid,
                "ouid" -> session.ouid,
                "sessionName" -> session.sessionName.take(30),
                "sessionType" -> session.sessionType,
                "sessionIcon" -> session.sessionIcon,
                "publicType" -> session.publicType,
                "lastUpdate" -> timeToStr(session.lastUpdate),
                "dateline" -> timeToStr(session.dateline),
                "newCount" -> sessionStatus.newCount,
                "message" -> jsonMessage
              )
            }
          }
        )
      }.flatMap(t => t).map { sessions =>
        Json.obj(
          "errmsg" -> "",
          "sessions" -> sessions
        )
      }
    }
  }

  def listJoinedSessionsCtl(userTokenStr: String)(implicit ec: ExecutionContext): Future[JsObject] = {
    val userToken = verifyUserToken(userTokenStr)
    if (userToken.uid == "") {
      Future(
        Json.obj(
          "errmsg" -> "no privilege to list sessions",
          "sessions" -> JsArray()
        )
      )
    } else {
      val uid = userToken.uid
      listJoinedSessions(uid).map { sessionInfoList =>
        val sessions = sessionInfoList.map { case (session, sessionStatus) =>
          Json.obj(
            "sessionid" -> session._id,
            "createuid" -> session.createuid,
            "sessionName" -> trimUtf8(session.sessionName, 12),
            "sessionType" -> session.sessionType,
            "sessionIcon" -> session.sessionIcon,
            "publicType" -> session.publicType,
            "dateline" -> timeToStr(session.dateline),
            "lastUpdate" -> timeToStr(session.lastUpdate),
            "newCount" -> sessionStatus.newCount
          )
        }
        Json.obj(
          "errmsg" -> "",
          "sessions" -> sessions
        )
      }
    }
  }

  def getNewNotificationCountCtl(userTokenStr: String)(implicit ec: ExecutionContext): Future[JsObject] = {
    val userToken = verifyUserToken(userTokenStr)
    if (userToken.uid == "") {
      Future(
        Json.obj(
          "errmsg" -> "no privilege to get new notification count",
          "rsCount" -> 0
        )
      )
    } else {
      val uid = userToken.uid
      getNewNotificationCount(uid).map { case (rsCount, errmsg) =>
        Json.obj(
          "errmsg" -> errmsg,
          "rsCount" -> rsCount
        )
      }
    }
  }

  def listMessagesCtl(userTokenStr: String, sessionid: String, page: Int = 1, count: Int = 10)(implicit ec: ExecutionContext): Future[JsObject] = {
    implicit val fileInfoWrites = Json.writes[FileInfo]
    implicit val chatMessageWrites = Json.writes[ChatMessage]
    val userToken = verifyUserToken(userTokenStr)
    if (userToken.uid == "") {
      Future(
        Json.obj(
          "errmsg" -> "no privilege to list session messages",
          "sessionToken" -> "",
          "messages" -> JsArray()
        )
      )
    } else {
      val uid = userToken.uid
      for {
        sessionTokenStr <- createSessionToken(uid, sessionid)
        ret <- {
          listHistoryMessages(uid, sessionid, page, count, sort = document("dateline" -> -1)).map { case (errmsg, messageUsers) =>
            var token = ""
            if (errmsg == "") {
              token = sessionTokenStr
            }
            Json.obj(
              "errmsg" -> errmsg,
              "sessionToken" -> token,
              "messages" -> messageUsers.reverse.map { case (message, user) =>
                var suid = ""
                var snickname = ""
                var savatar = ""
                if (user != null) {
                  suid = user._id
                  snickname = user.nickname
                  savatar = user.avatar
                }
                val chatMessage = ChatMessage(suid, snickname, savatar, message.msgType, message.content, message.fileInfo, timeToStr(message.dateline))
                Json.toJson(chatMessage)
              }
            )
          }
        }
      } yield {
        ret
      }
    }
  }

  def joinGroupSessionCtl(userTokenStr: String, sessionid: String)(implicit ec: ExecutionContext, notificationActor: ActorRef): Future[JsObject] = {
    val userToken = verifyUserToken(userTokenStr)
    if (userToken.uid == "") {
      Future(
        Json.obj(
          "errmsg" -> "no privilege to join session",
          "sessionToken" -> ""
        )
      )
    } else {
      val uid = userToken.uid
      for {
        updateResult <- joinGroupSession(uid, sessionid)
        json <- {
          if (updateResult.errmsg != "") {
            Future(
              Json.obj(
                "errmsg" -> updateResult.errmsg,
                "sessionToken" -> ""
              )
            )
          } else {
            createSessionToken(uid, sessionid).map { sessionTokenStr =>
              Json.obj(
                "errmsg" -> updateResult.errmsg,
                "sessionToken" -> sessionTokenStr
              )
            }
          }
        }
      } yield {
        json
      }
    }
  }

  def leaveGroupSessionCtl(userTokenStr: String, sessionid: String)(implicit ec: ExecutionContext, notificationActor: ActorRef): Future[JsObject] = {
    val userToken = verifyUserToken(userTokenStr)
    if (userToken.uid == "") {
      Future(
        Json.obj(
          "errmsg" -> "no privilege to leave session"
        )
      )
    } else {
      val uid = userToken.uid
      leaveGroupSession(uid, sessionid).map { updateResult =>
        Json.obj(
          "errmsg" -> updateResult.errmsg
        )
      }
    }
  }

  def getJoinedUsersCtl(userTokenStr: String, sessionid: String)(implicit ec: ExecutionContext): Future[JsObject] = {
    val userToken = verifyUserToken(userTokenStr)
    if (userToken.uid == "") {
      Future(
        Json.obj(
          "errmsg" -> "no privilege to get joined users",
          "onlineUsers" -> JsArray(),
          "offlineUsers" -> JsArray()
        )
      )
    } else {
      val uid = userToken.uid
      getJoinedUsers(sessionid).map { case (session, users) =>
        if (session != null) {
          val onlineUsers = session.usersStatus.filter(_.online).map(_.uid).map { uid =>
            users.find(_._id == uid).orNull
          }.filter(_ != null)
          val offlineUsers = session.usersStatus.filterNot(_.online).map(_.uid).map { uid =>
            users.find(_._id == uid).orNull
          }.filter(_ != null)
          Json.obj(
            "errmsg" -> "",
            "onlineUsers" -> onlineUsers.map { user =>
              Json.obj(
                "uid" -> user._id,
                "nickname" -> user.nickname,
                "avatar" -> user.avatar
              )
            },
            "offlineUsers" -> offlineUsers.map { user =>
              Json.obj(
                "uid" -> user._id,
                "nickname" -> user.nickname,
                "avatar" -> user.avatar
              )
            }
          )
        } else {
          Json.obj(
            "errmsg" -> "session not exist",
            "onlineUsers" -> JsArray(),
            "offlineUsers" -> JsArray()
          )
        }
      }
    }
  }

  def getFriendsCtl(userTokenStr: String)(implicit ec: ExecutionContext) = {
    val userToken = verifyUserToken(userTokenStr)
    if (userToken.uid == "") {
      Future(
        Json.obj(
          "errmsg" -> "no privilege to get friends",
          "friends" -> JsArray()
        )
      )
    } else {
      val uid = userToken.uid
      listFriends(uid).map { users =>
        Json.obj(
          "errmsg" -> "",
          "friends" -> users.map { user =>
            val gender = user.gender match {
              case 1 => "boy"
              case 2 => "girl"
              case _ => "unknown"
            }
            Json.obj(
              "uid" -> user._id,
              "nickname" -> user.nickname,
              "avatar" -> user.avatar,
              "gender" -> gender,
              "dateline" -> timeToStr(user.dateline)
            )
          }
        )
      }
    }
  }

  def inviteFriendsCtl(userTokenStr: String, sessionid: String, friendsStr: String, ouid: String)(implicit ec: ExecutionContext, notificationActor: ActorRef): Future[JsObject] = {
    val userToken = verifyUserToken(userTokenStr)
    if (userToken.uid == "") {
      Future(
        Json.obj(
          "errmsg" -> "no privilege to invite friends",
          "successmsg" -> "",
          "sessionid" -> ""
        )
      )
    } else {
      val uid = userToken.uid
      var friends = List[String]()
      try {
        friends = Json.parse(friendsStr).as[List[String]]
      } catch { case e: Throwable =>
          consoleLog("ERROR", s"friends string parse to json error: $e")
      }
      if (friends.isEmpty) {
        Future(
          Json.obj(
            "errmsg" -> "please select friends to invite",
            "successmsg" -> "",
            "sessionid" -> ""
          )
        )
      } else {
        for {
          (session, joined, editable) <- getSessionMenu(uid, sessionid)
          (sessionidNew, errmsgNew) <- {
            var errmsgNew = ""
            if (session == null) {
              //sessionid not exists
              errmsgNew = "session not exists"
              Future("", errmsgNew)
            } else if (session.publicType == 0) {
              //private session
              if (ouid != "") {
                //private session and ouid not empty
                friends = (friends :+ ouid).distinct
                generateNewGroupSession(uid, friends).map { case (sessionName, sessionIcons) =>
                  var filePath = ""
                  try {
                    implicit val writer = PngWriter.NoCompression
                    var bgImg = Image.filled(200, 200, Color.White)
                    sessionIcons.map { avatar =>
                      var avatarPath = avatar
                      if (avatar.startsWith("/")) {
                        avatarPath = avatar.drop(1)
                      } else {
                        avatarPath = s"www/$avatar"
                      }
                      Image.fromFile(new File(avatarPath)).cover(90, 90)
                    }.zipWithIndex.foreach { case (avatarImg, i) =>
                      val x = (i % 2) * 100 + 5
                      val y = (i / 2) * 100 + 5
                      bgImg = bgImg.overlay(avatarImg, x, y)
                    }
                    val path1 = new SimpleDateFormat("yyyyMM").format(System.currentTimeMillis())
                    val path2 = new SimpleDateFormat("dd").format(System.currentTimeMillis())
                    val pathRoot = "upload/avatar"
                    val path = s"$pathRoot/$path1/$path2"
                    val dir = new File(path)
                    if (!dir.exists()) {
                      dir.mkdirs()
                    }
                    val filenameNew = UUID.randomUUID().toString
                    filePath = s"$path/$filenameNew.thumb.png"
                    FileUtils.writeByteArrayToFile(new File(filePath), bgImg.bytes)
                    filePath = s"/$filePath"
                    createGroupSession(uid, sessionName = sessionName, sessionIcon = filePath, publicType = 1).map { case (sessionCreated, errmsgCreated) =>
                      //after session created user must join session first
                      joinSession(uid, sessionCreated).map { updateResult =>
                        if (updateResult.errmsg != "") {
                          ("", updateResult.errmsg)
                        } else {
                          (sessionCreated, errmsgCreated)
                        }
                      }
                    }.flatMap(t => t)
                  } catch { case e: Throwable =>
                    errmsgNew = s"create group session icon error: $e"
                    consoleLog("ERROR", errmsgNew)
                    Future("", errmsgNew)
                  }
                }.flatMap(t => t)
              } else {
                //private session but ouid is empty
                errmsgNew = "ouid is empty"
                Future("", errmsgNew)
              }
            } else {
              //group session
              Future(sessionid, errmsgNew)
            }
          }
          json <- {
            if (sessionidNew != "") {
              inviteFriendsToGroupSession(uid, friends, sessionidNew).map { list =>
                val successUsers = list.filter { case (nickname, updateResult) => updateResult.errmsg == "" }
                if (successUsers.isEmpty) {
                  Json.obj(
                    "errmsg" -> "no friends invite to session",
                    "successmsg" -> "",
                    "sessionid" -> ""
                  )
                } else {
                  val successUsersNickname = successUsers.map { case (nickname, updateResult) => nickname}.mkString(", ")
                  Json.obj(
                    "errmsg" -> "",
                    "successmsg" -> s"invite $successUsersNickname success",
                    "sessionid" -> sessionidNew
                  )
                }
              }
            } else {
              Future(
                Json.obj(
                  "errmsg" -> errmsgNew,
                  "successmsg" -> "",
                  "sessionid" -> ""
                )
              )
            }
          }
        } yield {
          json
        }
      }
    }
  }

  def joinFriendCtl(userTokenStr: String, fuid: String)(implicit ec: ExecutionContext): Future[JsObject] = {
    val userToken = verifyUserToken(userTokenStr)
    if (userToken.uid == "") {
      Future(
        Json.obj(
          "errmsg" -> "no privilege to join friends",
          "successmsg" -> ""
        )
      )
    } else {
      val uid = userToken.uid
      joinFriend(uid, fuid).map { updateResult =>
        if (updateResult.errmsg != "") {
          Json.obj(
            "errmsg" -> updateResult.errmsg,
            "successmsg" -> ""
          )
        } else {
          Json.obj(
            "errmsg" -> "",
            "successmsg" -> "join friend success"
          )
        }
      }
    }
  }

  def removeFriendCtl(userTokenStr: String, fuid: String)(implicit ec: ExecutionContext): Future[JsObject] = {
    val userToken = verifyUserToken(userTokenStr)
    if (userToken.uid == "") {
      Future(
        Json.obj(
          "errmsg" -> "no privilege to remove friends",
          "successmsg" -> ""
        )
      )
    } else {
      val uid = userToken.uid
      removeFriend(uid, fuid).map { updateResult =>
        if (updateResult.errmsg != "") {
          Json.obj(
            "errmsg" -> updateResult.errmsg,
            "successmsg" -> ""
          )
        } else {
          Json.obj(
            "errmsg" -> "",
            "successmsg" -> "remove friend success"
          )
        }
      }
    }
  }

  def getPrivateSessionCtl(userTokenStr: String, ouid: String)(implicit ec: ExecutionContext, notificationActor: ActorRef): Future[JsObject] = {
    val userToken = verifyUserToken(userTokenStr)
    if (userToken.uid == "") {
      Future(
        Json.obj(
          "errmsg" -> "no privilege to get private session",
          "sessionid" -> ""
        )
      )
    } else {
      val uid = userToken.uid
      createPrivateSession(uid, ouid).map { case (sessionid, errmsg) =>
        Json.obj(
          "errmsg" -> errmsg,
          "sessionid" -> sessionid
        )
      }
    }
  }

  def getSessionHeaderCtl(userTokenStr: String, sessionid: String)(implicit ec: ExecutionContext): Future[JsObject] = {
    val userToken = verifyUserToken(userTokenStr)
    if (userToken.uid == "") {
      Future(
        Json.obj(
          "errmsg" -> "no privilege to get session header",
          "session" -> JsNull
        )
      )
    } else {
      val uid = userToken.uid
      getSessionHeader(uid, sessionid).map { case (session, sessionToken) =>
        if (session != null && sessionToken.sessionName != "") {
          Json.obj(
            "errmsg" -> "",
            "session" -> Json.obj(
              "sessionid" -> sessionid,
              "sessionName" -> sessionToken.sessionName,
              "sessionIcon" -> sessionToken.sessionIcon,
              "createuid" -> session.createuid,
              "ouid" -> session.ouid
            )
          )
        } else {
          Json.obj(
            "errmsg" -> "no privilege or session not exists",
            "session" -> JsNull
          )
        }
      }
    }
  }

  def getSessionMenuCtl(userTokenStr: String, sessionid: String)(implicit ec: ExecutionContext): Future[JsObject] = {
    val userToken = verifyUserToken(userTokenStr)
    if (userToken.uid == "") {
      Future(
        Json.obj(
          "errmsg" -> "no privilege to get session menu",
          "session" -> JsNull
        )
      )
    } else {
      val uid = userToken.uid
      getSessionMenu(uid, sessionid).map { case (session, joined, editable) =>
        if (session == null) {
          Json.obj(
            "errmsg" -> "no privilege to get session menu",
            "session" -> JsNull
          )
        } else {
          Json.obj(
            "errmsg" -> "",
            "session" -> Json.obj(
              "sessionid" -> session._id,
              "sessionName" -> session.sessionName,
              "sessionIcon" -> session.sessionIcon,
              "createuid" -> session.createuid,
              "ouid" -> session.ouid,
              "joined" -> joined,
              "editable" -> editable
            )
          )
        }
      }
    }
  }

  def getUserMenuCtl(userTokenStr: String, ouid: String)(implicit ec: ExecutionContext): Future[JsObject] = {
    val userToken = verifyUserToken(userTokenStr)
    if (userToken.uid == "") {
      Future(
        Json.obj(
          "errmsg" -> "no privilege to get user menu",
          "user" -> JsNull
        )
      )
    } else {
      val uid = userToken.uid
      getUserMenu(uid, ouid).map { case (ouser, isFriend) =>
        if (ouid == null) {
          Json.obj(
            "errmsg" -> "no privilege to get user menu",
            "user" -> JsNull
          )
        } else {
          Json.obj(
            "errmsg" -> "",
            "user" -> Json.obj(
              "uid" -> ouser._id,
              "nickname" -> ouser.nickname,
              "avatar" -> ouser.avatar,
              "gender" -> ouser.gender,
              "isFriend" -> isFriend
            )
          )
        }
      }
    }
  }

  def listNotificationsCtl(userTokenStr: String, page: Int = 10, count: Int = 1)(implicit ec: ExecutionContext): Future[JsObject] = {
    val userToken = verifyUserToken(userTokenStr)
    if (userToken.uid == "") {
      Future(
        Json.obj(
          "errmsg" -> "no privilege to list notifications",
          "notifications" -> JsArray()
        )
      )
    } else {
      val uid = userToken.uid
      listNotifications(uid, page, count).map { results =>
        val notifications = results.map { case (notification, senduser, session) =>
          var uid = ""
          var nickname = ""
          var avatar = ""
          var sessionid = ""
          var sessionName = ""
          var content = ""
          if (senduser != null) {
            uid = senduser._id
            nickname = senduser.nickname
            avatar = senduser.avatar
          }
          if (session != null) {
            sessionName = session.sessionName
            sessionid = session._id
          }
          if (notification.noticeType == "joinFriend") {
            content = s"$nickname join you as friend"
          } else if (notification.noticeType == "removeFriend") {
            content = s"$nickname remove you from friend"
          } else {
            content = s"$nickname invite you in $sessionName"
          }
          Json.obj(
            "uid" -> uid,
            "nickname" -> nickname,
            "avatar" -> avatar,
            "content" -> content,
            "sessionid" -> sessionid,
            "sessionName" -> sessionName,
            "isRead" -> notification.isRead,
            "dateline" -> timeToStr(notification.dateline)
          )
        }
        Json.obj(
          "errmsg" -> "",
          "notifications" -> notifications
        )
      }
    }
  }


}
