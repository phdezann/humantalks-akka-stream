package steps

import akka.stream.scaladsl.{Flow, Sink, Source}
import support.AkkaStreamApp

import scala.concurrent.duration._

object Step2App extends AkkaStreamApp {

  def run = {
    val source = Source.cycle(() => (1 to 10).iterator)
    val flow = Flow[Int].map(_ * 2)
    val sink = Sink.foreach[Int](println(_))

    source.via(flow).runWith(sink)

    block(10 milliseconds)
  }
}
