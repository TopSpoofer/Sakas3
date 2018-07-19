package top.spoofer.sakas3.s3v4.commons

import akka.util.ByteString
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import top.spoofer.sakas3.TestBasis

@RunWith(classOf[JUnitRunner])
class HexCoderTest extends TestBasis {
  "HexCoderTest" should "return" in {
    val rawStr = ByteString("123a")
    val ret = "31323361"
    HexCoder.encodeHex(rawStr) should be(ret)
    HexCoder.encodeHex(rawStr.toArray) should be(ret)
  }
}
