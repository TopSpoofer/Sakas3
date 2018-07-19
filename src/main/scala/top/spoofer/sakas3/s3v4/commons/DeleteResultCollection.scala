package top.spoofer.sakas3.s3v4.commons

import top.spoofer.sakas3.s3v4.commons.DeleteStatus.DeleteStatus

import scala.xml.Elem

object DeleteStatus extends Enumeration {
  type DeleteStatus = Value
  val Success: DeleteStatus.Value = Value(1)
  val Fail: DeleteStatus.Value = Value(0)
}

case class DeleteResult(status: DeleteStatus, name: String) {
  def isSuccess: Boolean = status == DeleteStatus.Success
}

case class DeleteResultCollection(bucket: String, results: Seq[DeleteResult]) {
  def fails: Seq[DeleteResult] = results.filter(!_.isSuccess)
}

object DeleteResultCollection {
  /**
    * get delete success result
    *
    * @param responseXml delete result
    * @return
    */
  private def parsingSuccessDeleteResult(responseXml: Elem) = {
    val elems = responseXml \\ "Deleted" \\ "Key"
    elems.map {
      // @formatter:off
      case <Key>{objectName}</Key> =>
        // @formatter:on
        DeleteResult(DeleteStatus.Success, objectName.toString())
    }
  }

  /**
    * get delete fail result
    *
    * @param responseXml delete result
    * @return
    */
  private def parsingFailDeleteResult(responseXml: Elem) = {
    val failElem = responseXml \\ "Error" \\ "Key"
    val deleteFailObject = failElem.map {
      // @formatter:off
      case <Key>{objectName}</Key> =>
        // @formatter:on
        DeleteResult(DeleteStatus.Fail, objectName.toString())
    }
    deleteFailObject
  }

  def apply(bucket: String, responseXml: Elem): DeleteResultCollection = {
    val deletedObject = parsingSuccessDeleteResult(responseXml)
    val deleteFailObject = parsingFailDeleteResult(responseXml)
    DeleteResultCollection(bucket, deletedObject ++ deleteFailObject)
  }
}
