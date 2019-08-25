package top.spoofer.sakas3.s3v4.apis

import akka.http.scaladsl.model.{HttpHeader, HttpMethods, HttpResponse, StatusCode}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import akka.util.ByteString
import top.spoofer.sakas3.s3v4.apis.ObjectDownloader.{DownloadResult, ErrorResult, SuccessResult}
import top.spoofer.sakas3.s3v4.commons.{S3MetaData, S3RequestFactory}
import top.spoofer.sakas3.util.HttpClient

import scala.concurrent.{ExecutionContextExecutor, Future}

class ObjectDownloader(client: HttpClient, requestFactory: S3RequestFactory)
                      (implicit mate: ActorMaterializer, ec: ExecutionContextExecutor) {
  /**
    * download an object from s3 server
    *
    * when http status not ok, will return Future[SakaS3Exception]
    *
    * @param bucket bucket name
    * @param objectName object name in s3 server
    * @return
    */
  def getObject(bucket: String, objectName: String, extraHeaders: Seq[HttpHeader] = Nil): Future[DownloadResult] = {
    for {
      response <- client.sendRaw(buildRequest(bucket, objectName, extraHeaders))
      errorEntity <- errorEntityDataBytes(response)
    } yield {
      if (response.status.isSuccess()) {
        val result = SuccessResult(response.entity.dataBytes, S3MetaData(response.headers), response.headers)
        DownloadResult(bucket, objectName, Right(result))
      } else {
        val log = s"can't get $bucket/$objectName , s3 server http status not ok , code is: ${response.status} , msg is: ${errorEntity.utf8String}"
        val errorResult = ErrorResult(response.status, log)
        DownloadResult(bucket, objectName, Left(errorResult))
      }
    }
  }

  private def errorEntityDataBytes(response: HttpResponse): Future[ByteString] = {
    if (!response.status.isSuccess()) {
      response.entity.dataBytes.runFold(ByteString.empty)(_ ++ _)
    } else Future.successful(ByteString.empty)
  }

  @inline
  private def buildRequest(bucket: String, objectName: String, extraHeaders: Seq[HttpHeader]) = {
    requestFactory.createRequest(HttpMethods.GET, bucket, objectName, queries = Nil, headerList = extraHeaders)
  }
}

object ObjectDownloader {
  case class ErrorResult(httpStatus: StatusCode, error: String)

  case class SuccessResult(content: Source[ByteString, Any], meta: S3MetaData, headers: Seq[HttpHeader])

  case class DownloadResult(bucket: String,
                            objectName: String,
                            result: Either[ErrorResult, SuccessResult])

  def apply(client: HttpClient, requestFactory: S3RequestFactory)
           (implicit mate: ActorMaterializer, ec: ExecutionContextExecutor): ObjectDownloader = {
    new ObjectDownloader(client, requestFactory)
  }
}
