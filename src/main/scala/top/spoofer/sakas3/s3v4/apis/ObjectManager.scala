package top.spoofer.sakas3.s3v4.apis

import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, StatusCodes}
import akka.stream.ActorMaterializer
import akka.util.ByteString
import top.spoofer.sakas3.s3v4.authorization.Signer
import top.spoofer.sakas3.s3v4.commons._
import top.spoofer.sakas3.s3v4.errors.SakaS3Exception
import top.spoofer.sakas3.util.HttpClient
import top.spoofer.sakas3.s3v4.authorization._

import scala.concurrent.{ExecutionContextExecutor, Future}

class ObjectManager(client: HttpClient, s3RequestFactory: S3RequestFactory)
                   (implicit mater: ActorMaterializer, ec: ExecutionContextExecutor) {
  def deleteObjects(bucket: String, objects: Set[String]): Future[DeleteResultCollection] = {
    val content = buildDeleteContent(objects)
    val request = s3RequestFactory.createRequest(
      HttpMethods.POST,
      bucket = bucket,
      entity = Some(HttpEntity(ContentTypes.`application/octet-stream`, ByteString(content))),
      queries = Map("delete" -> "")
    )

    client.send(request.addHeader(RawHeader("Content-MD5", Signer.Md5(content)))) map { response =>
      if (response.status.isSuccess()) {
        val responseXml = scala.xml.XML.loadString(response.content.utf8String)
        DeleteResultCollection(bucket, responseXml)
      } else {
        throw SakaS3Exception(s"storage server response status not ok! status: ${response.status}")
      }
    }
  }

  def deleteObject(bucket: String, objectName: String): Future[DeleteResult] = {
    deleteObjects(bucket, Set(objectName)) map { response =>
      response.results.headOption match {
        case Some(result) => result
        case None => throw SakaS3Exception(s"can't find the result...")
      }
    }
  }

  def listObjects(bucket: String, prefix: String): Future[ListObjectsResult] = {
    val queries = Map("list-type" -> "2", "prefix" -> prefix)
    client.send(s3RequestFactory.createRequest(HttpMethods.GET, bucket, queries = queries)) map { response =>
      if (response.status.isSuccess()) {
        val responseXml = scala.xml.XML.loadString(response.content.utf8String)
        ListObjectsResult(responseXml)
      } else {
        throw SakaS3Exception(s"list bucket's object fail. s3 server status not ok ${response.status}," +
          s" msg: ${response.content.utf8String}")
      }
    }
  }

  def listIncompleteObjects(bucket: String, prefix: String): Future[IncompleteObjects] = {
    val request = s3RequestFactory.createRequest(HttpMethods.GET, bucket, queries = Map("uploads" -> "", "prefix" -> prefix))
    client.send(request) map { response =>
      if (response.status.isSuccess()) {
        val responseXml = scala.xml.XML.loadString(response.content.utf8String)
        IncompleteObjects(responseXml)
      } else {
        throw SakaS3Exception(s"list bucket's incomplete object fail. s3 server status not ok ${response.status}," +
          s" msg: ${response.content.utf8String}")
      }
    }
  }

  def copyObject(srcBucket: String, srcObjectName: String, desBucket: String, desObjectName: String): Future[CopyObjectResult] = {
    val request = s3RequestFactory.createRequest(
      HttpMethods.PUT,
      desBucket,
      desObjectName,
      headerList = List(`X-Amz-Copy-Source`(srcBucket, srcObjectName))
    )

    client.send(request) map { response =>
      if (response.status.isSuccess()) {
        CopyObjectResult(desBucket, desObjectName)
      } else {
        throw SakaS3Exception(s"copy fail, ${response.content.utf8String}")
      }
    }
  }

  def objectState(bucket: String, objectName: String): Future[ObjectState] = {
    val request = s3RequestFactory.createRequest(HttpMethods.HEAD, bucket, objectName)
    client.send(request) map { response =>
      response.status match {
        case status if status.isSuccess() =>
          ObjectState(bucket, objectName, objectExisted = true, S3MetaData(response.headers))
        case StatusCodes.NotFound =>
          ObjectState(bucket, objectName, objectExisted = false, S3MetaData(response.headers))
        case _ =>
          throw SakaS3Exception(s"obtain object state fail, " +
            s"s3 server status ont ok ${response.status}, ${response.content.utf8String}")
      }
    }
  }

  private[this] def buildDeleteContent(objects: Set[String]): String = {
    // @formatter:off
    <Delete>
      <Quiet>false</Quiet>{for (elem <- objects) yield <Object>
      <Key>{elem}</Key>
    </Object>}
    </Delete>.toString()
    // @formatter:on
  }
}

object ObjectManager {
  def apply(client: HttpClient, s3RequestFactory: S3RequestFactory)
           (implicit mater: ActorMaterializer, ec: ExecutionContextExecutor): ObjectManager = {
    new ObjectManager(client, s3RequestFactory)
  }
}