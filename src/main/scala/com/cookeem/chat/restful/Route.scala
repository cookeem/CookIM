package com.cookeem.chat.restful


import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.{HttpRequest, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.stream.ActorMaterializer
import com.cookeem.chat.restful.RouteOps._
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
        //this setting come from nginx
        if (header.name() == "X-Real-Ip") {
          remoteAddress = header.value()
        }
        if (header.name() == "User-Agent") {
          userAgent = header.value()
        }
        //you must set akka.http.raw-request-uri-header=on config
        if (header.name() == "Raw-Request-URI") {
          rawUri = header.value()
        }
      })
      Future {
        val mapPattern = Seq("chat", "upload")
        var isIgnore = false
        mapPattern.foreach(pattern =>
          isIgnore = isIgnore || rawUri.startsWith(s"/$pattern")
        )
        if (!isIgnore) {
          println(s"# $currentTimeStr ${ctx.request.uri} [$remoteAddress] [${ctx.request.method.name}] [${resp.status.value}] [$userAgent] took: ${duration}ms")
        }
      }
      resp
    }(innerRejectionsHandled)(ctx)
  }

  def routeRoot(implicit ec: ExecutionContext, system: ActorSystem, materializer: ActorMaterializer, notificationActor: ActorRef) = {
    routeLogic ~
    extractRequest { request =>
      badRequest(request)
    }
  }

  def logRoute(implicit ec: ExecutionContext, system: ActorSystem, materializer: ActorMaterializer, notificationActor: ActorRef) = logDuration(routeRoot)
}
