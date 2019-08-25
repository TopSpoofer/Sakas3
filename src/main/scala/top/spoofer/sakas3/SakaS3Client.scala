package top.spoofer.sakas3

import java.io.{File, FileInputStream, InputStream}

import akka.actor.ActorSystem
import akka.http.scaladsl.model.HttpHeader
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.StreamConverters
import top.spoofer.sakas3.commons.S3Cluster
import top.spoofer.sakas3.s3v4.apis.BucketManager.Bucket
import top.spoofer.sakas3.s3v4.apis.ObjectDownloader.DownloadResult
import top.spoofer.sakas3.s3v4.apis.{BucketManager, ObjectDownloader, ObjectManager, ObjectUploader}
import top.spoofer.sakas3.s3v4.apis.ObjectUploader.UploadResult
import top.spoofer.sakas3.s3v4.commons._
import top.spoofer.sakas3.util.HttpClient

import language._
import scala.concurrent.{ExecutionContextExecutor, Future}

class SakaS3Client(s3Cluster: S3Cluster,
                   authKeys: AuthKeys,
                   blockSize: Int)
                  (implicit actorSystem: ActorSystem,
                   mate: ActorMaterializer,
                   ec: ExecutionContextExecutor) {
  /**
    * put a stream's object to s3 server
    *
    * @param bucket        the object save the object
    * @param objectName    object name in s3 service
    * @param file          file info
    * @param meta          meta data save in s3 service
    * @return
    */
  def putObject(bucket: String, objectName: String, file: SakaFile, meta: S3MetaData): Future[UploadResult] = {
    uploader.putObject(bucket, objectName, file, meta)
  }

  /**
    * put a stream's object to s3 server
    *
    * @param bucket        the object save the object
    * @param objectName    object name in s3 service
    * @param file          file info
    * @param meta          meta data save in s3 service
    * @return
    */
  def putObject(bucket: String, objectName: String, file: File, meta: S3MetaData): Future[UploadResult] = {
    uploader.putObject(bucket, objectName, file, meta)
  }

  /**
    * put a object to s3 server from FileInputStream
    *
    * can get the FileInputStream length by inputStream.getChannel.size()
    *
    * @param bucket      the bucket to save the object
    * @param objectName  the object name in storage
    * @param inputStream the object stream
    * @param meta the meta data need save
    * @param rawName raw file name
    * @return
    */
  def putObject(bucket: String,
                objectName: String,
                inputStream: FileInputStream,
                meta: S3MetaData,
                rawName: String): Future[UploadResult] = {
    val source = StreamConverters.fromInputStream(() => inputStream, blockSize)
    uploader.putObject(bucket, objectName, SakaFile(rawName, source, inputStream.getChannel.size()), meta)
  }

  /**
    * put object to s3 server
    *
    * @param bucket      the bucket to save the object
    * @param objectName  the object name in s3 server
    * @param inputStream the object stream
    * @param streamLength
    *                    the length of the stream,
    *                    note: the InputStream can't get the length when the object size more than 2G
    * @param meta the meta data need save
    * @param rawName raw file name
    * @return
    */
  def putObject(bucket: String,
                objectName: String,
                inputStream: InputStream,
                streamLength: Long,
                meta: S3MetaData,
                rawName: String): Future[UploadResult] = {
    val source = StreamConverters.fromInputStream(() => inputStream, blockSize)
    uploader.putObject(bucket, objectName, SakaFile(rawName, source, streamLength), meta)
  }

  /**
    * download an object from s3 server
    *
    * @param bucket bucket name
    * @param objectName object name in s3 server
    * @return
    */
  def getObject(bucket: String, objectName: String, extraHeaders: Seq[HttpHeader] = Nil): Future[DownloadResult] = {
    downloader.getObject(bucket, objectName, extraHeaders)
  }

  /**
    * create a bucket
    *
    * not support for qingyun s3 storage (you can view the qingyun api doc)
    *
    * if the bucket existed, will return true
    *
    * @param bucket bucket name
    * @return
    */
  def createBucket(bucket: String): Future[Boolean] = {
    bucketManager.createBucket(bucket)
  }

  /**
    * bucket exist
    *
    * @param bucket bucket name
    * @return
    */
  def bucketExist(bucket: String): Future[Boolean] = {
    bucketManager.bucketExist(bucket)
  }

  /**
    * list all of buckets
    *
    * @return
    */
  def listBuckets(): Future[Set[Bucket]] = {
    bucketManager.listBuckets()
  }

  /**
    * delete a bucket in s3 server
    *
    * if the bucket not found, will return true
    *
    * @param bucket bucket name
    * @return
    */
  def deleteBucket(bucket: String): Future[Boolean] = {
    bucketManager.deleteBucket(bucket)
  }

  /**
    * delete objects in bucket
    *
    * @param bucket bucket name
    * @param objects object list
    * @return
    */
  def deleteObjects(bucket: String, objects: Set[String]): Future[DeleteResultCollection] = {
    objectManager.deleteObjects(bucket, objects)
  }

  /**
    * delete an object in bucket
    *
    * @param bucket bucket name
    * @param objectName object name
    * @return
    */
  def deleteObject(bucket: String, objectName: String): Future[DeleteResult] = {
    objectManager.deleteObject(bucket, objectName)
  }

  /**
    * list object in bucket
    *
    * @param bucket bucket name
    * @param prefix dir?
    * @return
    */
  def listObjects(bucket: String, prefix: String = ""): Future[ListObjectsResult] = {
    objectManager.listObjects(bucket, prefix)
  }

  /**
    * list the incomplete upload objects
    *
    * @param bucket the bucket save the objects
    * @param prefix Lists in-progress uploads only for those keys that begin with the specified prefix.
    * @return
    */
  def listIncompleteObjects(bucket: String, prefix: String): Future[IncompleteObjects] = {
    objectManager.listIncompleteObjects(bucket, prefix)
  }

  /**
    * copy object in s3 server
    *
    * @param srcBucket source bucket
    * @param srcObjectName source object name
    * @param desBucket destination bucket
    * @param desObjectName destination object name
    * @return
    */
  def copyObject(srcBucket: String, srcObjectName: String, desBucket: String, desObjectName: String): Future[CopyObjectResult] = {
    objectManager.copyObject(srcBucket, srcObjectName, desBucket, desObjectName)
  }

  /**
    * obtain an object state
    *
    * @param bucket bucket name
    * @param objectName object name
    * @return
    */
  def objectState(bucket: String, objectName: String): Future[ObjectState] = {
    objectManager.objectState(bucket, objectName)
  }

  private def client: HttpClient = this.synchronized {
    var i = 0
    var ret: Option[HttpClient] = None
    while (i < clusterSize && ret.isEmpty) {
      if (s3Clients(cursor).circuitBreakerIsClosed) ret = Some(s3Clients(cursor))
      cursor = cursor + 1
      if (cursor >= clusterSize) cursor = 0
      i = i + 1
    }
    ret getOrElse s3Clients.head
  }

  @volatile
  private var cursor = 0
  private val s3Clients = s3Cluster.servers.toSeq map { server =>
    HttpClient(server.host, server.port, server.queueSize, server.useHttps)
  } toArray
  private val clusterSize = s3Cluster.servers.size
  private def s3RequestFactory = new S3RequestFactory(authKeys)
  private def uploader = ObjectUploader(client, s3RequestFactory, blockSize)
  private def downloader = ObjectDownloader(client, s3RequestFactory)
  private def bucketManager = BucketManager(client, s3RequestFactory)
  private def objectManager = ObjectManager(client, s3RequestFactory)
}

object SakaS3Client {
  val BlockSize = 1048576

  def apply(host: String,
            port: Int,
            authKeys: AuthKeys)
           (implicit actorSystem: ActorSystem,
            mate: ActorMaterializer,
            ec: ExecutionContextExecutor): SakaS3Client = {
    new SakaS3Client(S3Cluster((host, port)), authKeys, BlockSize)
  }

  def apply(s3Cluster: S3Cluster,
            authKeys: AuthKeys)
           (implicit actorSystem: ActorSystem,
            mate: ActorMaterializer,
            ec: ExecutionContextExecutor): SakaS3Client = {
    new SakaS3Client(s3Cluster, authKeys, BlockSize)
  }

  def apply(s3Cluster: S3Cluster,
            authKeys: AuthKeys,
            blockSize: Int)
           (implicit actorSystem: ActorSystem,
            mate: ActorMaterializer,
            ec: ExecutionContextExecutor): SakaS3Client = {
    new SakaS3Client(s3Cluster, authKeys, blockSize)
  }
}
