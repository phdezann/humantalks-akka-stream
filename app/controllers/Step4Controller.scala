package controllers

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpEntity.Chunked
import akka.http.scaladsl.model.{HttpRequest, _}
import akka.stream.SourceShape
import akka.stream.scaladsl.{Flow, GraphDSL, Source}
import akka.util.ByteString
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Action
import support.ControllerBase

class Step4Controller(implicit system: ActorSystem) extends ControllerBase {

  case class SensorValue(value: Long, timestamp: Long)

  override def index = Action {
    val request = Source.single(HttpRequest(uri = "/temperature"))
    val endpoint = Http().outgoingConnection("temperature-sensor", 9001)
    val toLines = Flow[HttpResponse].flatMapConcat { response =>
      response.entity match {
        case chunk: Chunked => chunk.dataBytes
        case _ => Source.empty
      }
    }.via(delimiter)
    val toSensorValues = Flow[ByteString].map(line =>
      line.utf8String.split(' ') match {
        case Array(value, timestamp, _) => SensorValue(value.trim.toLong, timestamp.trim.toLong)
      })
    val toJson = Flow[SensorValue].map(value => Json.toJson(value)(Json.writes[SensorValue]))

    val response = Source.fromGraph(GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val identity = builder.add(Flow[JsValue])

      request ~> endpoint ~> toLines ~> toSensorValues ~> toJson ~> identity

      SourceShape(identity.out)
    })
    Ok.chunked(response)
  }
}
