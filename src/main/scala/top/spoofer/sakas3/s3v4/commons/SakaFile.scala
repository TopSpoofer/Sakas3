package top.spoofer.sakas3.s3v4.commons

import akka.stream.scaladsl.Source
import akka.util.ByteString

/**
  * file info
  * @param name file raw name
  * @param content file content, is Source
  * @param size file content size, can't get the size from the stream...
  */
case class SakaFile(name: String, content: Source[ByteString, Any], size: Long)
