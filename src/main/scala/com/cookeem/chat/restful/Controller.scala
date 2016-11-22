package com.cookeem.chat.restful

import com.cookeem.chat.common.CommonUtils._
import com.cookeem.chat.mongo.MongoLogic._
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
            "lastlogin" -> timeToStr(user.lastlogin),
            "logincount" -> user.logincount
          )
        )
      }
    }
  }

  def createGroupSessionCtl(userTokenStr: String, sessionicon: String, publictype: Int, sessionname: String)(implicit ec: ExecutionContext) = {
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
      createGroupSession(uid, sessionicon, publictype, sessionname).map { case (sessionid, errmsg) =>
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

  def listSessionsCtl(userTokenStr: String, isPublic: Boolean, showType: Int = 2, page: Int = 1, count: Int = 10)(implicit ec: ExecutionContext): Future[JsObject] = {
    val userToken = verifyUserToken(userTokenStr)
    if (userToken.uid == "") {
      Future(
        Json.obj(
          "errmsg" -> "no privilege to list sessions",
          "successmsg" -> "",
          "sessions" -> JsArray()
        )
      )
    } else {
      val uid = userToken.uid
      listSessions(uid, isPublic, showType, page, count).map { sessions =>
        val lastMessages = sessions.map { session =>
          getSessionLastMessage(userTokenStr, session._id).map { case (sessionLast, messageLast, userLast) =>
            var jsonMessage: JsValue = JsNull
            if (messageLast != null && userLast != null) {
              var jsonFileInfo: JsValue = JsNull
              if (messageLast.fileinfo != null) {
                jsonFileInfo = Json.obj(
                  "filepath" -> messageLast.fileinfo.filepath,
                  "filename" -> messageLast.fileinfo.filename,
                  "filetype" -> messageLast.fileinfo.filetype,
                  "size" -> messageLast.fileinfo.size
                )
              }
              jsonMessage = Json.obj(
                "uid" -> userLast._id,
                "nickname" -> userLast.nickname,
                "avatar" -> userLast.avatar,
                "msgtype" -> messageLast.msgtype,
                "noticetype" -> messageLast.noticetype,
                "message" -> messageLast.message,
                "fileinfo" -> jsonFileInfo
              )
            }
            Json.obj(
              "sessionid" -> session._id,
              "sessionname" -> session.sessionname,
              "sessiontype" -> session.sessiontype,
              "sessionicon" -> session.sessionicon,
              "publictype" -> session.publictype,
              "dateline" -> timeToStr(session.dateline),
              "message" -> jsonMessage
            )
          }
        }
        Future.sequence(lastMessages)
      }.flatMap(t => t).map { sessions =>
        Json.obj(
          "errmsg" -> "",
          "successmsg" -> "get public session success",
          "sessions" -> sessions
        )
      }
    }
  }

  def listMessagesCtl(userTokenStr: String, sessionid: String, page: Int = 1, count: Int = 10)(implicit ec: ExecutionContext): Future[JsObject] = {
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
              "messages" -> messageUsers.map { case (message, user) =>
                var jsonFileInfo: JsValue = JsNull
                if (message.fileinfo != null) {
                  jsonFileInfo = Json.obj(
                    "filepath" -> message.fileinfo.filepath,
                    "filename" -> message.fileinfo.filename,
                    "filetype" -> message.fileinfo.filetype,
                    "size" -> message.fileinfo.size
                  )
                }
                if (user != null) {
                  Json.obj(
                    "uid" -> user._id,
                    "nickname" -> user.nickname,
                    "avatar" -> user.avatar,
                    "msgtype" -> message.msgtype,
                    "noticetype" -> message.noticetype,
                    "message" -> message.message,
                    "fileinfo" -> jsonFileInfo
                  )
                } else {
                  Json.obj(
                    "uid" -> "",
                    "nickname" -> "",
                    "avatar" -> "",
                    "msgtype" -> message.msgtype,
                    "noticetype" -> message.noticetype,
                    "message" -> message.message,
                    "fileinfo" -> jsonFileInfo
                  )
                }
              }
            )
          }
        }
      } yield {
        ret
      }
    }
  }

}
