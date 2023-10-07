package portfolzio.util

import portfolzio.util.Regex.{AlbumRegex, DelimiterRegex, ImageRegex, RawFileRegex}
import zio.*
import zio.test.*
import zio.test.Assertion.*

object RegexSpec extends ZIOSpecDefault {
  override def spec = suite("RegexSpec")(
    test("ImageRegex") {
      assert("image.jpg")(matchesRegex(ImageRegex.regex)) &&
        assert("image.jpeg")(matchesRegex(ImageRegex.regex)) &&
        assert("image.JPG")(matchesRegex(ImageRegex.regex)) &&
        assert("image.JPEG")(matchesRegex(ImageRegex.regex)) &&
        assert("image.jpg.xsf")(not(matchesRegex(ImageRegex.regex)))
    },
    test("RawFileRegex") {
      assert("image.raw")(matchesRegex(RawFileRegex.regex)) &&
        assert("image.arw")(matchesRegex(RawFileRegex.regex)) &&
        assert("image.RAW")(matchesRegex(RawFileRegex.regex)) &&
        assert("image.ARW")(matchesRegex(RawFileRegex.regex)) &&
        assert("image.raw.copy")(not(matchesRegex(RawFileRegex.regex)))
    },
    test("AlbumRegex") {
      assert("Norway.album")(matchesRegex(AlbumRegex.regex)) &&
        assert("Norway")(not(matchesRegex(AlbumRegex.regex)))
    },
    test("DelimiterRegex") {
      assert("me.enjoying-a_peaceful\\walk".replaceAll(DelimiterRegex.regex, " "))(
        equalTo("me enjoying a peaceful walk")
      )
    },
  )
}
