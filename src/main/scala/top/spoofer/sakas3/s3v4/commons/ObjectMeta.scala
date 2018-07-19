package top.spoofer.sakas3.s3v4.commons

import java.time.ZonedDateTime

import top.spoofer.sakas3.s3v4.commons.S3StorageClass.S3StorageClass

case class Owner(id: String, displayName: Option[String])

object S3StorageClass extends Enumeration {
  type S3StorageClass = Value
  val STANDARD: Value = Value("STANDARD")
  val STANDARD_IA: Value = Value("STANDARD_IA")
  val REDUCED_REDUNDANCY: Value = Value("REDUCED_REDUNDANCY")
  val GLACIER: Value = Value("GLACIER")
}

/**
  * object meta info
  *
  * @param name         object name
  * @param time         last modified time or inited time
  * @param etag         The entity tag is an MD5 hash of the object.
  * @param uploadId     the id when object uses multipart uploads method
  * @param size         object length
  * @param storageClass STANDARD | STANDARD_IA | REDUCED_REDUNDANCY | GLACIER
  */
case class ObjectMeta(name: String,
                      time: ZonedDateTime,
                      etag: String,
                      uploadId: String,
                      size: Long,
                      storageClass: S3StorageClass)
