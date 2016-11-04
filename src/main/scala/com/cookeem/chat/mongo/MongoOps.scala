package com.cookeem.chat.mongo

import com.cookeem.chat.common.CommonUtils._

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
object MongoOps {
  val dbName = configMongoDbname
  val colUsersName = "users"
  val colSessionsName = "sessions"
  val colMessagesName = "messages"
  val colInboxName = "inbox"
  val colOnlineName = "online"

  implicit val ec: ExecutionContextExecutor = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(50))
  val mongoUri = configMongoUri
  val driver = MongoDriver()
  val parsedUri = MongoConnection.parseURI(mongoUri)
  val connection = parsedUri.map(driver.connection)
  val futureConnection = Future.fromTry(connection)
  val cookimDB = futureConnection.map(_.database(dbName)).flatMap(f => f)

  val usersCollection = cookimDB.map(_.collection[BSONCollection](colUsersName))
  val sessionsCollection = cookimDB.map(_.collection[BSONCollection](colSessionsName))
  val messagesCollection = cookimDB.map(_.collection[BSONCollection](colMessagesName))
  val inboxCollection = cookimDB.map(_.collection[BSONCollection](colInboxName))
  val onlineCollection = cookimDB.map(_.collection[BSONCollection](colOnlineName))

  implicit def userHandler = Macros.handler[User]
  implicit def sessionHandler = Macros.handler[Session]
  implicit def fileInfoHandler = Macros.handler[FileInfo]
  implicit def messageHandler = Macros.handler[Message]
  implicit def inboxHandler = Macros.handler[Inbox]
  implicit def onlineHandler = Macros.handler[Online]

  //create collection and index
  /**
  * @param colName: String, collection name to create
  * @param indexSettings: Array[(indexField: String, sort: Int, unique: Boolean, expireAfterSeconds: Int)], index setting
  * @return Future[errmsg: String], if no error, errmsg is empty
  */
  def createIndex(colName: String, indexSettings: Array[(String, Int, Boolean, Int)]): Future[String] = {
    var errmsg = ""
    var indexSettingDoc = array()
    indexSettings.foreach { case (indexCol, indexMode, unique, expireAfterSeconds) =>
      if (expireAfterSeconds > 0) {
        indexSettingDoc = indexSettingDoc.add(
          document(
            "key" -> document(indexCol -> indexMode),
            "name" -> s"index-$colName-$indexCol",
            "unique" -> unique,
            "expireAfterSeconds" -> expireAfterSeconds
          )
        )
      } else {
        indexSettingDoc = indexSettingDoc.add(
          document(
            "key" -> document(indexCol -> indexMode),
            "name" -> s"index-$colName-$indexCol",
            "unique" -> unique
          )
        )
      }
    }
    val createResult = for {
      db <- cookimDB
      doc <- {
        val runner: CommandWithPackRunner[BSONSerializationPack.type] = Command.run(BSONSerializationPack, FailoverStrategy.default)
        val commandDoc = document(
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
        s"create index error: $e"
    }
  }

  //insert single document into collection
  /**
    * @param futureCollection: Future[BSONCollection], collection to insert
    * @param record: T, record is BaseMongoObj
    * @return Future[(id: String, errmsg: String)], inserted id string and errmsg
    */
  def insertCollection[T <: BaseMongoObj](futureCollection: Future[BSONCollection], record: T)(implicit handler: BSONDocumentReader[T] with BSONDocumentWriter[T] with BSONHandler[BSONDocument, T]): Future[(String, String)] = {
    val recordIns = record
    recordIns._id = BSONObjectID.generate().stringify
    val insertResult = for {
      col <- futureCollection
      wr <- col.insert[T](recordIns)
    } yield {
      var errmsg = ""
      var id = ""
      if (wr.ok) {
        id = recordIns._id
      } else {
        errmsg = s"insert ${record.getClass} record error"
      }
      (id, errmsg)
    }
    insertResult.recover { case e: Throwable =>
      ("", s"insert ${record.getClass} record error: $e")
    }
  }

  def bulkInsertCollection[T <: BaseMongoObj](futureCollection: Future[BSONCollection], records: List[T])(implicit handler: BSONDocumentReader[T] with BSONDocumentWriter[T] with BSONHandler[BSONDocument, T]) = {
    val recordsIns = records.map { record =>
      val recordIns = record
      recordIns._id = BSONObjectID.generate().stringify
      recordIns
    }
    val bulkResult = for {
      col <- futureCollection
      mwr <- {
        val docs = recordsIns.map(implicitly[col.ImplicitlyDocumentProducer](_))
        col.bulkInsert(ordered = false)(docs: _*)
      }
    } yield {
      UpdateResult(n = mwr.n, errmsg = mwr.errmsg.getOrElse(""))
    }
    bulkResult.recover { case e: Throwable =>
      ("", s"bulk insert records error: $e")
    }
  }

  //find in collection
  /**
    * @param futureCollection: Future[BSONCollection], collection to insert
    * @param selector: BSONDocument, filter
    * @param count = -1: Int, return record count
    * @param sort: BSONDocument = document(), sort
    * @return Future[List[T] ], return the record list
    */
  def findCollection[T <: BaseMongoObj](futureCollection: Future[BSONCollection], selector: BSONDocument, count: Int = -1, sort: BSONDocument = document())(implicit handler: BSONDocumentReader[T] with BSONDocumentWriter[T] with BSONHandler[BSONDocument, T]): Future[List[T]] = {
    val findResult = for {
      col <- futureCollection
      rs <- col.find(selector).sort(sort).cursor[T]().collect(count, Cursor.FailOnError[List[T]]())
    } yield {
      rs
    }
    findResult.recover { case e: Throwable =>
      List[T]()
    }
  }

  //update in collection
  /**
    * @param futureCollection: Future[BSONCollection], collection to update
    * @param selector: BSONDocument, filter
    * @param update: BSONDocument, update info
    * @param multi: Boolean = false, update multi records
    * @return Future[UpdateResult], return the update result
    */
  def updateCollection(futureCollection: Future[BSONCollection], selector: BSONDocument, update: BSONDocument, multi: Boolean = false): Future[UpdateResult] = {
    val updateResult = for {
      col <- futureCollection
      uwr <- col.update(selector, update, multi = multi)
    } yield {
      UpdateResult(
        n = uwr.nModified,
        errmsg = uwr.errmsg.getOrElse("")
      )
    }
    updateResult.recover { case e: Throwable =>
      UpdateResult(
        n = 0,
        errmsg = s"update collection error: $e"
      )
    }
  }

  //remove in collection
  /**
    * @param futureCollection: Future[BSONCollection], collection to update
    * @param selector: BSONDocument, filter
    * @param firstMatchOnly: Boolean = false, only remove fisrt match record
    * @return Future[UpdateResult], return the update result
    */
  def removeCollection(futureCollection: Future[BSONCollection], selector: BSONDocument, firstMatchOnly: Boolean = false): Future[UpdateResult] = {
    val removeResult = for {
      col <- futureCollection
      wr <- col.remove(selector, firstMatchOnly = firstMatchOnly)
    } yield {
      UpdateResult(
        n = wr.n,
        errmsg = wr.writeErrors.map(_.errmsg).mkString
      )
    }
    removeResult.recover { case e: Throwable =>
      UpdateResult(
        n = 0,
        errmsg = s"remove collection item error: $e"
      )
    }
  }
}
