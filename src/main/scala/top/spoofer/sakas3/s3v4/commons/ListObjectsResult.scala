package top.spoofer.sakas3.s3v4.commons

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import scala.language.postfixOps

case class ListObjectsResult(name: String,
                             prefix: String,
                             commonPrefixes: String,
                             continuationToken: String,
                             nextContinuationToken: String,
                             encodingType: String,
                             startAfter: String,
                             keyCount: Int,
                             maxKeys: Int,
                             delimiter: String,
                             isTruncated: Boolean,
                             objects: Seq[ObjectMeta])

object ListObjectsResult {
  private def formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX")

  def apply(node: scala.xml.Elem): ListObjectsResult = {
    val commonPrefixes = node \\ "CommonPrefixes" text
    val delimiter = node \\ "Delimiter" text
    val encodingType = node \\ "Encoding-Type" text
    val isTruncated = (node \\ "IsTruncated" text) toBoolean
    val maxKeys = (node \\ "MaxKeys" text) toInt
    val name = node \\ "Name" text
    val prefix = node \\ "Prefix" text
    val continuationToken = node \\ "ContinuationToken" text
    val keyCount = (node \\ "KeyCount" text) toInt
    val nextContinuationToken = node \\ "NextContinuationToken" text
    val startAfter = node \\ "StartAfter" text
    val objects = (node \\ "Contents").map { elem =>
      ObjectMeta(
        name = elem \\ "Key" text,
        time = ZonedDateTime.parse(elem \\ "LastModified" text, formatter),
        etag = elem \\ "ETag" text,
        uploadId = "",
        size = (elem \\ "Size" text) toLong,
        storageClass = S3StorageClass.withName(elem \\ "StorageClass" text)
      )
    }

    ListObjectsResult(
      commonPrefixes = commonPrefixes,
      delimiter = delimiter,
      encodingType = encodingType,
      isTruncated = isTruncated,
      maxKeys = maxKeys,
      name = name,
      prefix = prefix,
      continuationToken = continuationToken,
      keyCount = keyCount,
      nextContinuationToken = nextContinuationToken,
      startAfter = startAfter,
      objects = objects
    )
  }
}


