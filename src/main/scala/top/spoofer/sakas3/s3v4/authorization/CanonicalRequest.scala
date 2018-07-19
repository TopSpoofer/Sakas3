package top.spoofer.sakas3.s3v4.authorization

import akka.http.scaladsl.model.Uri.{Path, Query}
import akka.http.scaladsl.model.{HttpHeader, HttpRequest}

case class CanonicalRequest(method: String, uri: String,
                            queryString: String,
                            headerString: String,
                            signedHeaders: String,
                            hashedPayload: String) {
  def canonicalString: String = {
    s"$method\n$uri\n$queryString\n$headerString\n\n$signedHeaders\n$hashedPayload"
  }
}

object CanonicalRequest {
  private[this] val content_sha256 = "x-amz-content-sha256"
  private[this] val specialCharMap = Map(
    "!" -> "%21",
    "#" -> "%23",
    "$" -> "%24",
    "&" -> "%26",
    "'" -> "%27",
    "(" -> "%28",
    ")" -> "%29",
    "*" -> "%2A",
    "+" -> "%28",
    "," -> "%2C",
    ":" -> "%3A",
    ";" -> "%3B",
    "=" -> "%3D",
    "?" -> "%3F",
    "@" -> "%40",
    "[" -> "%5B",
    "]" -> "%5D",
    "{" -> "%7B",
    "}" -> "%7D",
    "|" -> "%7C")

  def from(req: HttpRequest): CanonicalRequest = {
    val hashedBody = req.headers.find(_.name == content_sha256).map(_.value).getOrElse("")
    CanonicalRequest(
      req.method.value,
      preprocessPath(req.uri.path),
      canonicalQueryString(req.uri.query()),
      canonicalHeaderString(req.headers),
      signedHeadersString(req.headers),
      hashedBody
    )
  }

  def canonicalQueryString(query: Query): String = {
    query.sortBy(_._1).map { case (a, b) => s"${uriEncode(a)}=${uriEncode(b)}" }.mkString("&")
  }

  def uriEncode(str: String, encodeSlash: Boolean = false): String = {
    val result = new StringBuilder
    str.foreach { ch =>
      if ((ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z') || (ch >= '0' && ch <= '9') || ch == '_' || ch == '-' || ch == '~' || ch == '.') {
        result.append(ch)
      } else if (ch == '/') {
        result.append(if (encodeSlash) "%2F" else ch)
      } else {
        result.append("%" + Integer.toHexString(ch.toInt).toUpperCase)
      }
    }
    result.toString()
  }

  def preprocessPath(path: Path): String = {
    specialCharMap.foldLeft(path.toString()) { (accum, keyValue) =>
      accum.replace(keyValue._1, keyValue._2)
    }
  }

  def canonicalHeaderString(headers: Seq[HttpHeader]): String = {
    val grouped = headers.groupBy(_.lowercaseName())
    val combined = grouped.mapValues(_.map(_.value.replaceAll("\\s+", " ").trim).mkString(","))
    combined.toList.sortBy(_._1).map { case (k, v) =>
      s"$k:$v"
    }.mkString("\n")
  }

  def signedHeadersString(headers: Seq[HttpHeader]): String = {
    headers.map(_.lowercaseName()).distinct.sorted.mkString(";")
  }
}
