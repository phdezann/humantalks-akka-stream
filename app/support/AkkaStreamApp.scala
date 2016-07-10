package support

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

trait AkkaStreamApp extends App {
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  def run
  def block(atMost: Duration) = Thread.sleep(atMost.toMillis)
  def awaitCompletion[T](future: Future[T]) = awaitResult(future)
  def awaitResult[T](future: Future[T]) = Await.result(future, Duration.Inf)

  run
  system.terminate
}
