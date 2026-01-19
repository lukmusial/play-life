package controllers

import play.api.mvc._
import play.api.libs.json.Json
import models.com.bulba._
import scala.jdk.CollectionConverters._
import com.google.common.cache.CacheBuilder
import java.util.concurrent.TimeUnit
import org.webjars.play.WebJarsUtil
import javax.inject._

@Singleton
class ThreedController @Inject()(
    val controllerComponents: ControllerComponents,
    implicit val webJarsUtil: WebJarsUtil
) extends BaseController {

  val states = CacheBuilder.
    newBuilder().
    expireAfterAccess(1, TimeUnit.HOURS).
    build[String, Game3DState[VC, VVC]]().asMap().asScala

  def index = Action {
    Ok(views.html.threed())
  }

  def getState = Action {
    implicit request =>
      session.get("state") match {

        case Some(sessionState) =>
          if (!states.contains(sessionState)) {
            resetHelper(session.get("layers").map(_.toInt).getOrElse(20), session.get("height").map(_.toInt).getOrElse(300), session.get("width").map(_.toInt).getOrElse(424))
          } else {
            val state = states(sessionState)
            states += (sessionState -> state)
            state.advance()
            Ok(Json.toJson(state.toNumericSequence))
              .withSession("state" -> sessionState)
          }

        case None =>
          throw new Exception("not initialized")
      }

  }


  def resetHelper(layers: Int, height: Int, width: Int)(implicit request: Request[AnyContent]): Result = {
    session.get("state") match {

      case Some(sessionState) =>
        states += (sessionState -> new Game3DState[VC, VVC](Universe(layers, width, height)))
        Ok(Json.toJson(states(sessionState).toNumericSequence))
          .withSession("state" -> sessionState, "layers" -> layers.toString, "height" -> height.toString, "width" -> width.toString)

      case None =>
        val state = new Game3DState[VC, VVC](Universe(layers, width, height))
        states += (state.hashCode().toString -> state)
        Ok(Json.toJson(state.toNumericSequence))
          .withSession("state" -> state.hashCode().toString)
    }
  }

  def reset(layers: Int, height: Int, width: Int) = Action {
      implicit request =>
        resetHelper(layers, height, width)
 }

}
