package com.cookeem.chat.mongo

import java.util.concurrent.Executors

import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.api.commands.Command
import reactivemongo.api.commands.Command.CommandWithPackRunner
import reactivemongo.api._
import reactivemongo.bson._

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}

/**
  * Created by cookeem on 16/10/27.
  */
case class UpdateResult(ok: Boolean, nModified: Int, errmsg: String, ids: Seq[BSONValue])

object MongoOps {
  val dbName = "cookim"
  val colUsersName = "users"
  val colSessionsName = "sessions"
  val colMessagesName = "messages"

  implicit val ec: ExecutionContextExecutor = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(50))
  val mongoUri = "mongodb://localhost:27017/local"
  val driver = MongoDriver()
  val parsedUri = MongoConnection.parseURI(mongoUri)
  val connection = parsedUri.map(driver.connection)
  val futureConnection = Future.fromTry(connection)
  val cookimDB = futureConnection.map(_.database(dbName)).flatMap(f => f)

  //create collection and index
  /**
  * @param colName: String, collection name to create
  * @param indexSettings: Array[(indexField: String, sort: Int, unique: Boolean)], index setting
  * @return Future[errmsg: String], if no error, errmsg is empty
  */
  def createIndex(colName: String, indexSettings: Array[(String, Int, Boolean)]): Future[String] = {
    var errmsg = ""
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
    val createResult = for {
      db <- cookimDB
      doc <- {
        val runner: CommandWithPackRunner[BSONSerializationPack.type] = Command.run(BSONSerializationPack, FailoverStrategy.default)
        val commandDoc = BSONDocument(
          "createIndexes" -> colName,
          "indexes" -> indexSettingDoc
        )
        runner(db, runner.rawCommand(commandDoc)).one[BSONDocument](ReadPreference.Primary)
      }
    } yield {
      if (doc.get("errmsg").isDefined) {
        errmsg = doc.getAs[String]("errmsg").getOrElse("")
      } else {
        errmsg = ""
      }
      errmsg
    }
    createResult.recover { case e: Throwable =>
        s"create index error: ${e.getClass}, ${e.getMessage}, ${e.getCause}, ${e.getStackTrace.mkString("\n")}"
    }
  }

  //update or insert single document into collection
  /**
    * @param futureCollection: Future[BSONCollection], collection name to update or insert
    * @param selector: BSONDocument, selector to find, if found update, if not found then insert
    *                BSONDocument("field" -> "content")
    * @param doc: BSONDocument, update or insert document
    *                BSONDocument(
    *                 "field1" -> "content1",
    *                 "field2" -> "content2"
    *                )
    * @param upsert: Boolean
    * @return Future[BSONValue], update or insert BSONObjectID, if error return BSONNull
    */
  def upsertCollection(futureCollection: Future[BSONCollection], selector: BSONDocument, doc: BSONDocument, upsert: Boolean): Future[BSONValue] = {
    val upsertResult = for {
      col <- futureCollection
      fr <- col.findAndUpdate(selector, doc, upsert = upsert)
    } yield {
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
    upsertResult.recover { case e: Throwable =>
      BSONNull
    }
  }

  //insert single document into collection
  /**
    * @param futureCollection: Future[BSONCollection], collection name to insert
    * @param doc: BSONDocument, insert document
    *                BSONDocument(
    *                 "field1" -> "content1",
    *                 "field2" -> "content2"
    *                )
    * @return Future[(id: BSONValue)]
    */
  def insertCollection(futureCollection: Future[BSONCollection], doc: BSONDocument): Future[BSONValue] = {
    val id = BSONObjectID.generate()
    val docIns = doc.merge(BSONDocument("_id" -> id))
    val insertResult = for {
      col <- futureCollection
      wr <- col.insert(docIns)
    } yield {
      if (wr.ok) {
        id
      } else {
        BSONNull
      }
    }
    insertResult.recover { case e: Throwable =>
      BSONNull
    }
  }

  //find collection
  /**
    * @param futureCollection: Future[BSONCollection], collection name to find
    * @param selector: BSONDocument, selector to find, if found update, if not found then insert
    *                BSONDocument("field" -> "content")
    * @param count: Int = -1, return records number, if -1 mean unlimit
    * @return Future[ List[BSONDocument] ], return the list of BSONDocument
    */
  def findCollection(futureCollection: Future[BSONCollection], selector: BSONDocument, count: Int = -1): Future[List[BSONDocument]] = {
    val findResult = for {
      col <- futureCollection
      rs <- col.find(selector).cursor[BSONDocument]().collect(count, Cursor.FailOnError[List[BSONDocument]]())
    } yield {
      rs
    }
    findResult.recover { case e: Throwable =>
      List[BSONDocument]()
    }
  }

  def updateCollection(futureCollection: Future[BSONCollection], selector: BSONDocument, update: BSONDocument, upsert: Boolean, multi: Boolean): Future[UpdateResult] = {
    val updateResult = for {
      col <- futureCollection
      uwr <- col.update(selector, update, upsert = true, multi = true)
    } yield {
      UpdateResult(
        ok = uwr.ok,
        nModified = uwr.nModified,
        errmsg = uwr.errmsg.getOrElse(""),
        ids = uwr.upserted.map(_._id)
      )
    }
    updateResult.recover { case e: Throwable =>
      UpdateResult(
        ok = false,
        nModified = 0,
        errmsg = s"update collection error: ${e.getClass}, ${e.getMessage}, ${e.getCause}, ${e.getStackTrace.mkString("\n")}",
        ids = Seq[BSONValue]()
      )
    }
  }


}
