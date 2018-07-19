package top.spoofer.sakas3.s3v4.commons

import akka.http.scaladsl.model.HttpHeader
import akka.http.scaladsl.model.headers.RawHeader

import language._

case class S3MetaData(metaData: Map[String, String]) {
  def withMetaData(kvs: (String, String)*): S3MetaData = {
    S3MetaData(metaData ++ kvs.toMap)
  }

  def toHeaders: Set[RawHeader] = {
    metaData map { elem => RawHeader(s"x-amz-meta-${elem._1}", elem._2) } toSet
  }

  private[s3v4] def withFileName(name: String): S3MetaData = {
    S3MetaData(metaData + (S3MetaData.fileNameKey -> Base64Coder.encoded(name)))
  }

  private[s3v4] def withFileSize(size: Long): S3MetaData = {
    S3MetaData(metaData + (S3MetaData.sizeKey -> size.toString))
  }

  private[s3v4] def withFileNameAndSize(name: String, size: Long): S3MetaData = {
    S3MetaData(metaData + (S3MetaData.fileNameKey -> Base64Coder.encoded(name)) + (S3MetaData.sizeKey -> size.toString))
  }

  def fileRawName: Option[String] = {
    metaData.get(S3MetaData.fileNameKey) map {
      name => Base64Coder.decoded(name).getOrElse(name)
    }
  }

  def fileSize: Option[Long] = {
    metaData.get(S3MetaData.sizeKey) map { _.toLong }
  }

  def value(key: String): Option[String] = metaData.get(key)
}

object S3MetaData {
  private[s3v4] val fileNameKey: String = "raw-file-name-s3"
  private[s3v4] val sizeKey: String = "raw-file-size-s3"

  def apply(headers: Seq[HttpHeader]): S3MetaData = {
    val rex = raw".*-amz-meta-(.*)".r
    val hs = headers.map { header =>
      header.lowercaseName() match {
        case rex(name) => Some(name, header.value())
        case _ => None
      }
    }.filter(_.isDefined).map(_.get)
    new S3MetaData(Map(hs: _*))
  }

  val DefaultS3MetaData = new S3MetaData(Map.empty)
}

