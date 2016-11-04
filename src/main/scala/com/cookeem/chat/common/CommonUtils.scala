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
  val config = ConfigFactory.parseFile(new File("conf/application.conf"))

  val configMongo = config.getConfig("mongodb")
  val configMongoDbname = configMongo.getString("dbname")
  val configMongoUri = configMongo.getString("uri")

  val configRedis = config.getConfig("redis")
  val configRedisHost = configRedis.getString("redis-host")
  val configRedisPort = configRedis.getInt("redis-port")
  var configRedisPass = configRedis.getString("redis-pass")

  case class CustomException(message: String = "", cause: Throwable = null) extends Exception(message, cause)

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

  //从参数Map中获取Int
  def paramsGetInt(params: Map[String, String], key: String, default: Int): Int = {
    var ret = default
    if (params.contains(key)) {
      try {
        ret = params(key).toInt
      } catch {
        case e: Throwable =>
      }
    }
    ret
  }

  //从参数Map中获取String
  def paramsGetString(params: Map[String, String], key: String, default: String): String = {
    var ret = default
    if (params.contains(key)) {
      ret = params(key)
    }
    ret
  }

  def sha1(str: String) = MessageDigest.getInstance("SHA-1").digest(str.getBytes).map("%02x".format(_)).mkString

  def md5(str: String) = MessageDigest.getInstance("MD5").digest(str.getBytes).map("%02x".format(_)).mkString

  def isEmail(email: String): Boolean = {
    """(?=[^\s]+)(?=(\w+)@([\w\.]+))""".r.findFirstIn(email).isDefined
  }

  def classToMap(c: AnyRef): Map[String, String] = {
    c.getClass.getDeclaredFields.map{ f =>
      f.setAccessible(true)
      f.getName -> f.get(c).toString
    }.toMap
  }

}
