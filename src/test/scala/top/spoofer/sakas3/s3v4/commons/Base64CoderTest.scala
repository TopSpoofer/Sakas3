package top.spoofer.sakas3.s3v4.commons

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import top.spoofer.sakas3.TestBasis

@RunWith(classOf[JUnitRunner])
class Base64CoderTest extends TestBasis {
  "Base64CoderTest" should "return" in {
    val rawstr = "1234"
    val str64 = "MTIzNA=="
    Base64Coder.encoded(rawstr) should be(str64)
    Base64Coder.decoded(str64).get should be(rawstr)
    Base64Coder.decoded("12354").isFailure should be(true)
  }
}
