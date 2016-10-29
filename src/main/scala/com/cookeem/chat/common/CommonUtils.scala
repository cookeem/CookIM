package com.cookeem.chat.common

import java.io.File
import java.security.MessageDigest

import com.typesafe.config.ConfigFactory
import org.joda.time.DateTime
import play.api.libs.json.{JsArray, JsNumber, JsString, JsValue}

/**
  * Created by cookeem on 16/9/25.
  */
object CommonUtils {
  case class CustomException(message: String = "", cause: Throwable = null) extends Exception(message, cause)

  val config = ConfigFactory.parseFile(new File("conf/application.conf"))

  def consoleLog(logType: String, msg: String) = {
    val timeStr = new DateTime().toString("yyyy-MM-dd HH:mm:ss")
    println(s"[$logType] $timeStr: $msg")
  }

  def md5(bytes: Array[Byte]) = {
    MessageDigest.getInstance("MD5").digest(bytes).map("%02x".format(_)).mkString
  }

  def getJsonString(json: JsValue, field: String, default: String = ""): String = {
    val ret = (json \ field).getOrElse(JsString(default)).as[String]
    ret
  }

  def getJsonInt(json: JsValue, field: String, default: Int = 0): Int = {
    val ret = (json \ field).getOrElse(JsNumber(default)).as[Int]
    ret
  }

  def getJsonLong(json: JsValue, field: String, default: Long = 0L): Long = {
    val ret = (json \ field).getOrElse(JsNumber(default)).as[Long]
    ret
  }

  def getJsonDouble(json: JsValue, field: String, default: Double = 0D): Double = {
    val ret = (json \ field).getOrElse(JsNumber(default)).as[Double]
    ret
  }

  def getJsonSeq(json: JsValue, field: String, default: Seq[JsValue] = Seq[JsValue]()): Seq[JsValue] = {
    val ret = (json \ field).getOrElse(JsArray(default)).as[Seq[JsValue]]
    ret
  }

  def sha1(str: String) = MessageDigest.getInstance("SHA-1").digest(str.getBytes).map("%02x".format(_)).mkString

  def md5(str: String) = MessageDigest.getInstance("MD5").digest(str.getBytes).map("%02x".format(_)).mkString

  def isEmail(email: String): Boolean = {
    """(\w+)@([\w\.]+)""".r.unapplySeq(email).isDefined
  }

}
