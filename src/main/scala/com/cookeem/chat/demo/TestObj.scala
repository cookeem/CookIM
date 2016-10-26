package com.cookeem.chat.demo

import java.io.File
import java.security.MessageDigest
import org.apache.commons.io.FileUtils

/**
  * Created by cookeem on 16/9/26.
  */
object TestObj extends App {
  val filename: String = ""
  val file: File = new File(filename)
  val bytes: Array[Byte] = FileUtils.readFileToByteArray(file)
  val md5: String = MessageDigest.getInstance("MD5").digest(bytes).map("%02x".format(_)).mkString
}
