package com.cookeem.chat.restful


import akka.actor.ActorSystem
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{HttpRequest, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.stream.ActorMaterializer
import com.cookeem.chat.ChatSession
import org.joda.time.DateTime

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by cookeem on 16/11/2.
  */
object Route {
  def badRequest(request: HttpRequest): StandardRoute = {
    val method = request.method.value.toLowerCase
    val path = request.getUri().path()
    val queryString = request.getUri().rawQueryString().orElse("")
    method match {
      case _ =>
        complete((StatusCodes.NotFound, "404 error, resource not found!"))
    }
  }

  //log duration and request info route
  def logDuration(inner: Route)(implicit ec: ExecutionContext): Route = { ctx =>
    val rejectionHandler = RejectionHandler.default
    val start = System.currentTimeMillis()
    val innerRejectionsHandled = handleRejections(rejectionHandler)(inner)
    mapResponse { resp =>
      val currentTime = new DateTime()
      val currentTimeStr = currentTime.toString("yyyy-MM-dd HH:mm:ss")
      val duration = System.currentTimeMillis() - start
      var remoteAddress = ""
      var userAgent = ""
      var rawUri = ""
      ctx.request.headers.foreach(header => {
        if (header.name() == "X-Real-Ip") {
          remoteAddress = header.value()
        }
        if (header.name() == "User-Agent") {
          userAgent = header.value()
        }
        if (header.name() == "Raw-Request-URI") {
          rawUri = header.value()
        }
      })
      Future {
        val mapPattern = Seq("css", "images", "js", "lib", "manage", "weixin", "upload")
        var isIgnore = false
        mapPattern.foreach(mp =>
          isIgnore = isIgnore || rawUri.startsWith(s"/$mp/")
        )
        if (!isIgnore) {
          println(s"# $currentTimeStr ${ctx.request.uri} [$remoteAddress] [${ctx.request.method.name}] [${resp.status.value}] [$userAgent] took: ${duration}ms")
        }
      }
      resp
    }(innerRejectionsHandled)(ctx)
  }

  def route(implicit system: ActorSystem, materializer: ActorMaterializer, ec: ExecutionContext) = get {
    pathPrefix("ws-chat" / Segment) { chatId =>
      path(PathMatchers.Segment) { username =>
        val chatSession = new ChatSession(username, chatId)
        handleWebSocketMessages(chatSession.chatService(username))
      }
    } ~ pathSingleSlash {
      getFromFile("www/index.html")
    } ~ pathPrefix("") {
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
  } ~ extractRequest { request =>
    badRequest(request)
  }

  def logRoute(implicit system: ActorSystem, materializer: ActorMaterializer, ec: ExecutionContext) = logDuration(route)
}
