package top.spoofer.sakas3.commons

import akka.http.scaladsl.model.headers.{ModeledCustomHeader, ModeledCustomHeaderCompanion}
import scala.util.Try

object ExtendHttpHeaders {
  object `X-File-Size` extends ModeledCustomHeaderCompanion[`X-File-Size`] {
    override def name: String = "x-file-size"

    override def parse(value: String): Try[`X-File-Size`] = Try {
      new `X-File-Size`(value.toLong)
    }
  }

  final case class `X-File-Size`(size: Long) extends ModeledCustomHeader[`X-File-Size`] {
    override def companion: ExtendHttpHeaders.`X-File-Size`.type = `X-File-Size`

    override def value(): String = size.toString

    override def renderInRequests(): Boolean = true

    override def renderInResponses(): Boolean = false
  }
}
