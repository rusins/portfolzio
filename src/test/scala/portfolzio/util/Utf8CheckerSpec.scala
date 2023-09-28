package portfolzio.util

import zio.*
import zio.test.{test, *}
import zio.test.Assertion.*

import java.nio.file.Path
import scala.io.Source

object Utf8CheckerSpec extends ZIOSpecDefault {

  override def spec = suite("Utf8CheckerSpec")(
    test("utf8 checker detects text files as UTF-8") {
      val path = getClass.getResource("/text_file").getPath
      for isUtf8 <- Utf8Checker.checkFile(path)
      yield assertTrue(isUtf8 == true)
    },
    test("utf8 checker detects image file as NOT UTF-8") {
      val path = getClass.getResource("/day-night.jpg").getPath
      for isUtf8 <- Utf8Checker.checkFile(path)
      yield assertTrue(isUtf8 == false)
    },
  )

}
