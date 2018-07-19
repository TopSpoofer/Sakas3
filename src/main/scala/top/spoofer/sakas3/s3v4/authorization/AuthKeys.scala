package top.spoofer.sakas3.s3v4.authorization

case class AuthKeys(accessKey: String, secretKey: String, endpoint: String = "s3.amazonaws.com")