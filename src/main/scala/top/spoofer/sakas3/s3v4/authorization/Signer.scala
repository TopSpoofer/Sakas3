package top.spoofer.sakas3.s3v4.authorization

import akka.http.scaladsl.model.{HttpHeader, HttpRequest}
import java.security.MessageDigest
import java.time.format.DateTimeFormatter
import java.time.{ZoneOffset, ZonedDateTime}

import top.spoofer.sakas3.s3v4.commons.HexCoder

object Signer {
  val UNSIGNED_PAYLOAD = "UNSIGNED-PAYLOAD"
  val ALGORITHM = "AWS4-HMAC-SHA256"

  lazy val dateFormatter: ThreadLocal[DateTimeFormatter] = ThreadLocal.withInitial[DateTimeFormatter](() => DateTimeFormatter.ofPattern("YYYYMMdd'T'HHmmssX"))

  //region signedRequest
  def signedRequest(request: HttpRequest,
                    key: SigningKey,
                    date: ZonedDateTime = ZonedDateTime.now(ZoneOffset.UTC)): HttpRequest = {
    val requestWithS3Headers = appendS3HeadersToHttpRequest(request, date)

    val authHeader = createAuthorizationHeader(ALGORITHM, key, date, CanonicalRequest.from(requestWithS3Headers))
    requestWithS3Headers.withHeaders(requestWithS3Headers.headers :+ authHeader)
  }

  private def appendS3HeadersToHttpRequest(request: HttpRequest, date: ZonedDateTime) = {
    val reqWithHeaders = request.withHeaders(
      request.headers ++ Seq(`X-Amz-Date`(date), `X-Amz-Content-Sha256`(UNSIGNED_PAYLOAD)))
    reqWithHeaders
  }

  private def createAuthorizationHeader(algorithm: String,
                                        key: SigningKey, date: ZonedDateTime,
                                        canonicalRequest: CanonicalRequest): HttpHeader = {
    val sign = key.hexEncodedSignature(stringToSign(algorithm, key, date, canonicalRequest).getBytes())
    Authorization(algorithm, key.credentialString, canonicalRequest.signedHeaders, sign)
  }

  //endregion

  def stringToSign(algorithm: String,
                   signingKey: SigningKey,
                   requestDate: ZonedDateTime,
                   canonicalRequest: CanonicalRequest): String = {
    val digest = MessageDigest.getInstance("SHA-256")
    val hashedRequest = HexCoder.encodeHex(digest.digest(canonicalRequest.canonicalString.getBytes()))
    val date = requestDate.format(dateFormatter.get())
    val scope = signingKey.scope.scopeString

    s"$algorithm\n$date\n$scope\n$hashedRequest"
  }

  def Md5(data: String): String = {
    val md5 = java.security.MessageDigest.getInstance("MD5")
    val byte = data.getBytes("UTF-8")
    new sun.misc.BASE64Encoder().encode(md5.digest(byte))
  }
}

