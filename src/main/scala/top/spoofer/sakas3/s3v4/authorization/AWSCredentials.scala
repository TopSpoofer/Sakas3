package top.spoofer.sakas3.s3v4.authorization

sealed trait AWSCredentials {
  def accessKeyId: String

  def secretAccessKey: String
}

final case class BasicCredentials(accessKeyId: String, secretAccessKey: String) extends AWSCredentials

object AWSCredentials {
  def apply(accessKeyId: String, secretAccessKey: String): BasicCredentials =
    BasicCredentials(accessKeyId, secretAccessKey)
}