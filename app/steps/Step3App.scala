package steps

import java.nio.file.Paths

import akka.stream.ClosedShape
import akka.stream.scaladsl.{Broadcast, FileIO, Flow, GraphDSL, RunnableGraph, Source}
import akka.util.ByteString
import support.AkkaStreamApp

import scala.concurrent.duration._

object Step3App extends AkkaStreamApp {

  def run = {
    val ints = Source(1 to 10)
    val evenFilter = Flow[Int].filter(_ % 2 == 0)
    val oddFilter = Flow[Int].filter(_ % 2 != 0)
    val toByteString = Flow[Int].map(value => ByteString(value.toString))
    val evenFileSink = FileIO.toPath(Paths.get("target/even.txt"))
    val oddSink = FileIO.toPath(Paths.get("target/odd.txt"))

    val graph = RunnableGraph.fromGraph(GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val broadcast = builder.add(Broadcast[Int](2))

      ints ~> broadcast.in
      broadcast.out(0) ~> evenFilter ~> toByteString ~> evenFileSink
      broadcast.out(1) ~> oddFilter ~> toByteString ~> oddSink

      ClosedShape
    })

    graph.run

    block(100 milliseconds)
  }
}
