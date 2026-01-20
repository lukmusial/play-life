package test

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.play._
import org.scalatestplus.play.guice._
import play.api.test.Helpers._
import play.api.libs.ws.WSClient
import scala.concurrent.duration._
import scala.concurrent.Await

/**
 * Tests for rule button HTML structure and JavaScript correctness.
 */
class RuleButtonSpec extends AnyWordSpec with Matchers with GuiceOneServerPerSuite {

  "3D Rule Buttons" should {

    "all be present in HTML with correct attributes" in {
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
    }

    "have consistent button classes" in {
      val wsClient = app.injector.instanceOf[WSClient]
      val response = Await.result(
        wsClient.url(s"http://localhost:$port/threed").get(),
        10.seconds
      )

      val body = response.body

      // All buttons should have btn-outline-info class initially (blue outline)
      body should include("""btn-outline-info rule-btn" data-rule="4555"""")
      body should include("""btn-outline-info rule-btn" data-rule="5766"""")
      body should include("""btn-outline-info rule-btn" data-rule="pyroclastic"""")
    }

    "have JavaScript that uses attr() not data() for rule names" in {
      val wsClient = app.injector.instanceOf[WSClient]
      val response = Await.result(
        wsClient.url(s"http://localhost:$port/assets/javascripts/3dindex.js").get(),
        10.seconds
      )

      val body = response.body

      // Should use attr("data-rule") to avoid jQuery number conversion
      body should include(""".attr("data-rule")""")
      // Should NOT use .data("rule") which converts numbers
      body should not include """.data("rule")"""
    }

    "have rule descriptions defined for all rules" in {
      val wsClient = app.injector.instanceOf[WSClient]
      val response = Await.result(
        wsClient.url(s"http://localhost:$port/assets/javascripts/3dindex.js").get(),
        10.seconds
      )

      val body = response.body

      // Verify ruleDescriptions object has all rules
      body should include(""""4555":""")
      body should include(""""5766":""")
      body should include(""""pyroclastic":""")
      body should include(""""crystal":""")
      body should include(""""original":""")
      body should include(""""vonneumann":""")
    }

    "have control panel with high z-index to stay above WebGL canvas" in {
      val wsClient = app.injector.instanceOf[WSClient]
      val response = Await.result(
        wsClient.url(s"http://localhost:$port/threed").get(),
        10.seconds
      )

      val body = response.body
      body should include("z-index: 1000")
    }

    "have WebGL canvas with lower z-index" in {
      val wsClient = app.injector.instanceOf[WSClient]
      val response = Await.result(
        wsClient.url(s"http://localhost:$port/assets/javascripts/3ddraw.js").get(),
        10.seconds
      )

      val body = response.body
      // Canvas should have z-index: 1 (lower than control panel's 1000)
      body should include("""zIndex = '1'""")
    }
  }
}
