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

    "serve the landing page with correct structure" in {
      val wsClient = app.injector.instanceOf[WSClient]
      val response = Await.result(
        wsClient.url(s"http://localhost:$port/").get(),
        10.seconds
      )

      response.status shouldBe OK
      response.contentType should include("text/html")

      val body = response.body
      body should include("Game of Life")
      body should include("2D Simulation")
      body should include("3D Simulation")
      body should include("""href="/2d"""")
      body should include("""href="/3d"""")
    }

    "serve the 2D page with correct structure" in {
      val wsClient = app.injector.instanceOf[WSClient]
      val response = Await.result(
        wsClient.url(s"http://localhost:$port/2d").get(),
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

    "serve 3D page with all rule buttons" in {
      val wsClient = app.injector.instanceOf[WSClient]
      val response = Await.result(
        wsClient.url(s"http://localhost:$port/threed").get(),
        10.seconds
      )

      response.status shouldBe OK
      val body = response.body

      // Verify all rule buttons exist with correct data-rule attributes
      body should include("""data-rule="4555"""")
      body should include("""data-rule="5766"""")
      body should include("""data-rule="pyroclastic"""")
      body should include("""data-rule="crystal"""")
      body should include("""data-rule="original"""")
      body should include("""data-rule="vonneumann"""")

      // Verify all buttons have the rule-btn class
      body should include("""class="btn btn-sm btn-outline-info rule-btn" data-rule="4555"""")
      body should include("""class="btn btn-sm btn-outline-info rule-btn" data-rule="5766"""")
      body should include("""class="btn btn-sm btn-outline-info rule-btn" data-rule="pyroclastic"""")
      body should include("""class="btn btn-sm btn-outline-info rule-btn" data-rule="crystal"""")
      body should include("""class="btn btn-sm btn-outline-info rule-btn" data-rule="original"""")
      body should include("""class="btn btn-sm btn-outline-info rule-btn" data-rule="vonneumann"""")

      // Verify control panel has high z-index
      body should include("z-index: 1000")
    }

    "have Scala.js GameClient loaded" in {
      val wsClient = app.injector.instanceOf[WSClient]
      val response = Await.result(
        wsClient.url(s"http://localhost:$port/threed").get(),
        10.seconds
      )

      val body = response.body
      // Verify Scala.js script is included
      body should include("scalajs/main.js")
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

    "include correct webjar paths in 2D page" in {
      val wsClient = app.injector.instanceOf[WSClient]
      val response = Await.result(
        wsClient.url(s"http://localhost:$port/2d").get(),
        10.seconds
      )

      val body = response.body
      body should include("/webjars/bootstrap/5.3.3/css/bootstrap.min.css")
      body should include("/webjars/jquery/3.7.1/jquery.min.js")
    }

  }
}
