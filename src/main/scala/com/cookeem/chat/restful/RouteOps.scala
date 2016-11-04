package com.cookeem.chat.restful

import akka.actor.ActorSystem
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.PathMatchers
import akka.stream.ActorMaterializer
import com.cookeem.chat.ChatSession
import com.cookeem.chat.common.CommonUtils._
import com.cookeem.chat.restful.Controller._
import play.api.libs.json.Json

import scala.concurrent.ExecutionContext

/**
  * Created by cookeem on 16/11/3.
  */
object RouteOps {

  def routeLogic(implicit ec: ExecutionContext, system: ActorSystem, materializer: ActorMaterializer) = {
    routeWebsocket ~
    routeAsset ~
    routeRegisterUser
  }

  def routeWebsocket(implicit ec: ExecutionContext, system: ActorSystem, materializer: ActorMaterializer) = {
    get {
      pathPrefix("ws-chat" / Segment) { chatId =>
        path(PathMatchers.Segment) { username =>
          val chatSession = new ChatSession(username, chatId)
          handleWebSocketMessages(chatSession.chatService(username))
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

  def routeRegisterUser(implicit ec: ExecutionContext) = post {
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


}
