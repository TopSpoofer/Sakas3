package top.spoofer.sakas3.s3v4.authorization

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import top.spoofer.sakas3.s3v4.commons.HexCoder

case class CredentialScope(date: LocalDate, awsRegion: String = "us-east-1", awsService: String = "s3") {
  lazy val formattedDate: String = date.format(DateTimeFormatter.BASIC_ISO_DATE)

  def scopeString = s"$formattedDate/$awsRegion/$awsService/aws4_request"
}

case class SigningKey(credentials: AWSCredentials, scope: CredentialScope, algorithm: String = "HmacSHA256") {
  lazy val rawKey = new SecretKeySpec(s"AWS4${credentials.secretAccessKey}".getBytes, algorithm)
  lazy val dateKey: SecretKeySpec = wrapSignature(rawKey, scope.formattedDate.getBytes)
  lazy val dateRegionKey: SecretKeySpec = wrapSignature(dateKey, scope.awsRegion.getBytes)
  lazy val dateRegionServiceKey: SecretKeySpec = wrapSignature(dateRegionKey, scope.awsService.getBytes)
  lazy val signingKey: SecretKeySpec = wrapSignature(dateRegionServiceKey, "aws4_request".getBytes)

  def signature(message: Array[Byte]): Array[Byte] = signWithKey(signingKey, message)

  def hexEncodedSignature(message: Array[Byte]): String = HexCoder.encodeHex(signature(message))

  def credentialString: String = s"${credentials.accessKeyId}/${scope.scopeString}"

  def wrapSignature(signature: SecretKeySpec, message: Array[Byte]): SecretKeySpec =
    new SecretKeySpec(signWithKey(signature, message), algorithm)

  def signWithKey(key: SecretKeySpec, message: Array[Byte]): Array[Byte] = {
    val mac = Mac.getInstance(algorithm)
    mac.init(key)
    mac.doFinal(message)
  }
}
