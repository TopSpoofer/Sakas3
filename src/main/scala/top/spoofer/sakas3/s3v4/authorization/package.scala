package top.spoofer.sakas3.s3v4

import java.security.MessageDigest
import akka.stream.scaladsl.{Flow, Keep, Sink}
import akka.util.ByteString

import scala.concurrent.Future

package object authorization {
  def digest(algorithm: String = "SHA-256"): Sink[ByteString, Future[ByteString]] = {
    Flow[ByteString].fold(MessageDigest.getInstance(algorithm)) {
        case (digest, bytes) =>
          digest.update(bytes.asByteBuffer)
          digest
      }
      .map(d => ByteString(d.digest()))
      .toMat(Sink.head[ByteString])(Keep.right)
  }
}
