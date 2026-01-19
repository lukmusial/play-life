package test

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.play._
import org.scalatestplus.play.guice._

// Browser-based test - requires additional selenium dependencies
// Keeping for reference but skipped by default
class InBrowserSpec extends AnyWordSpec with Matchers with GuiceOneServerPerTest with OneBrowserPerTest with HtmlUnitFactory {

  "Browser" should {
    "connect to server page" in {
      go to ("http://localhost:" + port)
      pageSource should include ("Auto Refresh")
    }
  }

}
