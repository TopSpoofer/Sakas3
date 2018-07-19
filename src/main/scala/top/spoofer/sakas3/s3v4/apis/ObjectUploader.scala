package top.spoofer.sakas3.s3v4.apis

import java.io.{File, FileInputStream}
import java.nio.file.Paths

import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{FileIO, StreamConverters}
import top.spoofer.sakas3.s3v4.apis.ObjectUploader.UploadResult
import top.spoofer.sakas3.s3v4.commons.{S3MetaData, S3RequestFactory, SakaFile}
import top.spoofer.sakas3.s3v4.errors.SakaS3Exception
import top.spoofer.sakas3.util.HttpClient

import scala.concurrent.{ExecutionContextExecutor, Future}

class ObjectUploader(client: HttpClient, requestFactory: S3RequestFactory, blockSize: Int = 1048576)
                    (implicit mater: ActorMaterializer, ec: ExecutionContextExecutor) {
  def putObject(bucket: String, objectName: String, file: SakaFile, meta: S3MetaData): Future[UploadResult] = {
    val request = buildRequest(bucket, objectName, file, meta)
    for {
      response <- client.send(request)
    } yield {
      if (response.status.isSuccess()) UploadResult(bucket, objectName, file.size)
      else {
        val log = s"upload object $objectName to $bucket fail, http code: ${response.status}, msg: ${response.content.utf8String}"
        throw SakaS3Exception(log)
      }
    }
  }

  def putObject(bucket: String, objectName: String, file: File, meta: S3MetaData): Future[UploadResult] = {
    if (file.exists() && file.isFile) {
      val source = FileIO.fromPath(Paths.get(file.getAbsolutePath), blockSize)
      putObject(bucket, objectName, SakaFile(file.getName, source, file.length()), meta)
    } else Future.failed(SakaS3Exception(s"file can't find: ${file.getAbsolutePath}"))
  }

  private def buildRequest(bucket: String, objectName: String, file: SakaFile, meta: S3MetaData) = {
    val entity = HttpEntity(ContentTypes.`application/octet-stream`, file.size, file.content)
    requestFactory.createRequest(
      HttpMethods.PUT,
      bucket,
      objectName,
      headerList = meta.withFileNameAndSize(file.name, file.size).toHeaders.toSeq, //put the raw file's name and size
      entity = Some(entity)
    )
  }
}

object ObjectUploader {
  case class UploadResult(bucket: String, objectName: String, size: Long)

  def apply(client: HttpClient, requestFactory: S3RequestFactory, blockSize: Int = 1048576)
           (implicit mater: ActorMaterializer, ec: ExecutionContextExecutor): ObjectUploader = {
    new ObjectUploader(client, requestFactory, blockSize)
  }
}
