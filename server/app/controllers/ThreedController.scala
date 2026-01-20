package controllers

import play.api.mvc._
import org.webjars.play.WebJarsUtil
import javax.inject._

@Singleton
class ThreedController @Inject()(
    val controllerComponents: ControllerComponents,
    implicit val webJarsUtil: WebJarsUtil
) extends BaseController {

  def index = Action {
    Ok(views.html.threed())
  }

}
