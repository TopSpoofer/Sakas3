package top.spoofer.sakas3.s3v4.commons

case class ObjectState(bucket: String, name: String, objectExisted: Boolean, metaData: S3MetaData)
