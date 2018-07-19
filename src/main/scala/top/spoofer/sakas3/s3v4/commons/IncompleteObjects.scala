package top.spoofer.sakas3.s3v4.commons

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import scala.language.postfixOps

case class IncompleteObjects(maxUploads: Int, size: Int, objects: Seq[ObjectMeta])

object IncompleteObjects {
  private def formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX")

  def apply(responseXml: scala.xml.Elem): IncompleteObjects = {
    val maxUploads = (responseXml \\ "MaxUploads" text) toInt
    val uploads = responseXml \\ "Upload"
    val objects = uploads.map(elem => {
      val key = elem \\ "Key" text
      val uploadId = elem \\ "UploadId" text
      val initiated = elem \\ "Initiated" text
      val storageClass = S3StorageClass.withName(elem \\ "StorageClass" text)
      val etag = ""
      val size = 0
      ObjectMeta(key, ZonedDateTime.parse(initiated, formatter), etag, uploadId, size, storageClass)
    })
    IncompleteObjects(maxUploads, objects.size, objects)
  }
}