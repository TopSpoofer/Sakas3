package top.spoofer.sakas3.routes

import java.net.URLEncoder
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{HttpEntity, HttpHeader, HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.ContentTypeResolver
import top.spoofer.sakas3.SakaS3Client
import top.spoofer.sakas3.s3v4.apis.ObjectDownloader.SuccessResult

import scala.collection.immutable
import scala.util.{Failure, Success, Try}

trait ResourceDownloadTrait {
  private val resolver: ContentTypeResolver = ContentTypeResolver.Default
  protected val s3Client: SakaS3Client
  protected val bucket: String

  protected def downloadRoute(header: Seq[HttpHeader] = Nil): Route = extractLog { implicit log =>
    path(Segment) { objectName =>
      onComplete(downloadObject(objectName, header)) {
        case Success(result) if result.result.isRight =>
          val (rawName, entity, responseHeaders) = buildResult(objectName, result.result.right.get)
          val encodedName = encodedRawName(rawName, objectName)
          val headers = RawHeader(
            "Content-Disposition",
            s"""attachment; filename=$encodedName; filename*=utf-8''$encodedName""") +: responseHeaders
          complete(HttpResponse(StatusCodes.OK, immutable.Seq(headers: _*), entity))

        case Success(errorResult) if errorResult.result.isLeft =>
          val status = errorResult.result.left.get.httpStatus
          complete(status, errorResult.result.left.get.error)

        case Failure(ex) =>
          log.error(ex, s"download object $objectName error")
          complete(StatusCodes.InternalServerError, "server error")
      }
    }
  }

  private def buildResult(objectName: String, successResult: SuccessResult) = {
    val rawName = parsingRawName(successResult, objectName)
    (rawName, buildResponseHttpEntity(successResult, rawName), successResult.headers)
  }

  private def  buildResponseHttpEntity(successResult: SuccessResult, rawName: String) = {
    successResult.meta.fileSize match {
      case Some(size) =>
        HttpEntity(contentType = resolver(rawName), contentLength = size, data = successResult.content)
      case None =>
        HttpEntity(contentType = resolver(rawName), data = successResult.content)
    }
  }

  private def parsingRawName(successResult: SuccessResult, objectName: String) = {
    successResult.meta.fileRawName.fold(objectName) { x => x }
  }

  private def encodedRawName(rawName: String, objectName: String): String = {
    Try { URLEncoder.encode(rawName, "UTF-8") } getOrElse objectName
  }

  private def downloadObject(objectName: String, header: Seq[HttpHeader]) = {
    s3Client.getObject(bucket, objectName, header)
  }
}
