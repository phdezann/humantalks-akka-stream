package controllers

import javax.inject.Singleton
import play.api.mvc.{Action, Controller}

@Singleton
class AppController extends Controller {

  def index = Action {
    Ok(views.html.index())
  }
}
