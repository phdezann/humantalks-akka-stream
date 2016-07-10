package sensors

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
import scala.concurrent.{Await, Future, Promise}

class SensorHttpServer(interface: String = "0.0.0.0", port: Int, defaultPath: String, minValue: Int, maxValue: Int, rate: FiniteDuration) {
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
          .map(value => toChunkPayload(value))
          .map(chunkPayload => {
            print('.')
            chunkPayload
          })
        val response = source.throttle(1, rate, 1, ThrottleMode.Shaping)
        complete(HttpEntity(ContentTypes.`text/plain(UTF-8)`, response))
      }
    }


  private def toChunkPayload(value: Int) = {
    val paddedValue = f"$value%02d"
    val timestamp = System.currentTimeMillis().toString
    /* useful for demo: this artificially increases each chunk (4ko) and therefore fills up socket buffers more quickly */
    val padding = List.fill(1024 * 4 - paddedValue.length - timestamp.length - 3)('_').foldLeft("")(_ + _)
    val payload = paddedValue + " " + timestamp + " " + padding + "\n"
    ByteString(payload)
  }

  def startAndWait() = {
    Http().bindAndHandle(route, interface, port)
    println(s"Server online at http://${interface}:${port}")
    waitIndefinitely
  }

  private def waitIndefinitely: Future[Nothing] = Await.ready(Promise().future, Duration.Inf)
}

object TemperatureSensor extends App {
  val MinTemperatureValue = 5
  val MaxTemperatureLevel = 40
  new SensorHttpServer(
    port = 9001,
    defaultPath = "temperature",
    minValue = MinTemperatureValue,
    maxValue = MaxTemperatureLevel,
    rate = 20 milliseconds).startAndWait()
}

object HumiditySensor extends App {
  val MinHumidityLevel = 10
  val MaxHumidityLevel = 90
  new SensorHttpServer(
    port = 9002,
    defaultPath = "humidity",
    minValue = MinHumidityLevel,
    maxValue = MaxHumidityLevel,
    rate = 20 milliseconds).startAndWait()
}
