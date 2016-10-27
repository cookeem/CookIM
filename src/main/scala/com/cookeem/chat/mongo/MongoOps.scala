package com.cookeem.chat.mongo

import java.util.concurrent.Executors

import play.api.libs.json._
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.api.commands.Command
import reactivemongo.api.commands.Command.CommandWithPackRunner
import reactivemongo.api._
import reactivemongo.bson._
import reactivemongo.play.json._

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by cookeem on 16/10/27.
  */
object MongoOps {
  val dbName = "cookim"
  val colUsersName = "users"
  implicit val ec = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(50))
  val mongoUri = "mongodb://localhost:27017/local"
  val driver = MongoDriver()
  val parsedUri = MongoConnection.parseURI(mongoUri)
  val connection = parsedUri.map(driver.connection)
  val futureConnection = Future.fromTry(connection)
  val cookimDB = futureConnection.flatMap(_.database(dbName))

  val usersCollection = cookimDB.map(_.collection[BSONCollection](colUsersName))

  def initUsersCollection() = {
    cookimDB.foreach { db =>
      val runner: CommandWithPackRunner[BSONSerializationPack.type] = Command.run(BSONSerializationPack, FailoverStrategy.strict)
      val commandDoc = BSONDocument(
        "createIndexes" -> colUsersName,
        "indexes" -> BSONArray(
          BSONDocument(
            "key" -> BSONDocument(
              "login" -> 1
            ),
            "name" -> s"index-$colUsersName-login",
            "unique" -> BSONBoolean(false)
          )
        )
      )
      val futureDoc = runner(db, runner.rawCommand(commandDoc)).one[BSONDocument](ReadPreference.primaryPreferred)
      futureDoc.foreach { doc =>
        val json = Json.toJson(doc)
        println(Json.prettyPrint(json))
      }
    }
  }

}
