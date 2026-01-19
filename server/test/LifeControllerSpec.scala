package test

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.play._
import org.scalatestplus.play.guice._
import play.api.test._
import play.api.test.Helpers._


class LifeControllerSpec extends AnyWordSpec with Matchers with GuiceOneAppPerTest with Injecting {

  "LifeController" should {

    "getState should return JSON" in {
      val controller = inject[controllers.LifeController]
      val result = controller.getState(FakeRequest().withSession("state" -> "dummy"))

      status(result) shouldEqual OK
      contentType(result) should be(Some("application/json"))
      contentAsString(result) should startWith("[")
    }

  }
}
