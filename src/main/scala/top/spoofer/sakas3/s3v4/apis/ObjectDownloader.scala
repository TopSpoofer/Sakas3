package top.spoofer.sakas3.s3v4.apis

import akka.http.scaladsl.model.{HttpMethods, HttpResponse}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import akka.util.ByteString
import top.spoofer.sakas3.s3v4.apis.ObjectDownloader.DownloadResult
import top.spoofer.sakas3.s3v4.commons.{S3MetaData, S3RequestFactory}
import top.spoofer.sakas3.s3v4.errors.SakaS3Exception
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
  def getObject(bucket: String, objectName: String): Future[DownloadResult] = {
    for {
      response <- client.sendRaw(buildRequest(bucket, objectName))
      errorEntity <- errorEntityDataBytes(response)
    } yield {
      if (response.status.isSuccess()) {
        DownloadResult(bucket, objectName, response.entity.dataBytes, S3MetaData(response.headers))
      } else {
        val log = s"can't get $bucket/$objectName , s3 server http status not ok , code is: ${response.status} , msg is: ${errorEntity.utf8String}"
        throw SakaS3Exception(log)
      }
    }
  }

  private def errorEntityDataBytes(response: HttpResponse): Future[ByteString] = {
    if (!response.status.isSuccess()) {
      response.entity.dataBytes.runFold(ByteString.empty)(_ ++ _)
    } else Future.successful(ByteString.empty)
  }

  @inline
  private def buildRequest(bucket: String, objectName: String) = {
    requestFactory.createRequest(HttpMethods.GET, bucket, objectName)
  }
}

object ObjectDownloader {
  case class DownloadResult(bucket: String, objectName: String, content: Source[ByteString, Any], meta: S3MetaData)

  def apply(client: HttpClient, requestFactory: S3RequestFactory)
           (implicit mate: ActorMaterializer, ec: ExecutionContextExecutor): ObjectDownloader = {
    new ObjectDownloader(client, requestFactory)
  }
}
