package com.cookeem.chat.redis

import java.util.concurrent.Executors

import com.cookeem.chat.common.CommonUtils._
import com.cookeem.chat.mongo._
import redis.clients.jedis.{Jedis, JedisPool, JedisPoolConfig}

import scala.collection.JavaConversions._
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}

/**
  * Created by cookeem on 16/11/2.
  */
object RedisOps {
  implicit val ec: ExecutionContextExecutor = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(50))

  val poolConfig = new JedisPoolConfig()
  var pool: JedisPool = _
  if (configRedisPass == "") {
    pool = new JedisPool(poolConfig, configRedisHost, configRedisPort, 5000)
  } else {
    pool = new JedisPool(poolConfig, configRedisHost, configRedisPort, 5000, configRedisPass)
  }

  var jedis: Jedis = _
  try {
    jedis = pool.getResource
  } catch {
    case e: Throwable =>
  } finally {
    if (jedis != null) {
      jedis.close()
    }
  }

  def redisInsertOnline(uid: String): Future[String] = Future {
    var jedis: Jedis = null
    var errmsg = ""
    try {
      jedis = pool.getResource
      jedis.sadd("cookim:onlineusers", uid)
    } catch {
      case e: Throwable =>
        errmsg = s"redis update online error: ${e.getClass}, ${e.getMessage}, ${e.getCause}, ${e.getStackTrace.mkString("\n")}"
    } finally {
      if (jedis != null) {
        jedis.close()
      }
    }
    errmsg
  }

  def redisRemoveOnline(uid: String): Future[String] = Future {
    var jedis: Jedis = null
    var errmsg = ""
    try {
      jedis = pool.getResource
      jedis.srem("cookim:onlineusers", uid)
    } catch {
      case e: Throwable =>
        errmsg = s"redis remove online error: ${e.getClass}, ${e.getMessage}, ${e.getCause}, ${e.getStackTrace.mkString("\n")}"
    } finally {
      if (jedis != null) {
        jedis.close()
      }
    }
    errmsg
  }

  def redisGetOnline(): Future[List[String]] = Future {
    var jedis: Jedis = null
    var uids = List[String]()
    try {
      jedis = pool.getResource
      uids = jedis.smembers("cookim:onlineusers").toList
    } catch {
      case e: Throwable =>
        consoleLog("ERROR", s"redis get online error: ${e.getClass}, ${e.getMessage}, ${e.getCause}, ${e.getStackTrace.mkString("\n")}")
    } finally {
      if (jedis != null) {
        jedis.close()
      }
    }
    uids
  }

  def redisUpdateUser(user: User): Future[String] = Future {
    var jedis: Jedis = null
    var errmsg = ""
    try {
      jedis = pool.getResource
      val userMap = classToMap(user)
      jedis.hmset(s"cookim:users:uid:${user._id}:info", userMap)
    } catch {
      case e: Throwable =>
        errmsg = s"redis update user error: ${e.getClass}, ${e.getMessage}, ${e.getCause}, ${e.getStackTrace.mkString("\n")}"
    } finally {
      if (jedis != null) {
        jedis.close()
      }
    }
    errmsg
  }

  def redisGetUser(uid: String): Future[Map[String, String]] = Future {
    var jedis: Jedis = null
    var userMap = Map[String, String]()
    try {
      jedis = pool.getResource
      userMap = jedis.hgetAll(s"cookim:users:uid:$uid:info").toMap
    } catch {
      case e: Throwable =>
        consoleLog("ERROR", s"redis get user error: ${e.getClass}, ${e.getMessage}, ${e.getCause}, ${e.getStackTrace.mkString("\n")}")
    } finally {
      if (jedis != null) {
        jedis.close()
      }
    }
    userMap
  }

  def redisUpdateSession(session: Session): Future[String] = Future {
    var jedis: Jedis = null
    var errmsg = ""
    try {
      jedis = pool.getResource
      val sessionMap = classToMap(session)
      jedis.hmset(s"cookim:sessions:sessionid:${session._id}:info", sessionMap)
    } catch {
      case e: Throwable =>
        errmsg = s"redis update session error: ${e.getClass}, ${e.getMessage}, ${e.getCause}, ${e.getStackTrace.mkString("\n")}"
    } finally {
      if (jedis != null) {
        jedis.close()
      }
    }
    errmsg
  }

  def redisGetSession(sessionid: String): Future[Map[String, String]] = Future {
    var jedis: Jedis = null
    var sessionMap = Map[String, String]()
    try {
      jedis = pool.getResource
      sessionMap = jedis.hgetAll(s"cookim:sessions:sessionid:$sessionid:info").toMap
    } catch {
      case e: Throwable =>
        consoleLog("ERROR", s"redis get session error: ${e.getClass}, ${e.getMessage}, ${e.getCause}, ${e.getStackTrace.mkString("\n")}")
    } finally {
      if (jedis != null) {
        jedis.close()
      }
    }
    sessionMap
  }

}
