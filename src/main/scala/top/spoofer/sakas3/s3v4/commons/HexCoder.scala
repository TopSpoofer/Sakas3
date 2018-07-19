package top.spoofer.sakas3.s3v4.commons

import akka.util.ByteString
import javax.xml.bind.DatatypeConverter

object HexCoder {
  def encodeHex(bytes: Array[Byte]): String = DatatypeConverter.printHexBinary(bytes).toLowerCase

  def encodeHex(bytes: ByteString): String = encodeHex(bytes.toArray)
}
