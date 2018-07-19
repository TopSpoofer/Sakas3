package top.spoofer.sakas3.s3v4.authorization

import akka.http.scaladsl.model.headers._

import scala.util.Try

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

object `X-Amz-Date` extends ModeledCustomHeaderCompanion[`X-Amz-Date`] {
  def dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("YYYYMMdd'T'HHmmssX") //todo refactor

  override def name: String = "x-amz-date"

  override def parse(value: String): Try[`X-Amz-Date`] = Try {
    new `X-Amz-Date`(ZonedDateTime.parse(value, dateFormatter))
  }
}

final case class `X-Amz-Date`(date: ZonedDateTime) extends ModeledCustomHeader[`X-Amz-Date`] {
  override def companion = `X-Amz-Date`

  override def value(): String = `X-Amz-Date`.dateFormatter.format(date)

  override def renderInRequests(): Boolean = true

  override def renderInResponses(): Boolean = false
}


object `X-Amz-Content-Sha256` extends ModeledCustomHeaderCompanion[`X-Amz-Content-Sha256`] {
  override def name: String = "x-amz-content-sha256"

  override def parse(value: String): Try[`X-Amz-Content-Sha256`] = Try {
    new `X-Amz-Content-Sha256`(value)
  }
}

final case class `X-Amz-Content-Sha256`(value: String) extends ModeledCustomHeader[`X-Amz-Content-Sha256`] {
  override def companion: ModeledCustomHeaderCompanion[`X-Amz-Content-Sha256`] = `X-Amz-Content-Sha256`

  override def renderInRequests(): Boolean = true

  override def renderInResponses(): Boolean = false
}

object Authorization extends ModeledCustomHeaderCompanion[Authorization] {
  val pattern = "(.*) Credential=(.*), SignedHeaders=(.*), Signature=(.*)".r

  override def name: String = "Authorization"

  override def parse(value: String): Try[Authorization] = Try {
    value match {
      case pattern(algorithm, credential, signedHeaders, sign) =>
        new Authorization(algorithm, credential, signedHeaders, sign)
    }
  }
}

final case class Authorization(algorithm: String,
                               credential: String,
                               signedHeaders: String,
                               sign: String) extends ModeledCustomHeader[Authorization] {

  override def companion: ModeledCustomHeaderCompanion[Authorization] = Authorization

  override def renderInRequests(): Boolean = true

  override def renderInResponses(): Boolean = false

  override def value(): String = {
    s"$algorithm Credential=${credential}, SignedHeaders=${signedHeaders}, Signature=$sign"
  }
}

object `X-Amz-Copy-Source` extends ModeledCustomHeaderCompanion[`X-Amz-Copy-Source`] {
  val pattern = "//(.*)//(.*)".r
  override def name: String = "x-amz-copy-source"

  override def parse(value: String): Try[`X-Amz-Copy-Source`] = Try {
    value match {
      case pattern(bucket, objectName) =>
        new `X-Amz-Copy-Source`(bucket,objectName)
    }
  }
}

final case class `X-Amz-Copy-Source`(bucket:String, objectName:String) extends ModeledCustomHeader[`X-Amz-Copy-Source` ] {
  override def companion: ModeledCustomHeaderCompanion[`X-Amz-Copy-Source`] = `X-Amz-Copy-Source`

  override def value(): String = s"/$bucket/$objectName"

  override def renderInRequests(): Boolean = true

  override def renderInResponses(): Boolean = false
}