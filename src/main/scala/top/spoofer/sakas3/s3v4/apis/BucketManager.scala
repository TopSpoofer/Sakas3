package top.spoofer.sakas3.s3v4.apis

import akka.http.scaladsl.model.{HttpMethods, StatusCodes}
import akka.stream.ActorMaterializer
import top.spoofer.sakas3.s3v4.apis.BucketManager.Bucket
import top.spoofer.sakas3.s3v4.commons.S3RequestFactory
import top.spoofer.sakas3.s3v4.errors.SakaS3Exception
import top.spoofer.sakas3.util.HttpClient

import scala.concurrent.{ExecutionContextExecutor, Future}

class BucketManager(client: HttpClient, s3RequestFactory: S3RequestFactory)
                   (implicit mater: ActorMaterializer, ec: ExecutionContextExecutor) {
  /**
    * create a bucket
    *
    * not support for qingyun s3 storage (you can view the qingyun api doc)
    *
    * @param bucket bucket name
    * @return
    */
  def createBucket(bucket: String): Future[Boolean] = {
    val request = s3RequestFactory.createRequest(HttpMethods.PUT, bucket)
    for {
      response <- client.send(request)
    } yield {
      if (response.status.isSuccess() || response.status == StatusCodes.Conflict) true //if bucket existed, will be true
      else {
        val log = s"create bucket: $bucket fail, s3 server return: ${response.status}, msg: ${response.content.utf8String}"
        throw SakaS3Exception(log)
      }
    }
  }

  def bucketExist(bucket: String): Future[Boolean] = {
    val request = s3RequestFactory.createRequest(HttpMethods.HEAD, bucket)
    for {
      response <- client.send(request)
    } yield {
      response.status match {
        case StatusCodes.OK => true
        case StatusCodes.NotFound => false
        case _ =>
          val log = s"s3 server http status not ok in bucketExist, bucket is: $bucket, status is: ${response.status}, msg is: ${response.content.utf8String}"
          throw SakaS3Exception(log)
      }
    }
  }

  def listBuckets(): Future[Set[Bucket]] = {
    val request = s3RequestFactory.createRequest(HttpMethods.GET)
    for {
      response <- client.send(request)
    } yield {
      if (response.status.isSuccess()) parsingListResponse(response.content.utf8String)
      else {
        val log = s"list buckets fail, s3 server return ${response.status}, msg is ${response.content.utf8String}"
        throw SakaS3Exception(log)
      }
    }
  }

  def deleteBucket(bucket: String): Future[Boolean] = {
    val request = s3RequestFactory.createRequest(HttpMethods.DELETE, bucket)
    for {
      response <- client.send(request)
    } yield {
      if (response.status.isSuccess() || response.status == StatusCodes.NotFound) true //if bucket not exist, will true
      else {
        val log = s"delete bucket $bucket fail, s3 server return ${response.status}, msg is ${response.content.utf8String}"
        throw SakaS3Exception(log)
      }
    }
  }

  /**
    * parsing xml response
    *
    * @param response the server response, is a xml
    * @return
    */
  private def parsingListResponse(response: String): Set[Bucket] = {
    val responseXml = scala.xml.XML.loadString(response)
    val buckets = responseXml \ "Buckets" \ "Bucket"
    buckets.map { elem =>
      val name = elem \\ "Name"
      val date = elem \\ "CreationDate"
      Bucket(name.text, date.text)
    } .toSet
  }
}

object BucketManager {
  case class Bucket(name: String, created: String)

  def apply(client: HttpClient, s3RequestFactory: S3RequestFactory)
           (implicit mater: ActorMaterializer, ec: ExecutionContextExecutor): BucketManager = {
    new BucketManager(client, s3RequestFactory)
  }
}
