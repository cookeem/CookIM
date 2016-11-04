package com.cookeem.chat.restful

import com.cookeem.chat.mongo.MongoLogic._
import com.cookeem.chat.jwt.JwtOps._
import play.api.libs.json.{JsArray, JsObject, Json}

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by cookeem on 16/11/2.
  */
object Controller {
  def registerUserCtl(login: String, nickname: String, password: String, repassword: String, gender: Int)(implicit ec: ExecutionContext): Future[JsObject] = {
    if (password != repassword) {
      val errmsg = s"password and repassword must be same"
      Future {
        Json.obj(
          "uid" -> "",
          "errmsg" -> errmsg,
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
        if (uid != "") {
          val payload = Map[String, Any]("uid" -> uid)
          token = encodeJwt(payload)
        }
        Json.obj(
          "uid" -> uid,
          "errmsg" -> errmsg,
          "token" -> token
        )
      }
    }
  }

  def listPublicSessionsCtl(uid: String)(implicit ec: ExecutionContext) = {
    var errmsg = ""
    if (uid.length != 24) {
      errmsg = "user id format error"
      Future {
        Json.obj(
          "sessions" -> JsArray(),
          "errmsg" -> errmsg
        )
      }
    } else {
      listPublicSessions(uid).map { sessionList =>
        sessionList.map { session =>
          Json.obj(
            "" -> ""
          )
        }
      }
    }
  }
}
