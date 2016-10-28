package com.cookeem.chat.mongo

import java.util.concurrent.Executors

import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.api.commands.{Command, UpdateWriteResult}
import reactivemongo.api.commands.Command.CommandWithPackRunner
import reactivemongo.api._
import reactivemongo.bson._

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

  def createIndex(colName: String, indexSettings: Array[(String, Int, Boolean)]): Future[String] = {
    cookimDB.map { db =>
      var errmsg = ""
      val runner: CommandWithPackRunner[BSONSerializationPack.type] = Command.run(BSONSerializationPack, FailoverStrategy.strict)
      var indexSettingDoc = BSONArray()
      indexSettings.foreach { case (indexCol, indexMode, unique) =>
        indexSettingDoc = indexSettingDoc.add(
          BSONDocument(
            "key" -> BSONDocument(indexCol -> indexMode),
            "name" -> s"index-$colName-$indexCol",
            "unique" -> BSONBoolean(unique)
          )
        )
      }
      val commandDoc = BSONDocument(
        "createIndexes" -> colName,
        "indexes" -> indexSettingDoc
      )
      val futureDoc = runner(db, runner.rawCommand(commandDoc)).one[BSONDocument](ReadPreference.primaryPreferred)
      futureDoc.map { doc =>
        if (doc.get("errmsg").isDefined) {
          errmsg = doc.getAs[String]("errmsg").getOrElse("")
        } else {
          errmsg = ""
        }
        errmsg
      }
    }.recover { case e: Throwable =>
      Future(s"${e.getClass}, ${e.getMessage}, ${e.getCause}, ${e.getStackTrace.mkString("\n")}")
    }.flatMap(f => f)
  }

  def upsertCollection(futureCollection: Future[BSONCollection], selector: BSONDocument, update: BSONDocument, upsert: Boolean): Future[BSONValue] = {
    futureCollection.map { col =>
      col.findAndUpdate(
        selector,
        update,
        upsert = upsert
      ).map{ fr =>
        var id: BSONValue = BSONNull
        fr.value match {
          case Some(frv) =>
            id = frv.getTry("_id").getOrElse(BSONNull)
          case _ =>
            fr.lastError match {
              case Some(frl) =>
                frl.upsertedId match {
                  case Some(v) =>
                    id = v.asInstanceOf[BSONValue]
                  case _ =>
                }
              case _ =>
            }
        }
        id
      }
    }.flatMap(f => f)
  }

  def findCollection(futureCollection: Future[BSONCollection], selector: BSONDocument, count: Int = -1): Future[List[BSONDocument]] = {
    futureCollection.map(
      _.find(selector).cursor[BSONDocument]().collect(count, Cursor.FailOnError[List[BSONDocument]]())
    ).flatMap(f => f)
  }

  def pushArrayCollection(futureCollection: Future[BSONCollection], selector: BSONDocument, pushDoc: BSONDocument, upsert: Boolean, multi: Boolean): Future[UpdateWriteResult] = {
    futureCollection.map(
      _.update(
        selector,
        BSONDocument("$push" -> pushDoc),
        upsert = true,
        multi = true
      )
    ).flatMap(f => f)
  }

  def updateCollection(futureCollection: Future[BSONCollection], selector: BSONDocument, update: BSONDocument, upsert: Boolean, multi: Boolean): Future[UpdateWriteResult] = {
    futureCollection.map(
      _.update(
        selector,
        update,
        upsert = true,
        multi = true
      )
    ).flatMap(f => f)
  }






}
