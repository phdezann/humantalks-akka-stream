package controllers

import javax.inject.{Inject, Singleton}

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream._
import akka.stream.scaladsl._
import akka.util.ByteString
import play.api.libs.EventSource
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, Controller}

import scala.concurrent.duration._

@Singleton
class AppController @Inject()(implicit system: ActorSystem) extends Controller {

  def index = Action {
    Ok(views.html.index())
  }

  def sse = Action {
    val response = Source.fromGraph(GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val temperatureEndpoint = Http().outgoingConnection("localhost", 9001)
      val humidityEndpoint = Http().outgoingConnection("localhost", 9002)

      val temperatureSource = Source.single(HttpRequest(uri = "/temperature")).via(temperatureEndpoint)
      val humiditySource = Source.single(HttpRequest(uri = "/humidity")).via(humidityEndpoint)

      //      val merge = builder.add(Merge[SensorValue](2))
      val zip = builder.add(ZipWith[SensorValue, SensorValue, Seq[SensorValue]]((temperature, humidity) => List(temperature, humidity)))
      val flow = builder.add(Flow[JsValue])
      val flatten = builder.add(Flow[Seq[SensorValue]].flatMapConcat(pair => Source.fromIterator(() => pair.iterator)))

      temperatureSource ~> toChunks ~> delimiter ~> sensorLineParser(idx = 0) ~> accumulator ~> zip.in0
      humiditySource ~> toChunks ~> delimiter ~> sensorLineParser(idx = 1) ~> accumulator ~> zip.in1
      zip.out ~> flatten ~> toJson ~> flow

      SourceShape(flow.out)
    })

    Ok.chunked(response via EventSource.flow)
  }

  case class SensorValue(idx: Int, value: Long, timestamp: Long)

  lazy implicit val SensorValueWrites = Json.writes[SensorValue]

  def sensorLineParser(idx: Int) = Flow[ByteString].map(line => {
    line.utf8String.split(' ') match {
      case Array(value, timestamp) => SensorValue(idx, value.trim.toLong, timestamp.trim.toLong)
    }
  })

  val toJson = Flow[SensorValue].map(value => Json.toJson(value))

  val removeObsoleteValues = Flow[SensorValue].filter(value => Math.abs(value.timestamp - System.currentTimeMillis()) < 1000)

  val toChunks = Flow[HttpResponse].flatMapConcat { response =>
    response.entity match {
      case chunk: HttpEntity.Chunked => chunk.dataBytes
      case _ => Source.empty
    }
  }

  val delimiter = Framing.delimiter(ByteString("\n"), maximumFrameLength = 100, allowTruncation = true)

  val accumulator = Flow[SensorValue].groupedWithin(10, 1 minute).map(all => {
    val values = all.map(_.value)
    SensorValue(all.head.idx, values.sum / values.size, all.head.timestamp)
  })

  val throttler = Flow[SensorValue].throttle(100, 500 seconds, 1, ThrottleMode.Shaping)
}
