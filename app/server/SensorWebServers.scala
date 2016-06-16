package server

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.Marshaller._
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives._
import akka.stream.scaladsl._
import akka.stream.{ActorMaterializer, ThrottleMode}
import akka.util.ByteString
import org.apache.commons.lang3.RandomUtils

import scala.concurrent.duration._
import scala.io.StdIn

class WebServer(interface: String = "localhost", port: Int, defaultPath: String, minValue: Int, maxValue: Int, rate: FiniteDuration) {
  implicit val system = ActorSystem("system")
  implicit val executionContext = system.dispatcher
  implicit val materializer = ActorMaterializer()

  val route =
    path(defaultPath) {
      get {
        val source = Source
          .repeat(NotUsed)
          .map(_ => RandomUtils.nextInt(minValue, maxValue))
          .sliding(2)
          .mapConcat { case Seq(prev, curr) =>
            val step = if (curr > prev) 1 else -1;
            prev until curr by step
          }
          .map(value => ByteString(value + " " + System.currentTimeMillis() + "\n"))
          .throttle(1, rate, 1, ThrottleMode.Shaping)
        complete(HttpEntity(ContentTypes.`text/plain(UTF-8)`, source))
      }
    }

  def start() = {
    val bindingFuture = Http().bindAndHandle(route, interface, port)

    println(
      s"""Server online at http://${interface}:${port}
          |Press RETURN to stop...""".stripMargin)
    StdIn.readLine()
    bindingFuture
      .flatMap(_.unbind())
      .onComplete(_ => system.terminate())
  }
}

object TemperatureWebServer extends App {
  val MinTemperatureValue = 5
  val MaxHumidityLevel = 40
  new WebServer(
    port = 9001,
    defaultPath = "temperature",
    minValue = MinTemperatureValue,
    maxValue = MaxHumidityLevel,
    rate = 50 milliseconds).start()
}

object HumidityWebServer extends App {
  val MinHumidityLevel = 10
  val MaxHumidityLevel = 90
  new WebServer(
    port = 9002,
    defaultPath = "humidity",
    minValue = MinHumidityLevel,
    maxValue = MaxHumidityLevel,
    rate = 50 milliseconds).start()
}
