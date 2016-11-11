package com.cookeem.chat.restful

import akka.actor.ActorSystem
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.PathMatchers
import akka.stream.ActorMaterializer
import com.cookeem.chat.common.CommonUtils._
import com.cookeem.chat.event.UserSessionInfo
import com.cookeem.chat.mongo.MongoLogic._
import com.cookeem.chat.restful.Controller._
import com.cookeem.chat.websocket.ChatSession
import com.cookeem.chat.websocket.OperateSession._
import play.api.libs.json.Json

import scala.concurrent.ExecutionContext

/**
  * Created by cookeem on 16/11/3.
  */
object RouteOps {

  def routeLogic(implicit ec: ExecutionContext, system: ActorSystem, materializer: ActorMaterializer) = {
    routeWebsocket ~
    routeAsset ~
    routeUserRegister ~
    routeUserLogin ~
    routeUserLogout ~
    routeUserInfoUpdate ~
    routeUserPwdChange ~
    routeGetUserInfo ~
    routeListPublicSessions
  }

  def routeWebsocket(implicit ec: ExecutionContext, system: ActorSystem, materializer: ActorMaterializer) = {
    get {
      pathPrefix("ws-chat" / Segment) { userToken =>
        path(PathMatchers.Segment) { sessionToken =>
          val UserSessionInfo(uid, nickname, avatar, sessionid) = verifyUserSessionToken(userToken, sessionToken)
          if (uid != "" && sessionid != "") {
            val chatSession = new ChatSession(uid, nickname, avatar, sessionid)
            handleWebSocketMessages(chatSession.chatService(uid, nickname, avatar))
          } else {
            handleWebSocketMessages(rejectWebsocket())
          }
        }
      } ~ pathPrefix("ws-user" / Segment) { uid =>
        handleWebSocketMessages(createUserTokenWebsocket(uid))
      } ~ pathPrefix("ws-session" / Segment) { uid =>
        path(PathMatchers.Segment) { sessionid =>
          handleWebSocketMessages(createSessionTokenWebsocket(uid, sessionid))
        }
      }
    }
  }

  def routeAsset(implicit ec: ExecutionContext) = {
    get {
      pathSingleSlash {
        redirect("chat/", StatusCodes.PermanentRedirect)
      } ~ path("chat") {
        redirect("chat/", StatusCodes.PermanentRedirect)
      } ~ path("chat" / "") {
        getFromFile("www/index.html")
      } ~ pathPrefix("chat") {
        getFromDirectory("www")
      } ~ path("ping") {
        val headers = List(
          RawHeader("X-MyObject-Id", "myobjid"),
          RawHeader("X-MyObject-Name", "myobjname")
        )
        respondWithHeaders(headers) {
          complete("pong")
        }
      }
    }
  }

  def routeUserRegister(implicit ec: ExecutionContext) = post {
    path("api" / "registerUser") {
      formFieldMap { params =>
        val login = paramsGetString(params, "login", "")
        val nickname = paramsGetString(params, "nickname", "")
        val password = paramsGetString(params, "password", "")
        val repassword = paramsGetString(params, "repassword", "")
        val gender = paramsGetInt(params, "gender", 0)
        complete {
          val registerUserResult = registerUserCtl(login, nickname, password, repassword, gender)
          registerUserResult map { json =>
            HttpEntity(ContentTypes.`application/json`, Json.stringify(json))
          }
        }
      }
    }
  }

  def routeUserLogin(implicit ec: ExecutionContext) = post {
    path("api" / "loginUser") {
      formFieldMap { params =>
        val login = paramsGetString(params, "login", "")
        val password = paramsGetString(params, "password", "")
        complete {
          val loginUserResult = loginCtl(login, password)
          loginUserResult map { json =>
            HttpEntity(ContentTypes.`application/json`, Json.stringify(json))
          }
        }
      }
    }
  }

  def routeUserLogout(implicit ec: ExecutionContext) = post {
    path("api" / "logoutUser") {
      formFieldMap { params =>
        val userTokenStr = paramsGetString(params, "userToken", "")
        complete {
          val logoutUserResult = logoutCtl(userTokenStr)
          logoutUserResult map { json =>
            HttpEntity(ContentTypes.`application/json`, Json.stringify(json))
          }
        }
      }
    }
  }

  def routeUserInfoUpdate(implicit ec: ExecutionContext) = post {
    path("api" / "updateUser") {
      formFieldMap { params =>
        val userTokenStr = paramsGetString(params, "userToken", "")
        val nickname = paramsGetString(params, "nickname", "")
        val gender = paramsGetInt(params, "gender", 0)
        val avatar = paramsGetString(params, "avatar", "")
        complete {
          val updateUserInfoResult = updateUserInfoCtl(userTokenStr, nickname, gender, avatar)
          updateUserInfoResult map { json =>
            HttpEntity(ContentTypes.`application/json`, Json.stringify(json))
          }
        }
      }
    }
  }

  def routeUserPwdChange(implicit ec: ExecutionContext) = post {
    path("api" / "changePwd") {
      formFieldMap { params =>
        val userTokenStr = paramsGetString(params, "userToken", "")
        val oldPwd = paramsGetString(params, "oldPwd", "")
        val newPwd = paramsGetString(params, "newPwd", "")
        complete {
          val changePwdResult = changePwdCtl(userTokenStr, oldPwd, newPwd)
          changePwdResult map { json =>
            HttpEntity(ContentTypes.`application/json`, Json.stringify(json))
          }
        }
      }
    }
  }

  def routeGetUserInfo(implicit ec: ExecutionContext) = post {
    path("api" / "getUserInfo") {
      formFieldMap { params =>
        val userTokenStr = paramsGetString(params, "userToken", "")
        val uid = paramsGetString(params, "uid", "")
        complete {
          val getUserInfoResult = getUserInfoCtl(userTokenStr, uid)
          getUserInfoResult map { json =>
            HttpEntity(ContentTypes.`application/json`, Json.stringify(json))
          }
        }
      }
    }
  }

  def routeListPublicSessions(implicit ec: ExecutionContext) = post {
    path("api" / "listPublicSessions") {
      formFieldMap { params =>
        val userTokenStr = paramsGetString(params, "userToken", "")
        complete {
          val listPublicSessionsResult = listPublicSessionsCtl(userTokenStr)
          listPublicSessionsResult map { json =>
            HttpEntity(ContentTypes.`application/json`, Json.stringify(json))
          }
        }
      }
    }
  }

}
