package controllers

import play.api.mvc._
import play.api.libs.json.Json
import play.api.routing.JavaScriptReverseRouter
import models.com.bulba.GameState
import scala.jdk.CollectionConverters._
import com.google.common.cache.CacheBuilder
import java.util.concurrent.TimeUnit
import models.com.bulba.canvas.RandomCanvas
import javax.inject._

@Singleton
class LifeController @Inject()(val controllerComponents: ControllerComponents) extends BaseController {

  val states = CacheBuilder.
    newBuilder().
    expireAfterAccess(1, TimeUnit.HOURS).
    build[String, GameState]().
    asMap().
    asScala

  def getState = Action {
    implicit request =>
      session.get("state") match {

        case Some(sessionState) =>
          if (!states.contains(sessionState)) {
            resetHelper(session.get("height").map(_.toInt).getOrElse(300), session.get("width").map(_.toInt).getOrElse(424))
          } else {
            val state = states(sessionState)
            states += (sessionState -> state)
            state.advance()
            Ok(Json.toJson(state.toHex))
              .withSession("state" -> sessionState)
          }

        case None =>
          throw new Exception("not initialized")
      }

  }


  def resetHelper(height: Int, width: Int)(implicit request: Request[AnyContent]): Result = {
    session.get("state") match {

      case Some(sessionState) =>
        states += (sessionState -> new GameState(RandomCanvas(height, width)))
        Ok(Json.toJson(states(sessionState).toHex))
          .withSession("state" -> sessionState, "height" -> height.toString, "width" -> width.toString)

      case None =>
        val state = new GameState(RandomCanvas(height, width))
        states += (state.hashCode().toString -> state)
        Ok(Json.toJson(state.toHex))
          .withSession("state" -> state.hashCode().toString)
    }
  }

  def reset(height: Int, width: Int) = Action {
      implicit request =>
        resetHelper(height, width)
 }


  def javascriptRoutes = Action {
    implicit request =>
      Ok(JavaScriptReverseRouter("jsRoutes")(
        routes.javascript.LifeController.getState,
        routes.javascript.LifeController.reset,
        routes.javascript.ThreedController.getState,
        routes.javascript.ThreedController.reset
      )).as(JAVASCRIPT)
  }

}
