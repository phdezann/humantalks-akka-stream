package controllers

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpEntity.Chunked
import akka.http.scaladsl.model.{HttpRequest, _}
import akka.stream.SourceShape
import akka.stream.scaladsl.{Flow, GraphDSL, Source, ZipWith}
import akka.util.ByteString
import play.api.libs.EventSource
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Action
import support.ControllerBase

import scala.concurrent.duration._

class Step7Controller(implicit system: ActorSystem) extends ControllerBase {

  case class SensorValue(value: Long, timestamp: Long, idx: Int)

  override def sse = Action {
    val temperatureRequest = Source.single(HttpRequest(uri = "/temperature"))
    val humidityHumidity = Source.single(HttpRequest(uri = "/humidity"))
    val temperatureEndpoint = Http().outgoingConnection("temperature-sensor", 9001)
    val humidityEndpoint = Http().outgoingConnection("humidity-sensor", 9002)
    val toLines = Flow[HttpResponse].flatMapConcat { response =>
      response.entity match {
        case chunk: Chunked => chunk.dataBytes
        case _ => Source.empty
      }
    }.via(delimiter)
    def toSensorValues(idx: Int) = Flow[ByteString].map(line =>
      line.utf8String.split(' ') match {
        case Array(value, timestamp, _) => SensorValue(value.trim.toLong, timestamp.trim.toLong, idx)
      })
    val rewriteTimestamp = Flow[SensorValue].map(value => value.copy(timestamp = System.currentTimeMillis))
    val toJson = Flow[SensorValue].map(value => Json.toJson(value)(Json.writes[SensorValue]))
    val pickEverySecond = Flow[SensorValue].groupedWithin(1000, 1 second).map(_.last)

    val zipWith = ZipWith[SensorValue, SensorValue, List[SensorValue]]((in1, in2) => List(in1, in2))
    val flatten = Flow[List[SensorValue]].flatMapConcat(values => Source(values))

    val response = Source.fromGraph(GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val zip = builder.add(zipWith)
      val identity = builder.add(Flow[JsValue])

      temperatureRequest ~> temperatureEndpoint ~> toLines ~> toSensorValues(idx = 0) ~> pickEverySecond ~> zip.in0
      humidityHumidity ~> humidityEndpoint ~> toLines ~> toSensorValues(idx = 1) ~> pickEverySecond ~> zip.in1
      zip.out ~> flatten ~> rewriteTimestamp ~> toJson ~> identity

      SourceShape(identity.out)
    })
    Ok.chunked(response via EventSource.flow)
  }
}
