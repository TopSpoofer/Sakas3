package top.spoofer.sakas3.routes

import akka.event.LoggingAdapter
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.{ContentTypeResolver, FileInfo}
import top.spoofer.sakas3.SakaS3Client
import top.spoofer.sakas3.s3v4.commons.SakaFile

import scala.concurrent.ExecutionContextExecutor

class PictureUploadRouter(override val s3Client: SakaS3Client,
                          override val bucket: String) extends ResourceUploadTrait {
  private val resolver: ContentTypeResolver = ContentTypeResolver.Default

  override protected def uploadObject(objectName: String, fileInfo: FileInfo, file: SakaFile)
                                     (implicit log: LoggingAdapter, ec: ExecutionContextExecutor): Route = {
    if (contentTypeCheck(fileInfo.fileName, resolver)) {
      complete(StatusCodes.UnsupportedMediaType)
    } else super.uploadObject(objectName, fileInfo, file)
  }

  private def contentTypeCheck(rawName: String, resolver: ContentTypeResolver): Boolean = {
    resolver.resolve(rawName).mediaType.isImage
  }
}
