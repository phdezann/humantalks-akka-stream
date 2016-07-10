package steps

import akka.stream.scaladsl.{Flow, Sink, Source}
import support.AkkaStreamApp

object Step1App extends AkkaStreamApp {

  def run = {
    val source = Source(1 to 10)
    val flow = Flow[Int].map(_ * 2)
    val sink = Sink.foreach[Int](println(_))

    val result = source.via(flow).runWith(sink)

    awaitCompletion(result)
  }
}

/**
  * For an infinite source, checkout Step2.
  */
