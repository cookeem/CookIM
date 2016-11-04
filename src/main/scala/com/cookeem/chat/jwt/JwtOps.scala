package com.cookeem.chat.jwt

import java.util.Date

import io.jsonwebtoken.{Jwts, SignatureAlgorithm}
import io.jsonwebtoken.impl.crypto.MacProvider

import scala.collection.JavaConversions._
/**
  * Created by cookeem on 16/11/3.
  */
object JwtOps {
  val key = MacProvider.generateKey()
  val expireMs = 15 * 60 * 1000L

  def encodeJwt(payload: Map[String, Any], expireMs: Long = expireMs): String = {
    val jwtBuilder = Jwts.builder()
      .setHeaderParams(payload.asInstanceOf[Map[String, AnyRef]])
      .signWith(SignatureAlgorithm.HS512, key)
    if (expireMs > 0) {
      jwtBuilder.setExpiration(new Date(System.currentTimeMillis() + expireMs))
    }
    jwtBuilder.compact()
  }

  def decodeJwt(jwtStr: String): Map[String, Any] = {
    Jwts.parser().setSigningKey(key).parse(jwtStr).getHeader.entrySet().map { t => (t.getKey, t.getValue)}.toMap[String, Any]
  }
}
