package com.cookeem.chat.restful

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.util.ByteString
import com.cookeem.chat.common.CommonUtils._
import com.cookeem.chat.mongo.MongoLogic._
import com.cookeem.chat.restful.Controller._
import com.cookeem.chat.websocket.{ChatSession, PushSession}
import play.api.libs.json.Json

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

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
    routeListNotifications ~
    routeGetFileMeta ~
    routeGetFile
  }

  // mix multiform to Future[Map[String, ByteString]].
  // if part type is file, then part name have prefix "binary!", and ByteString content is {"fileName": "xxx", "fileType": "xxx"} ++ <#HeaderInfo#> ++ file content bytestring
  def multiPartExtract(formData: Multipart.FormData)(implicit ec: ExecutionContext, materializer: ActorMaterializer): Future[Map[String, ByteString]] = {
    formData.parts.map { part =>
      if (part.filename.isDefined) {
        val contentType = part.entity.contentType.value
        val jsonHeaderInfo = Json.obj(
          "fileName" -> part.filename.get,
          "fileType" -> contentType
        )
        val bsHeaderInfo = ByteString(Json.stringify(jsonHeaderInfo) + "<#HeaderInfo#>")
        part.entity.dataBytes.runFold(ByteString.empty)(_ ++ _).map(bs => (s"binary!${part.name}", bsHeaderInfo ++ bs))
      } else {
        part.entity.dataBytes.runFold(ByteString.empty)(_ ++ _).map(bs => (part.name, bs))
      }
    }.mapAsync[(String, ByteString)](6)(t => t).runFold(Map[String, ByteString]())(_ + _)
  }

  def extractHeaderInfo(paramBytes: Map[String, ByteString], key: String): (Array[Byte], String, String) = {
    var bytes = Array[Byte]()
    var fileName = ""
    var fileType = ""
    if (paramBytes.contains(s"binary!$key")) {
      try {
        val bs = paramBytes(s"binary!$key")
        val splitor = "<#HeaderInfo#>"
        val (bsJson, bsBin) = bs.splitAt(bs.indexOfSlice(splitor))
        val jsonStr = bsJson.utf8String
        bytes = bsBin.drop(splitor.length).toArray
        val json = Json.parse(jsonStr)
        fileName = getJsonString(json, "fileName")
        fileType = getJsonString(json, "fileType")
      } catch { case e: Throwable =>
          consoleLog("ERROR", s"extract header info error: key = $key, $e")
      }
    }
    (bytes, fileName, fileType)
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
        val futureParams: Future[Map[String, ByteString]] = multiPartExtract(formData)
        complete {
          // complete support nest future
          futureParams.map { paramBytes =>
            val params = paramBytes.filterNot { case (k, v) => k.startsWith("binary!")}.map { case (k, v) => (k, v.utf8String)}
            val userTokenStr = paramsGetString(params, "userToken", "")
            val nickname = paramsGetString(params, "nickname", "")
            val gender = paramsGetInt(params, "gender", 0)
            val (avatarBytes, avatarFileName, avatarFileType) = extractHeaderInfo(paramBytes, "avatar")
            updateUserInfoCtl(userTokenStr, nickname, gender, avatarBytes, avatarFileName, avatarFileType).map { json =>
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
        val futureParams: Future[Map[String, ByteString]] = multiPartExtract(formData)
        complete {
          // complete support nest future
          futureParams.map { paramBytes =>
            val params = paramBytes.filterNot { case (k, v) => k.startsWith("binary!")}.map { case (k, v) => (k, v.utf8String)}
            val userTokenStr = paramsGetString(params, "userToken", "")
            val publicType = paramsGetInt(params, "publicType", 0)
            val sessionName = paramsGetString(params, "sessionName", "")
            val (sessionIconBytes, sessionIconFileName, sessionIconFileType) = extractHeaderInfo(paramBytes, "sessionIcon")
            createGroupSessionCtl(userTokenStr, sessionName, sessionIconBytes, sessionIconFileName, sessionIconFileType, publicType).map { json =>
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
        val futureParams: Future[Map[String, ByteString]] = multiPartExtract(formData)
        complete {
          // complete support nest future
          futureParams.map { paramBytes =>
            val params = paramBytes.filterNot { case (k, v) => k.startsWith("binary!")}.map { case (k, v) => (k, v.utf8String)}
            val userTokenStr = paramsGetString(params, "userToken", "")
            val publicType = paramsGetInt(params, "publicType", 0)
            val sessionid = paramsGetString(params, "sessionid", "")
            val sessionName = paramsGetString(params, "sessionName", "")
            val (sessionIconBytes, sessionIconFileName, sessionIconFileType) = extractHeaderInfo(paramBytes, "sessionIcon")
            editGroupSessionCtl(userTokenStr, sessionid, sessionName, sessionIconBytes, sessionIconFileName, sessionIconFileType, publicType).map { json =>
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

  def routeGetFileMeta(implicit ec: ExecutionContext) = get {
    path("api" / "getFileMeta") {
      parameterMap { params =>
        val id = paramsGetString(params, "id", "")
        complete {
          getFileMetaCtl(id) map { json =>
            HttpEntity(ContentTypes.`application/json`, Json.stringify(json))
          }
        }
      }
    }
  }

  def routeGetFile(implicit ec: ExecutionContext) = get {
    path("api" / "getFile") {
      parameterMap { params =>
        val id = paramsGetString(params, "id", "")
        onComplete(getFileCtl(id)) {
          case Success((fileName, fileType, fileSize, fileMetaData, fileBytes, errmsg)) =>
            withPrecompressedMediaTypeSupport {
              var contentType = ContentTypes.`application/octet-stream`
              withPrecompressedMediaTypeSupport
              var headerDisposition = RawHeader("Content-Disposition", s"""attachment; filename="$fileName"""")
              if (fileType == "image/jpeg") {
                contentType = ContentType(MediaTypes.`image/jpeg`)
              } else if (fileType == "image/png") {
                contentType = ContentType(MediaTypes.`image/png`)
              } else if (fileType == "image/gif") {
                contentType = ContentType(MediaTypes.`image/gif`)
              }
              if (contentType != ContentTypes.`application/octet-stream`) {
                headerDisposition = RawHeader("Content-Disposition", s"""inline; filename="$fileName"""")
              }
              respondWithHeaders(headerDisposition) {
                complete(HttpEntity(contentType, ByteString(fileBytes)))
              }
            }
          case Failure(e) =>
            complete((StatusCodes.InternalServerError, s"An error occurred: $e"))
        }
      }
    }
  }

}
