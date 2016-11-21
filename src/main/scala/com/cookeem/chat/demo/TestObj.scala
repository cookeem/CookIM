package com.cookeem.chat.demo

import java.util.Date

import io.jsonwebtoken.{Jwts, SignatureAlgorithm}
import io.jsonwebtoken.impl.crypto.MacProvider

import scala.collection.JavaConversions._
/**
  * Created by cookeem on 16/9/26.
  */
object TestObj extends App {
  val key = MacProvider.generateKey()
  val str = "Haijian"
  val map = Map(
    "username" -> "haijian",
    "uid" -> 1234,
    "lat" -> 12.34D,
    "lng" -> 56.78F,
    "long" -> System.currentTimeMillis(),
    "date" -> new Date(),
    "friends" -> Array(1, 2, 3, 4)
  ).asInstanceOf[Map[String, AnyRef]]
  val compactJws = Jwts
    .builder()
    .setExpiration(new Date(System.currentTimeMillis() + 120 * 1000))
    .setSubject(str)
    .setHeaderParams(map)
    .signWith(SignatureAlgorithm.HS512, key)
    .compact()

  println(Jwts.parser().setSigningKey(key).parseClaimsJws(compactJws).getBody.getSubject)
  val header = Jwts.parser().setSigningKey(key).parse(compactJws).getHeader.entrySet().map { t => (t.getKey, t.getValue)}.toMap[String, Any]


  import akka.actor._

  class TestActor extends Actor with ActorLogging {
    def receive = {
      case s: String =>
        println(s"receive $s")
      case _ =>
        log.error("Receive type error!")
    }
  }

  object TestActor {
    def props = Props[TestActor]
  }

  class TestClass {
    var name = ""

    def helloName() = {
      val system: ActorSystem = ActorSystem("system")
      val testActor = system.actorOf(TestActor.props, "test-actor")
      testActor ! name
    }
  }

  val c = new TestClass

  c.name = "haijian"

  c.helloName()
}
