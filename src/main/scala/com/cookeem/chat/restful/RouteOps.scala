package com.cookeem.chat.restful

import java.io.File
import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.util.UUID

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, Multipart, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.FileInfo
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.FileIO
import com.cookeem.chat.common.CommonUtils._
import com.cookeem.chat.mongo.MongoLogic._
import com.cookeem.chat.restful.Controller._
import com.cookeem.chat.websocket.{ChatSession, PushSession}
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
  createNotificationsCollection()

  def routeLogic(implicit ec: ExecutionContext, system: ActorSystem, materializer: ActorMaterializer, notificationActor: ActorRef) = {
    routeWebsocket ~
    routeAsset ~
    routeUserRegister ~
    routeGetUserToken ~
    routeGetSessionToken ~
    routeVerifyUserToken ~
    routeUserLogin ~
    routeUserLogout ~
    routeUserInfoUpdate ~
    routeUserPwdChange ~
    routeGetUserInfo ~
    routeCreateGroupSession ~
    routeGetGroupSessionInfo ~
    routeEditGroupSession ~
    routeListSessions ~
    routeListJoinedSessions ~
    routeGetNewNotificationCount ~
    routeListMessages ~
    routeJoinGroupSession ~
    routeLeaveGroupSession ~
    routeGetJoinedUsers ~
    routeGetFriends ~
    routeInviteFriends ~
    routeJoinFriend ~
    routeRemoveFriend ~
    routeGetPrivateSession ~
    routeGetSessionHeader ~
    routeGetSessionMenu ~
    routeGetUserMenu ~
    routeListNotifications
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
    }.mapAsync[(String, String)](6)(t => t).runFold(Map[String, String]())(_ + _)
  }

  def routeWebsocket(implicit ec: ExecutionContext, system: ActorSystem, materializer: ActorMaterializer) = {
    get {
      //use for chat service
      path("ws-chat") {
        val chatSession = new ChatSession()
        handleWebSocketMessages(chatSession.chatService)
        //use for push service
      } ~ path("ws-push") {
        val pushSession = new PushSession()
        handleWebSocketMessages(pushSession.pushService)
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
          registerUserCtl(login, nickname, password, repassword, gender) map { json =>
            HttpEntity(ContentTypes.`application/json`, Json.stringify(json))
          }
        }
      }
    }
  }

  def routeGetUserToken(implicit ec: ExecutionContext) = post {
    path("api" / "userToken") {
      formFieldMap { params =>
        val userTokenStr = paramsGetString(params, "userToken", "")
        complete {
          createUserTokenCtl(userTokenStr) map { json =>
            HttpEntity(ContentTypes.`application/json`, Json.stringify(json))
          }
        }
      }
    }
  }

  def routeGetSessionToken(implicit ec: ExecutionContext) = post {
    path("api" / "sessionToken") {
      formFieldMap { params =>
        val userTokenStr = paramsGetString(params, "userToken", "")
        val sessionid = paramsGetString(params, "sessionid", "")
        complete {
          createSessionTokenCtl(userTokenStr, sessionid) map { json =>
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
          verifyUserTokenCtl(userTokenStr) map { json =>
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
          loginCtl(login, password) map { json =>
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
          logoutCtl(userTokenStr) map { json =>
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
          changePwdCtl(userTokenStr, oldPwd, newPwd, renewPwd) map { json =>
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
          getUserInfoCtl(userTokenStr, uid) map { json =>
            HttpEntity(ContentTypes.`application/json`, Json.stringify(json))
          }
        }
      }
    }
  }

  def routeCreateGroupSession(implicit ec: ExecutionContext, materializer: ActorMaterializer, notificationActor: ActorRef) = post {
    path("api" / "createGroupSession") {
      entity(as[Multipart.FormData]) { formData =>
        //mix file upload and text formdata to Map[String, String]
        val futureParams: Future[Map[String, String]] = multiPartExtract(formData, "upload/avatar")
        complete {
          // complete support nest future
          futureParams.map { params =>
            val userTokenStr = paramsGetString(params, "userToken", "")
            val publicType = paramsGetInt(params, "publicType", 0)
            val sessionName = paramsGetString(params, "sessionName", "")
            val sessionIcon = paramsGetString(params, "sessionIcon", "")
            createGroupSessionCtl(userTokenStr, sessionName, sessionIcon, publicType).map { json =>
              HttpEntity(ContentTypes.`application/json`, Json.stringify(json))
            }
          }
        }
      }
    }
  }

  def routeGetGroupSessionInfo(implicit ec: ExecutionContext) = post {
    path("api" / "getGroupSessionInfo") {
      formFieldMap { params =>
        val userTokenStr = paramsGetString(params, "userToken", "")
        val sessionid = paramsGetString(params, "sessionid", "")
        complete {
          getEditGroupSessionInfoCtl(userTokenStr, sessionid) map { json =>
            HttpEntity(ContentTypes.`application/json`, Json.stringify(json))
          }
        }
      }
    }
  }

  def routeEditGroupSession(implicit ec: ExecutionContext, materializer: ActorMaterializer) = post {
    path("api" / "editGroupSession") {
      entity(as[Multipart.FormData]) { formData =>
        //mix file upload and text formdata to Map[String, String]
        val futureParams: Future[Map[String, String]] = multiPartExtract(formData, "upload/avatar")
        complete {
          // complete support nest future
          futureParams.map { params =>
            val userTokenStr = paramsGetString(params, "userToken", "")
            val publicType = paramsGetInt(params, "publicType", 0)
            val sessionid = paramsGetString(params, "sessionid", "")
            val sessionName = paramsGetString(params, "sessionName", "")
            val sessionIcon = paramsGetString(params, "sessionIcon", "")
            editGroupSessionCtl(userTokenStr, sessionid, sessionName, sessionIcon, publicType).map { json =>
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
        complete {
          listSessionsCtl(userTokenStr, isPublic) map { json =>
            HttpEntity(ContentTypes.`application/json`, Json.stringify(json))
          }
        }
      }
    }
  }

  def routeListJoinedSessions(implicit ec: ExecutionContext) = post {
    path("api" / "listJoinedSessions") {
      formFieldMap { params =>
        val userTokenStr = paramsGetString(params, "userToken", "")
        complete {
          listJoinedSessionsCtl(userTokenStr) map { json =>
            HttpEntity(ContentTypes.`application/json`, Json.stringify(json))
          }
        }
      }
    }
  }

  def routeGetNewNotificationCount(implicit ec: ExecutionContext) = post {
    path("api" / "getNewNotificationCount") {
      formFieldMap { params =>
        val userTokenStr = paramsGetString(params, "userToken", "")
        complete {
          getNewNotificationCountCtl(userTokenStr) map { json =>
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
        val page = paramsGetInt(params, "page", 1)
        val count = paramsGetInt(params, "count", 10)
        complete {
          listMessagesCtl(userTokenStr, sessionid, page, count) map { json =>
            HttpEntity(ContentTypes.`application/json`, Json.stringify(json))
          }
        }
      }
    }
  }

  def routeJoinGroupSession(implicit ec: ExecutionContext, notificationActor: ActorRef) = post {
    path("api" / "joinGroupSession") {
      formFieldMap { params =>
        val userTokenStr = paramsGetString(params, "userToken", "")
        val sessionid = paramsGetString(params, "sessionid", "")
        complete {
          joinGroupSessionCtl(userTokenStr, sessionid) map { json =>
            println(s"notificationActor: $notificationActor")
            HttpEntity(ContentTypes.`application/json`, Json.stringify(json))
          }
        }
      }
    }
  }

  def routeLeaveGroupSession(implicit ec: ExecutionContext, notificationActor: ActorRef) = post {
    path("api" / "leaveGroupSession") {
      formFieldMap { params =>
        val userTokenStr = paramsGetString(params, "userToken", "")
        val sessionid = paramsGetString(params, "sessionid", "")
        complete {
          leaveGroupSessionCtl(userTokenStr, sessionid) map { json =>
            HttpEntity(ContentTypes.`application/json`, Json.stringify(json))
          }
        }
      }
    }
  }

  def routeGetJoinedUsers(implicit ec: ExecutionContext) = post {
    path("api" / "getJoinedUsers") {
      formFieldMap { params =>
        val userTokenStr = paramsGetString(params, "userToken", "")
        val sessionid = paramsGetString(params, "sessionid", "")
        complete {
          getJoinedUsersCtl(userTokenStr, sessionid) map { json =>
            HttpEntity(ContentTypes.`application/json`, Json.stringify(json))
          }
        }
      }
    }
  }

  def routeGetFriends(implicit ec: ExecutionContext) = post {
    path("api" / "getFriends") {
      formFieldMap { params =>
        val userTokenStr = paramsGetString(params, "userToken", "")
        complete {
          getFriendsCtl(userTokenStr) map { json =>
            HttpEntity(ContentTypes.`application/json`, Json.stringify(json))
          }
        }
      }
    }
  }

  def routeInviteFriends(implicit ec: ExecutionContext, notificationActor: ActorRef) = post {
    path("api" / "inviteFriends") {
      formFieldMap { params =>
        val userTokenStr = paramsGetString(params, "userToken", "")
        val sessionid = paramsGetString(params, "sessionid", "")
        val ouid = paramsGetString(params, "ouid", "")
        val friendsStr = paramsGetString(params, "friends", "")
        complete {
          inviteFriendsCtl(userTokenStr, sessionid, friendsStr, ouid) map { json =>
            HttpEntity(ContentTypes.`application/json`, Json.stringify(json))
          }
        }
      }
    }
  }

  def routeJoinFriend(implicit ec: ExecutionContext) = post {
    path("api" / "joinFriend") {
      formFieldMap { params =>
        val userTokenStr = paramsGetString(params, "userToken", "")
        val fuid = paramsGetString(params, "fuid", "")
        complete {
          joinFriendCtl(userTokenStr, fuid) map { json =>
            HttpEntity(ContentTypes.`application/json`, Json.stringify(json))
          }
        }
      }
    }
  }

  def routeRemoveFriend(implicit ec: ExecutionContext) = post {
    path("api" / "removeFriend") {
      formFieldMap { params =>
        val userTokenStr = paramsGetString(params, "userToken", "")
        val fuid = paramsGetString(params, "fuid", "")
        complete {
          removeFriendCtl(userTokenStr, fuid) map { json =>
            HttpEntity(ContentTypes.`application/json`, Json.stringify(json))
          }
        }
      }
    }
  }

  def routeGetPrivateSession(implicit ec: ExecutionContext, notificationActor: ActorRef) = post {
    path("api" / "getPrivateSession") {
      formFieldMap { params =>
        val userTokenStr = paramsGetString(params, "userToken", "")
        val ouid = paramsGetString(params, "ouid", "")
        complete {
          getPrivateSessionCtl(userTokenStr, ouid) map { json =>
            HttpEntity(ContentTypes.`application/json`, Json.stringify(json))
          }
        }
      }
    }
  }

  def routeGetSessionHeader(implicit ec: ExecutionContext) = post {
    path("api" / "getSessionHeader") {
      formFieldMap { params =>
        val userTokenStr = paramsGetString(params, "userToken", "")
        val sessionid = paramsGetString(params, "sessionid", "")
        complete {
          getSessionHeaderCtl(userTokenStr, sessionid) map { json =>
            HttpEntity(ContentTypes.`application/json`, Json.stringify(json))
          }
        }
      }
    }
  }

  def routeGetSessionMenu(implicit ec: ExecutionContext) = post {
    path("api" / "getSessionMenu") {
      formFieldMap { params =>
        val userTokenStr = paramsGetString(params, "userToken", "")
        val sessionid = paramsGetString(params, "sessionid", "")
        complete {
          getSessionMenuCtl(userTokenStr, sessionid) map { json =>
            HttpEntity(ContentTypes.`application/json`, Json.stringify(json))
          }
        }
      }
    }
  }

  def routeGetUserMenu(implicit ec: ExecutionContext) = post {
    path("api" / "getUserMenu") {
      formFieldMap { params =>
        val userTokenStr = paramsGetString(params, "userToken", "")
        val ouid = paramsGetString(params, "ouid", "")
        complete {
          getUserMenuCtl(userTokenStr, ouid) map { json =>
            HttpEntity(ContentTypes.`application/json`, Json.stringify(json))
          }
        }
      }
    }
  }

  def routeListNotifications(implicit ec: ExecutionContext) = post {
    path("api" / "getNotifications") {
      formFieldMap { params =>
        val userTokenStr = paramsGetString(params, "userToken", "")
        val page = paramsGetInt(params, "page", 1)
        val count = paramsGetInt(params, "count", 30)
        complete {
          listNotificationsCtl(userTokenStr, page, count) map { json =>
            HttpEntity(ContentTypes.`application/json`, Json.stringify(json))
          }
        }
      }
    }
  }

}
