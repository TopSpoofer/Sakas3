package top.spoofer.sakas3.util

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpHeader, HttpRequest, HttpResponse, StatusCode}
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.stream.{ActorMaterializer, OverflowStrategy, QueueOfferResult}
import akka.util.ByteString
import top.spoofer.sakas3.util.HttpClient.HttpClientResponse

import scala.concurrent.{ExecutionContextExecutor, Future, Promise}
import scala.util.{Failure, Success}

class HttpClient(host: String,
                 port: Int,
                 queueSize: Int = 2048,
                 useHttps: Boolean = false,
                 overflowStrategy: OverflowStrategy = OverflowStrategy.backpressure)
                (implicit actorSystem: ActorSystem, mate: ActorMaterializer, ec: ExecutionContextExecutor) {
  private val client: (HttpRequest) => Future[HttpResponse] = createHttpClient(host, port, queueSize, useHttps, overflowStrategy)

  def send(request: HttpRequest): Future[HttpClientResponse] = {
    for {
      response <- client(request)
      entity <- response.entity.dataBytes.runFold(ByteString.empty)(_ ++ _)
    } yield {
      HttpClientResponse(response.status, entity, response.headers)
    }
  }

  def sendRaw(request: HttpRequest): Future[HttpResponse] = client(request)

  private def clientCachedPool[A](host: String, port: Int, https: Boolean = false)(implicit mat: ActorMaterializer) = {
    if (https) Http().cachedHostConnectionPoolHttps[A](host, port)
    else Http().cachedHostConnectionPool[A](host, port)
  }

  private def createHttpClient(host: String,
                               port: Int,
                               queueSize: Int,
                               useHttps: Boolean,
                               overflowStrategy: OverflowStrategy): (HttpRequest) => Future[HttpResponse] = {
    val sourceQueue = Source.queue[(HttpRequest, Promise[HttpResponse])](queueSize, overflowStrategy)
      .via(clientCachedPool(host, port, useHttps))
      .toMat(Sink.foreach {
        case (Success(httpResponse), promise) => promise.success(httpResponse)
        case (Failure(ex), promise) => promise.failure(ex)
      })(Keep.left)
      .run()

    def handleRequest(request: HttpRequest): Future[HttpResponse] = {
      val responsePromise = Promise[HttpResponse]()
      sourceQueue.offer(request -> responsePromise) flatMap {
        case QueueOfferResult.Enqueued => responsePromise.future
        case QueueOfferResult.Dropped => Future.failed(new RuntimeException("Queue overflowed. Try again later."))
        case QueueOfferResult.Failure(ex) => Future.failed(ex)
        case QueueOfferResult.QueueClosed => Future.failed(new RuntimeException("Queue was closed"))
      }
    }

    handleRequest
  }
}

object HttpClient {
  case class HttpClientResponse(status: StatusCode, content: ByteString, headers: Seq[HttpHeader]) {
    def header(key: String): Option[HttpHeader] = {
      val name = key.toLowerCase
      headers.find(_.is(name))
    }
  }

  def apply(host: String,
            port: Int,
            queueSize: Int = 2048,
            useHttps: Boolean = false,
            overflowStrategy: OverflowStrategy = OverflowStrategy.backpressure)
           (implicit actorSystem: ActorSystem, mate: ActorMaterializer, ec: ExecutionContextExecutor): HttpClient = {
    new HttpClient(host, port, queueSize, useHttps)
  }
}
