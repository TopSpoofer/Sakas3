package top.spoofer.sakas3.routes

import java.util.UUID

import akka.http.scaladsl.model.{EntityStreamException, StatusCodes}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import spray.json.RootJsonFormat
import top.spoofer.sakas3.SakaS3Client
import top.spoofer.sakas3.commons.ExtendHttpHeaders._
import top.spoofer.sakas3.routes.ResourceUploadTrait.{ResourceUploadedResult, UploadCompletedEvent}
import top.spoofer.sakas3.s3v4.apis.ObjectUploader.UploadResult
import top.spoofer.sakas3.s3v4.commons.{S3MetaData, SakaFile}

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}
import ResourceUploadedResult._
import akka.event.LoggingAdapter
import akka.http.scaladsl.server.directives.FileInfo
import top.spoofer.sakas3.util.JsonSupport._

trait ResourceUploadTrait {
  protected val s3Client: SakaS3Client
  protected val bucket: String

  protected def uploadRoute: Route = extractLog { implicit log =>
    (headerValueByType(classOf[`X-File-Size`]) & optionalHeaderValueByName("ObjectName")) { (fileSize, objectNameOpt) =>
      extractExecutionContext { implicit ec =>
        fileUpload("file") { case (fileInfo, sourceData) =>
          val objectName = objectNameOpt.fold(UUID.randomUUID().toString) { name => name }
          val file = SakaFile(fileInfo.fileName, sourceData, fileSize.size)
          uploadObject(objectName, fileInfo, file)
        }
      }
    }
  }

  protected def uploadObject(objectName: String, fileInfo: FileInfo, file: SakaFile)
                            (implicit log: LoggingAdapter, ec: ExecutionContextExecutor): Route = {
    onComplete(uploadResource(objectName, file, S3MetaData.empty)) {
      case Success(result) =>
        complete(ResourceUploadedResult(result.objectName, fileInfo.fileName, result.size, fileInfo.contentType.value))
      case Failure(ex) =>
        ex match {
          case _: EntityStreamException =>
            complete(StatusCodes.BadRequest) // 用户中断了上传
          case other =>
            log.error(other, "error to store file in s3 store")
            complete(StatusCodes.InternalServerError)
        }
    }
  }

  protected def publishUploadCompletedEvent(event: UploadCompletedEvent): Unit = {
    Unit
  }

  private def uploadResource(objectName: String, file: SakaFile, meta: S3MetaData)
                            (implicit ec: ExecutionContextExecutor): Future[UploadResult] = {
    s3Client.putObject(bucket, objectName, file, meta) map { result =>
      publishUploadCompletedEvent(UploadCompletedEvent(bucket, objectName, file.name, file.size))
      result
    }
  }
}

object ResourceUploadTrait {
  case class UploadCompletedEvent(bucket: String, objectName: String, rawName: String, size: Long)
  case class ResourceUploadedResult(fileId: String, rawName: String, size: Long, contentType: String)
  object ResourceUploadedResult {
    implicit val ResourceUploadedResultFormat: RootJsonFormat[ResourceUploadedResult] = {
      jsonFormat4(ResourceUploadedResult.apply)
    }
  }
}
