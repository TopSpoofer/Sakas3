package top.spoofer.sakas3.s3v4.commons

import akka.http.scaladsl.model.headers.RawHeader
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import top.spoofer.sakas3.TestBasis

@RunWith(classOf[JUnitRunner])
class S3MetaDataTest extends TestBasis {
  "S3MetaDataTest" should "return" in {
    val rawData = Map("name" -> "lele")
    val addData = ("age", "12")
    val fileName = "object.avi"
    val size = 123
    val headers = Set(RawHeader("x-amz-meta-name", "lele"),
      RawHeader("x-amz-meta-raw-file-name-s3", "b2JqZWN0LmF2aQ=="))
    val meta = S3MetaData(rawData)
    meta.withMetaData(addData).metaData should be(rawData + addData)
    meta.withFileName(fileName).fileRawName.get should be(fileName)
    meta.withFileSize(size).fileSize.get should be(size)
    meta.value("hh") should be(None)
    meta.value("name").get should be("lele")
    meta.withFileName(fileName).toHeaders should be(headers)
  }
}
