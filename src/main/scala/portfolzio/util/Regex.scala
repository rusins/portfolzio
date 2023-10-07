package portfolzio.util

import scala.util.matching.Regex

object Regex {
  // Update README.md if you change this
  val ImageRegex: Regex = "(?i).*\\.(jpg|jpeg)$".r
  // Update README.md if you change this
  val RawFileRegex: Regex = "(?i).*\\.(arw|raw)$".r
  // Update README.md if you significantly change this
  val AlbumRegex: Regex = ".*\\.album$".r

  val DelimiterRegex: Regex = "[-_.\\\\]".r
}
