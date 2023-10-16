package portfolzio.model

import portfolzio.model.AlbumEntry.{Album, Id, IdSelector}
import zio.prelude.NonEmptyList

import java.nio.file.{Path, Paths}
import scala.annotation.tailrec

enum AlbumEntry(val id: Id):
  /** @param id directory path + album file name */
  case Album(
    override val id: Id,
    childSelectors: List[IdSelector],
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

  extension (entries: List[AlbumEntry])
    def partitionAlbumEntries: (List[Album], List[Image]) =
      entries.foldLeft((List.empty[Album], List.empty[Image])) { case ((albumAcc, imageAcc), image) =>
        image match {
          case img: Image => (albumAcc, img :: imageAcc)
          case alb: Album => (alb :: albumAcc, imageAcc)
        }
      }

  /** String that begins with a '/' but does not end with one */
  opaque type Id = String

  extension (album: Album)
    def name: String = album.id.lastPart

  object Id:
    def safe(input: String): Id = "/" + input.dropWhile(_ == '/').reverse.dropWhile(_ == '/').reverse
    def unsafe(input: String): Id = input
    def unapply(id: Id): Option[String] = Some(id.value)

  extension (id: Id)
    def value: String = id
    def relativePath: Path = Paths.get(id.stripPrefix("/"))
    def lastPart: String = id.reverse.takeWhile(_ != '/').reverse

  /** String possibly containing * wildcards, to match with multiple IDs */
  opaque type IdSelector = String

  object IdSelector:
    def fromString(str: String): IdSelector = str

    /** Loops over selectors and items in order so that items matched by the first selector come up
      * in the result first, but such that items don't appear more than once.
      */
    def findMatches(basePath: String)(selectors: Iterable[IdSelector], items: Iterable[Id]): List[Id] = {

      @tailrec
      def matches(query: List[String], candidate: List[String]): Boolean = (query, candidate) match
        case (Nil, Nil)                         => true
        case ("*" :: _, _ :: _)                 => true
        case (q :: qtail, c :: ctail) if q == c => matches(qtail, ctail)
        case _                                  => false

      val queries = selectors.map(selector =>
        val absoluteSelector = if (selector.startsWith("/")) selector else basePath.stripSuffix("/") + "/" + selector
        absoluteSelector.split('/').toList
      )
      val indexedItems = items.toIndexedSeq
      val matched = Array.fill(indexedItems.length)(false)
      var result = List.empty[Id]
      for
        query <- queries
        i <- indexedItems.indices if !matched(i)
        item = indexedItems(i)
      yield
        if (matches(query, item.split('/').toList)) {
          matched(i) = true
          result = item :: result
        }
      result.reverse

    }
