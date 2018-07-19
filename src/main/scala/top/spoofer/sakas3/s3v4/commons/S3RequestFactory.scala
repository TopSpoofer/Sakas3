package top.spoofer.sakas3.s3v4.commons

import java.time.LocalDate

import akka.http.scaladsl.model.Uri.{Path, Query}
import akka.http.scaladsl.model._
import top.spoofer.sakas3.AuthKeys
import top.spoofer.sakas3.s3v4.authorization.{AWSCredentials, CredentialScope, Signer, SigningKey}

import scala.collection.immutable

class S3RequestFactory(keys: AuthKeys) {
  def createRequest(method: HttpMethod,
                    bucket: String = "",
                    objectName: String = "",
                    queries: Iterable[(String, String)] = Nil,
                    headerList: Seq[HttpHeader] = Nil,
                    entity: Option[RequestEntity] = None): HttpRequest = {
    val uri = if (bucket == "") Uri./
    else Uri().withPath(Path.Empty / bucket / objectName).withQuery(Query(queries.toSeq: _*))

    signRequest(buildRequest(method, uri, headerList, entity))
  }

  private def buildRequest(method: HttpMethod, uri: Uri, headerList: Seq[HttpHeader], entityOpt: Option[RequestEntity]) = {
    val requestHeaders = appendS3Headers(headerList)
    entityOpt match {
      case Some(entity) =>
        HttpRequest(method, uri, headers = immutable.Seq(requestHeaders:_*), entity = entity)
      case None =>
        HttpRequest(method, uri, headers = immutable.Seq(requestHeaders:_*))
    }
  }

  private def signRequest(request: HttpRequest) = {
    val credentials = AWSCredentials(keys.accessKey, keys.secretKey)
    val sign = SigningKey(credentials, CredentialScope(LocalDate.now()))
    Signer.signedRequest(request, sign)
  }

  private def appendS3Headers(headerList: Seq[HttpHeader]) = {
    headers.Host(keys.endpoint) +: headerList
  }
}
