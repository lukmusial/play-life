package test

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.play._
import org.scalatestplus.play.guice._
import play.api.test._
import play.api.test.Helpers._

class MainControllerSpec extends AnyFlatSpec with Matchers with GuiceOneAppPerTest with Injecting {

  "Index" should "contain a correct string" in {
    val controller = inject[controllers.MainController]
    val result = controller.index(FakeRequest())
    status(result) should be (OK)
    contentType(result) should be (Some("text/html"))
    charset(result) should be (Some("utf-8"))
    contentAsString(result) should include ("Auto Refresh")
  }

}
