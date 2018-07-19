package top.spoofer.sakas3.s3v4.commons

import java.util.Base64

import scala.util.Try

object Base64Coder {
  def encoded(data: String): String = new String(Base64.getEncoder.encode(data.getBytes("UTF-8")), "UTF-8")
  def decoded(data: String): Try[String]= Try { new String(Base64.getDecoder.decode(data.getBytes("UTF-8")), "UTF-8") }
}
