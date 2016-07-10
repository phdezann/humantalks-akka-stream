package support

import akka.stream.scaladsl.Framing
import akka.util.ByteString
import play.api.mvc.{Action, Controller}

trait ControllerBase extends Controller {

  val delimiter = Framing.delimiter(ByteString("\n"), maximumFrameLength = 1024 * 10)

  def index = Action {
    Ok(views.html.index())
  }

  def sse = Action {
    Ok
  }
}
