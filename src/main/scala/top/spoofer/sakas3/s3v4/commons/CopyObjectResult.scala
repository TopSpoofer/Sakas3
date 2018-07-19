package top.spoofer.sakas3.s3v4.commons

/**
  * when copy object succeed, return this
  * @param bucket destination bucket
  * @param objectName destination object name
  */
case class CopyObjectResult(bucket: String, objectName: String)
