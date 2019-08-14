package top.spoofer.sakas3.commons

import language._

case class S3Server(host: String, port: Int, useHttps: Boolean = false, queueSize: Int = 1024)

case class S3Cluster(servers: Set[S3Server])

object S3Cluster {
  def apply(servers: (String, Int)*): S3Cluster = {
    new S3Cluster(servers map { s => S3Server(s._1, s._2) } toSet)
  }
}
