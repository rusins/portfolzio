package portfolzio.util

import portfolzio.util.ExifHelpers.*
import zio.test.Assertion.*
import zio.test.{ZIOSpecDefault, test, *}

import java.time.LocalDateTime

object ExifHelperSpec extends ZIOSpecDefault {

  override def spec = suite("ExifHelperSpec")(
    test("DateTimeFormatter correctly parses Exif date time") {
      val str = "'2021:10:17 00:33:35'"
      assert(parseExifDateTime(str))(isSome(equalTo(LocalDateTime.of(
        2021,
        10,
        17,
        0,
        33,
        35,
      ))))
    }
  )

}
