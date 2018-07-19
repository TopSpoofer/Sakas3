package top.spoofer

package object sakas3 {
  type AuthKeys = s3v4.authorization.AuthKeys
  val AuthKeys: s3v4.authorization.AuthKeys.type = s3v4.authorization.AuthKeys
}
