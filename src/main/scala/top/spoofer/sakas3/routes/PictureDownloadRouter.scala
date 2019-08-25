package top.spoofer.sakas3.routes

import akka.http.scaladsl.model.HttpHeader
import top.spoofer.sakas3.SakaS3Client
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.headers._

class PictureDownloadRouter(override val s3Client: SakaS3Client,
                            override val bucket: String) extends ResourceDownloadTrait {
  def downloadPicture: Route = (get & optionalHeaderValueByType(classOf[Range])) { range =>
    downloadRoute(range.fold(Seq.empty[HttpHeader]) { h => Seq(h) })
  }
}
