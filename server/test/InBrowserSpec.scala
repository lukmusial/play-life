package test

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.play._
import org.scalatestplus.play.guice._

// Browser-based test - HtmlUnit has limitations with modern JavaScript/webjars
// This test is marked as pending as it requires a full browser to properly load webjars
class InBrowserSpec extends AnyWordSpec with Matchers with GuiceOneServerPerTest with OneBrowserPerTest with HtmlUnitFactory {

  "Browser" should {
    "connect to server page" ignore {
      // Note: HtmlUnit doesn't fully support webjars asset loading
      // This test would work with a real browser like Chrome/Firefox
      go to ("http://localhost:" + port)
      pageSource should include ("Auto Refresh")
    }
  }

}
