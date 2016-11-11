package com.cookeem.chat.restful

import com.cookeem.chat.common.CommonUtils._
import com.cookeem.chat.mongo.MongoLogic._
import com.cookeem.chat.jwt.JwtOps._
import play.api.libs.json.{JsArray, JsNull, JsObject, Json}

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
          "token" -> ""
        )
      }
    } else {
      val avatar = gender match {
        case 1 => "avatar/boy.png"
        case 2 => "avatar/girl.png"
        case _ => "avatar/unknown.png"
      }
      registerUser(login, nickname, password, gender, avatar).map { case (uid, errmsg) =>
        var token = ""
        var successmsg = ""
        if (uid != "") {
          val payload = Map[String, Any]("uid" -> uid)
          token = encodeJwt(payload)
          successmsg = "register user success, thank you for join us"
        }
        Json.obj(
          "uid" -> uid,
          "errmsg" -> errmsg,
          "successmsg" -> successmsg,
          "token" -> token
        )
      }
    }
  }

  def loginCtl(login: String, password: String)(implicit ec: ExecutionContext): Future[JsObject] = {
    var errmsg = ""
    if (password.length < 6) {
      Future {
        Json.obj(
          "uid" -> "",
          "errmsg" -> s"password must at least 6 characters",
          "successmsg" -> "",
          "token" -> ""
        )
      }
    } else {
      loginAction(login, password).map { uid =>
        var token = ""
        var successmsg = ""
        if (uid != "") {
          val payload = Map[String, Any]("uid" -> uid)
          token = encodeJwt(payload)
          successmsg = "login in success"
        } else {
          errmsg = "user not exist or password not match"
        }
        Json.obj(
          "uid" -> uid,
          "errmsg" -> errmsg,
          "token" -> token
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

  def changePwdCtl(userTokenStr: String, oldPwd: String, newPwd: String)(implicit ec: ExecutionContext): Future[JsObject] = {
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
      changePwd(uid, oldPwd, newPwd).map { updateResult =>
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


  def listPublicSessionsCtl(userTokenStr: String)(implicit ec: ExecutionContext): Future[JsObject] = {
    val userToken = verifyUserToken(userTokenStr)
    if (userToken.uid == "") {
      Future(
        Json.obj(
          "errmsg" -> "no privilege to list public sessions",
          "successmsg" -> "",
          "sessions" -> JsArray()
        )
      )
    } else {
      val uid = userToken.uid
      listPublicSessions(uid).map { sessions =>
        val lastMessages = sessions.map { session =>
          getSessionLastMessage(userTokenStr, session._id).map { case (sessionLast, messageLast, userLast) =>
            Json.obj(
              "sessionid" -> session._id,
              "sessionname" -> session.name,
              "jointype" -> session.jointype,
              "sessiontype" -> session.sessiontype,
              "visabletype" -> session.visabletype,
              "dateline" -> timeToStr(session.dateline),
              "message" -> Json.obj(
                "uid" -> userLast._id,
                "nickname" -> userLast.nickname,
                "avatar" -> userLast.avatar,
                "msgtype" -> messageLast.msgtype,
                "noticetype" -> messageLast.noticetype,
                "message" -> messageLast.message,
                "fileinfo" -> Json.obj(
                  "filepath" -> messageLast.fileinfo.filepath,
                  "filename" -> messageLast.fileinfo.filename,
                  "filetype" -> messageLast.fileinfo.filetype,
                  "size" -> messageLast.fileinfo.size
                )
              )
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

}
