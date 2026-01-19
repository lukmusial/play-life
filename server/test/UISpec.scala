package test

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.play._
import org.scalatestplus.play.guice._
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.ws.WSClient
import scala.concurrent.duration._
import scala.concurrent.Await

class UISpec extends AnyWordSpec with Matchers with GuiceOneServerPerSuite {

  "The Application" should {

    "serve the index page with correct structure" in {
      val wsClient = app.injector.instanceOf[WSClient]
      val response = Await.result(
        wsClient.url(s"http://localhost:$port/").get(),
        10.seconds
      )

      response.status shouldBe OK
      response.contentType should include("text/html")

      val body = response.body
      body should include("Game of Life")
      body should include("id=\"autoRefreshButton\"")
      body should include("id=\"singleRefreshButton\"")
      body should include("id=\"resetButton\"")
      body should include("<canvas")
    }

    "serve the 3D page with correct structure" in {
      val wsClient = app.injector.instanceOf[WSClient]
      val response = Await.result(
        wsClient.url(s"http://localhost:$port/threed").get(),
        10.seconds
      )

      response.status shouldBe OK
      response.contentType should include("text/html")

      val body = response.body
      body should include("Three D")
      body should include("id=\"autoRefreshButton\"")
      body should include("id=\"resetButton\"")
      body should include("vertexshader")
      body should include("fragmentshader")
    }

    "serve Bootstrap CSS from webjars" in {
      val wsClient = app.injector.instanceOf[WSClient]
      val response = Await.result(
        wsClient.url(s"http://localhost:$port/webjars/bootstrap/5.3.3/css/bootstrap.min.css").get(),
        10.seconds
      )

      response.status shouldBe OK
      response.contentType should include("text/css")
      response.body should include("Bootstrap")
    }

    "serve jQuery from webjars" in {
      val wsClient = app.injector.instanceOf[WSClient]
      val response = Await.result(
        wsClient.url(s"http://localhost:$port/webjars/jquery/3.7.1/jquery.min.js").get(),
        10.seconds
      )

      response.status shouldBe OK
      response.contentType should include("javascript")
      response.body should include("jQuery")
    }

    "serve application CSS" in {
      val wsClient = app.injector.instanceOf[WSClient]
      val response = Await.result(
        wsClient.url(s"http://localhost:$port/assets/stylesheets/index.css").get(),
        10.seconds
      )

      response.status shouldBe OK
      response.contentType should include("text/css")
    }

    "serve JavaScript routes" in {
      val wsClient = app.injector.instanceOf[WSClient]
      val response = Await.result(
        wsClient.url(s"http://localhost:$port/assets/javascripts/routes").get(),
        10.seconds
      )

      response.status shouldBe OK
      response.contentType should include("javascript")
      response.body should include("jsRoutes")
      response.body should include("LifeController")
    }

    "return JSON from 2D reset endpoint" in {
      val wsClient = app.injector.instanceOf[WSClient]
      val response = Await.result(
        wsClient.url(s"http://localhost:$port/reset/height/100/width/100").post(""),
        10.seconds
      )

      response.status shouldBe OK
      response.contentType should include("application/json")
      // Response should be a JSON array starting with height and width
      response.body should startWith("[")
      response.body should include("100")
    }

    "return JSON from 2D getState endpoint after reset" in {
      val wsClient = app.injector.instanceOf[WSClient]

      // First reset to initialize state
      val resetResponse = Await.result(
        wsClient.url(s"http://localhost:$port/reset/height/50/width/50").post(""),
        10.seconds
      )
      resetResponse.status shouldBe OK

      // Extract session cookie
      val cookies = resetResponse.cookies
      val sessionCookie = cookies.find(_.name == "PLAY_SESSION")
      sessionCookie shouldBe defined

      // Now get state with session
      val stateResponse = Await.result(
        wsClient.url(s"http://localhost:$port/life")
          .addCookies(sessionCookie.get)
          .get(),
        10.seconds
      )

      stateResponse.status shouldBe OK
      stateResponse.contentType should include("application/json")
      stateResponse.body should startWith("[")
    }

    "return JSON from 3D reset endpoint" in {
      val wsClient = app.injector.instanceOf[WSClient]
      val response = Await.result(
        wsClient.url(s"http://localhost:$port/threed/reset/layers/5/height/20/width/20").post(""),
        10.seconds
      )

      response.status shouldBe OK
      response.contentType should include("application/json")
      response.body should startWith("[")
    }

    "return JSON from 3D getState endpoint after reset" in {
      val wsClient = app.injector.instanceOf[WSClient]

      // First reset to initialize 3D state
      val resetResponse = Await.result(
        wsClient.url(s"http://localhost:$port/threed/reset/layers/3/height/10/width/10").post(""),
        10.seconds
      )
      resetResponse.status shouldBe OK

      // Extract session cookie
      val cookies = resetResponse.cookies
      val sessionCookie = cookies.find(_.name == "PLAY_SESSION")
      sessionCookie shouldBe defined

      // Now get state with session
      val stateResponse = Await.result(
        wsClient.url(s"http://localhost:$port/threed/life")
          .addCookies(sessionCookie.get)
          .get(),
        10.seconds
      )

      stateResponse.status shouldBe OK
      stateResponse.contentType should include("application/json")
      stateResponse.body should startWith("[")
    }

    "include correct webjar paths in index page" in {
      val wsClient = app.injector.instanceOf[WSClient]
      val response = Await.result(
        wsClient.url(s"http://localhost:$port/").get(),
        10.seconds
      )

      val body = response.body
      body should include("/webjars/bootstrap/5.3.3/css/bootstrap.min.css")
      body should include("/webjars/jquery/3.7.1/jquery.min.js")
    }

  }
}
