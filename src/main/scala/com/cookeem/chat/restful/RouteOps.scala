package com.cookeem.chat.restful

import java.io.File
import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.util.UUID

import akka.actor.ActorSystem
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, Multipart, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.FileInfo
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.FileIO
import com.cookeem.chat.common.CommonUtils._
import com.cookeem.chat.mongo.MongoLogic._
import com.cookeem.chat.restful.Controller._
import com.cookeem.chat.websocket.ChatSession
import com.cookeem.chat.websocket.TokenWebsocket._
import com.sksamuel.scrimage.Image
import com.sksamuel.scrimage.nio.PngWriter
import org.apache.commons.io.FileUtils
import play.api.libs.json.Json

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

/**
  * Created by cookeem on 16/11/3.
  */
object RouteOps {
  //init create mongodb collection
  createUsersCollection()
  createSessionsCollection()
  createMessagesCollection()
  createOnlinesCollection()

  def routeLogic(implicit ec: ExecutionContext, system: ActorSystem, materializer: ActorMaterializer) = {
    routeWebsocket ~
    routeAsset ~
    routeUserRegister ~
    routeVerifyUserToken ~
    routeUserLogin ~
    routeUserLogout ~
    routeUserInfoUpdate ~
    routeUserPwdChange ~
    routeGetUserInfo ~
    routeCreateGroupSession ~
    routeListSessions ~
    routeListMessages
  }

  //mix multiform to Future[Map[String, String]]. if include file upload, save to pathroot and return path
  def multiPartExtract(formData: Multipart.FormData, pathRoot: String)(implicit ec: ExecutionContext, materializer: ActorMaterializer): Future[Map[String, String]] = {
    formData.parts.map { part =>
      if (part.filename.isDefined) {
        val fileInfo = FileInfo(part.name, part.filename.get, part.entity.contentType)
        val contentType = fileInfo.contentType.value
        val path1 = new SimpleDateFormat("yyyyMM").format(System.currentTimeMillis())
        val path2 = new SimpleDateFormat("dd").format(System.currentTimeMillis())
        val path = s"$pathRoot/$path1/$path2"
        val dir = new File(path)
        if (!dir.exists()) {
          dir.mkdirs()
        }
        val filenameNew = UUID.randomUUID().toString
        var avatarPath = s"$path/$filenameNew"
        if (contentType == "image/jpeg" || contentType == "image/gif" || contentType == "image/png") {
          avatarPath = s"$avatarPath.${contentType.replace("image/", "")}.thumb.png"
        } else {
          avatarPath = ""
        }
        part.entity.dataBytes.runWith(FileIO.toPath(Paths.get(avatarPath))).map { _ =>
          try {
            if (contentType == "image/jpeg" || contentType == "image/gif" || contentType == "image/png") {
              //crop and resize image
              implicit val writer = PngWriter.NoCompression
              val bytesImage = Image.fromFile(new File(avatarPath)).cover(200, 200).bytes
              FileUtils.writeByteArrayToFile(new File(avatarPath), bytesImage)
            }
            avatarPath = s"/$avatarPath"
          } catch { case e: Throwable =>
            consoleLog("ERROR", s"images process error: $e")
            avatarPath = ""
          }
          (part.name, avatarPath)
        }
      } else {
        part.entity.toStrict(5.seconds).map(e => (part.name, e.data.utf8String))
      }
    }.mapAsync[(String, String)](1)(t => t).runFold(Map[String, String]())(_ + _)
  }

  def routeWebsocket(implicit ec: ExecutionContext, system: ActorSystem, materializer: ActorMaterializer) = {
    get {
      path("ws-chat") {
        val chatSession = new ChatSession()
        handleWebSocketMessages(chatSession.chatService)
      } ~ path("ws-user") {
        handleWebSocketMessages(createUserTokenWebsocket())
      } ~ path("ws-session") {
        handleWebSocketMessages(createSessionTokenWebsocket())
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
      } ~ pathPrefix("upload") {
        getFromDirectory("upload")
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

  def routeVerifyUserToken(implicit ec: ExecutionContext) = post {
    path("api" / "verifyUserToken") {
      formFieldMap { params =>
        val userTokenStr = paramsGetString(params, "userToken", "")
        complete {
          val verifyUserTokenResult = verifyUserTokenCtl(userTokenStr)
          verifyUserTokenResult map { json =>
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
      entity(as[Multipart.FormData]) { formData =>
        //mix file upload and text formdata to Map[String, String]
        val futureParams: Future[Map[String, String]] = multiPartExtract(formData, "upload/avatar")
        complete {
          // complete support nest future
          futureParams.map { params =>
            val userTokenStr = paramsGetString(params, "userToken", "")
            val nickname = paramsGetString(params, "nickname", "")
            val gender = paramsGetInt(params, "gender", 0)
            val avatar = paramsGetString(params, "avatar", "")
            updateUserInfoCtl(userTokenStr, nickname, gender, avatar).map { json =>
              HttpEntity(ContentTypes.`application/json`, Json.stringify(json))
            }
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

  def routeCreateGroupSession(implicit ec: ExecutionContext, materializer: ActorMaterializer) = post {
    path("api" / "createGroupSession") {
      entity(as[Multipart.FormData]) { formData =>
        //mix file upload and text formdata to Map[String, String]
        val futureParams: Future[Map[String, String]] = multiPartExtract(formData, "upload/avatar")
        complete {
          // complete support nest future
          futureParams.map { params =>
            val userTokenStr = paramsGetString(params, "userToken", "")
            val publictype = paramsGetInt(params, "publictype", 0)
            val name = paramsGetString(params, "name", "")
            val chaticon = paramsGetString(params, "chaticon", "")
            createGroupSessionCtl(userTokenStr, chaticon, publictype, name).map { json =>
              HttpEntity(ContentTypes.`application/json`, Json.stringify(json))
            }
          }
        }
      }
    }
  }

  def routeListSessions(implicit ec: ExecutionContext) = post {
    path("api" / "listSessions") {
      formFieldMap { params =>
        val userTokenStr = paramsGetString(params, "userToken", "")
        val isPublic = paramsGetInt(params, "isPublic", 0) match {
          case 1 => true
          case _ => false
        }
        val showType = paramsGetInt(params, "showType", 0)
        val page = paramsGetInt(params, "page", 0)
        val count = paramsGetInt(params, "count", 0)
        complete {
          val listSessionsResult = listSessionsCtl(userTokenStr, isPublic, showType, page, count)
          listSessionsResult map { json =>
            HttpEntity(ContentTypes.`application/json`, Json.stringify(json))
          }
        }
      }
    }
  }

  def routeListMessages(implicit ec: ExecutionContext) = post {
    path("api" / "listMessages") {
      formFieldMap { params =>
        val userTokenStr = paramsGetString(params, "userToken", "")
        val sessionid = paramsGetString(params, "sessionid", "")
        val page = paramsGetInt(params, "page", 0)
        val count = paramsGetInt(params, "count", 0)
        complete {
          val listMessagesResult = listMessagesCtl(userTokenStr, sessionid, page, count)
          listMessagesResult map { json =>
            HttpEntity(ContentTypes.`application/json`, Json.stringify(json))
          }
        }
      }
    }
  }

}
