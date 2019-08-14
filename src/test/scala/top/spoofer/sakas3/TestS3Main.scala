package top.spoofer.sakas3

import akka.actor.ActorSystem
import akka.stream.scaladsl.Source
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import akka.util.ByteString
import top.spoofer.sakas3.s3v4.commons.{S3MetaData, SakaFile}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContextExecutor}
import java.io.{File, InputStream}

import top.spoofer.sakas3.commons.S3Cluster

object TestS3Main {
  implicit val system: ActorSystem = ActorSystem("SakaClientTest")
  implicit val dispatcher: ExecutionContextExecutor = system.dispatcher
  implicit val mater: ActorMaterializer = ActorMaterializer(ActorMaterializerSettings(system))

  val bucket = "test"
  val data = "1234"
  val fileName = "./README.md"
  val sakaFile = SakaFile("rawname_ppp", Source.single(ByteString(data)), data.length)
  val newBucket = "test2"
  //if qingyun stroage use AuthKeys("xxx", "xx", endpoint = s3.youdomain.com)
  val client = SakaS3Client(
    S3Cluster(("localhost", 9001), ("localhost", 9002), ("localhost", 9003)),
    AuthKeys("spoofer", "12345678*")
  )

  def main(args: Array[String]): Unit = {
    Range(0, 5) foreach { _ =>
      listObjects()
    }
    system.terminate()
  }

  private def filePut(): Unit = {
    val resultFuture = client.putObject(bucket, "filePut", new File(fileName), S3MetaData.DefaultS3MetaData)
    val result = Await.result(resultFuture, 8.seconds)
    println(result)
  }

  private def streamPut(): Unit = {
    val resultFuture = client.putObject(bucket, "streamPut", sakaFile, S3MetaData.DefaultS3MetaData)
    val result = Await.result(resultFuture, 8.seconds)
    println(result)
  }

  private def fileInputStreamPut(): Unit = {
    import java.io.FileInputStream
    val fis = new FileInputStream(new File(fileName))
    val resultFuture = client.putObject(bucket, "fileInputStreamPut", fis, S3MetaData.DefaultS3MetaData, "README")
    val result = Await.result(resultFuture, 8.seconds)
    println(result)
  }

  private def inputStreamPut(): Unit = {
    import java.io.FileInputStream
    val file = new File(fileName)
    val fis: InputStream = new FileInputStream(file)
    val resultFuture = client.putObject(bucket, "inputStreamPut", fis, file.length(), S3MetaData.DefaultS3MetaData, "README")
    val result = Await.result(resultFuture, 8.seconds)
    println(result)
  }

  private def downloadSuccess(): Unit = {
    val resultFuture = client.getObject(bucket, "filePut")
    val result = Await.result(resultFuture, 8.seconds)
    val entityFuture = result.content.runFold(ByteString.empty)(_ ++ _)
    val entity = Await.result(entityFuture, 8.seconds)
    println(result)
    println(entity.utf8String)
  }

  private def downloadFail(): Unit = {
    val resultFuture = client.getObject(bucket, "filePut1")
    val result = Await.result(resultFuture, 8.seconds)
    println(result)
  }

  private def createBucket(): Unit = {
    val retFuture = client.createBucket(newBucket)
    val result = Await.result(retFuture, 8.seconds)
    println(result)
  }

  private def bucketExist(): Unit = {
    val retFuture = client.bucketExist(newBucket)
    val result = Await.result(retFuture, 8.seconds)
    println(result)
  }

  private def listBuckets(): Unit = {
    val retFuture = client.listBuckets()
    val result = Await.result(retFuture, 8.seconds)
    println(result)
  }

  private def deleteBucket(): Unit = {
    val retFuture = client.deleteBucket(newBucket)
    val result = Await.result(retFuture, 8.seconds)
    println(result)
  }

  private def deleteObjects() = {
    val retFuture = client.deleteObjects(bucket, Set("1.txt", "2.txt", "3.txt"))
    val result = Await.result(retFuture, 8.seconds)
    println(result)
  }

  private def deleteObject() = {
    val retFuture = client.deleteObject(bucket, "filePut")
    val result = Await.result(retFuture, 8.seconds)
    println(result)
  }

  private def listObjects() = {
    val retFuture = client.listObjects(bucket)
    val result = Await.result(retFuture, 8.seconds)
    println(result)
  }

  private def listIncompleteObjects() = {
    val retFuture = client.listIncompleteObjects(bucket, "")
    val result = Await.result(retFuture, 8.seconds)
    println(result)
  }

  private def copyObject() = {
    val retFuture = client.copyObject(bucket, "fileInputStreamPut", newBucket, "new_object")
    val result = Await.result(retFuture, 8.seconds)
    println(result)
  }

  private def objectState() = {
    val retFuture = client.objectState(bucket, "fileInputStreamPut1")
    val result = Await.result(retFuture, 8.seconds)
    println(result)
  }
}
