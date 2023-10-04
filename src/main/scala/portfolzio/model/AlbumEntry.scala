package portfolzio.model

import portfolzio.model.AlbumEntry.{Id, IdSelector}
import zio.prelude.NonEmptyList

import java.nio.file.{Path, Paths}
import scala.annotation.tailrec

enum AlbumEntry(val id: Id):
  /** @param id directory path + album file name */
  case Album(
    override val id: Id,
    children: List[IdSelector],
  ) extends AlbumEntry(id)

  /** @param id         path of image's parent directory
    * @param imageFiles image file paths
    * @param rawFiles   raw file paths
    */
  case Image(
    override val id: Id,
    info: ImageInfo,
    imageFiles: NonEmptyList[Path],
    rawFiles: List[Path],
  ) extends AlbumEntry(id)

object AlbumEntry:

  /** String that begins with a '/' but does not end with one */
  opaque type Id = String

  object Id:
    def safe(input: String): Id = "/" + input.dropWhile(_ == '/').reverse.dropWhile(_ == '/').reverse
    def unsafe(input: String): Id = input

  extension (id: Id)
    def value: String = id
    def relativePath: Path = Paths.get(id.stripPrefix("/"))

  /** String possibly containing * wildcards, to match with multiple IDs */
  opaque type IdSelector = String

  object IdSelector:
    def fromString(str: String): IdSelector = str

  extension (selector: IdSelector)
    def findMatches(basePath: String, from: List[Id]): List[Id] = {
      val absoluteSelector = if (selector.startsWith("/")) selector else basePath + "/" + selector

      @tailrec
      def matches(query: List[String], candidate: List[String]): Boolean = (query, candidate) match
        case (Nil, Nil)                         => true
        case ("*" :: _, _ :: _)                 => true
        case (q :: qtail, c :: ctail) if q == c => matches(qtail, ctail)
        case _                                  => false

      from.filter(id =>
        matches(
          absoluteSelector.split("/").toList,
          id.split("/").toList,
        )
      )
    }
