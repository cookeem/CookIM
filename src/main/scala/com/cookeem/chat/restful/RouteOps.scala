package com.cookeem.chat.restful

import java.io.File
import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.util.UUID

import akka.actor.ActorSystem
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.PathMatchers
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.FileIO
import com.cookeem.chat.common.CommonUtils._
import com.cookeem.chat.event.UserSessionInfo
import com.cookeem.chat.mongo.MongoLogic._
import com.cookeem.chat.restful.Controller._
import com.cookeem.chat.websocket.ChatSession
import com.cookeem.chat.websocket.OperateSession._
import play.api.libs.json.Json

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

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

  def routeUserInfoUpdate(implicit ec: ExecutionContext, materializer: ActorMaterializer) = post {
    path("api" / "updateUser") {
      formFieldMap { params =>
        val userTokenStr = paramsGetString(params, "userToken", "")
        val nickname = paramsGetString(params, "nickname", "")
        val gender = paramsGetInt(params, "gender", 0)
        val avatar = paramsGetString(params, "avatar", "")
//        if (avatar != "") {
//          fileUpload("avatar") {
//            case (metadata, byteSource) =>
//              complete {
//                val path1 = new SimpleDateFormat("yyyyMM").format(System.currentTimeMillis())
//                val path2 = new SimpleDateFormat("dd").format(System.currentTimeMillis())
//                val path = s"upload/avatar/$path1/$path2"
//                val dir = new File(path)
//                if (!dir.exists()) {
//                  dir.mkdirs()
//                }
//                val filenameNew = UUID.randomUUID().toString
//                val avatar = s"$path/$filenameNew"
//                for {
//                  ioResult <- byteSource.runWith(FileIO.toPath(Paths.get(avatar)))
//                  json <- {
//                    ioResult.status match {
//                      case Success(done) =>
//                        updateUserInfoCtl(userTokenStr, nickname, gender, avatar)
//                      case Failure(e) =>
//                        Future(
//                          Json.obj(
//                            "errmsg" -> s"upload file error: $e",
//                            "successmsg" -> ""
//                          )
//                        )
//                    }
//                  }
//                } yield {
//                  HttpEntity(ContentTypes.`application/json`, Json.stringify(json))
//                }
//              }
//          }
//        } else {
          complete {
            updateUserInfoCtl(userTokenStr, nickname, gender, avatar).map { json =>
              HttpEntity(ContentTypes.`application/json`, Json.stringify(json))
            }
//          }
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
        val renewPwd = paramsGetString(params, "renewPwd", "")
        complete {
          val changePwdResult = changePwdCtl(userTokenStr, oldPwd, newPwd, renewPwd)
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
