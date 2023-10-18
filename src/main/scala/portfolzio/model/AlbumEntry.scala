package portfolzio.model

import portfolzio.model.AlbumEntry.{Album, Id}
import zio.prelude.{Equal, EqualOps, NonEmptyList}

import java.nio.file.{Path, Paths}
import scala.annotation.tailrec

sealed trait AlbumEntry:
  def id: Id

object AlbumEntry:

  /** @param id directory path + album file name */
  case class Album(
    override val id: Id,
    childSelectors: List[IdSelector],
  ) extends AlbumEntry

  /** @param id         path of image's parent directory
    * @param imageFiles image file paths
    * @param rawFiles   raw file paths
    */
  case class Image(
    override val id: Id,
    info: ImageInfo,
    imageFiles: NonEmptyList[Path],
    rawFiles: List[Path],
  ) extends AlbumEntry

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

  implicit def equal: Equal[Id] = Equal.make[Id]((a, b) => a == b)

  extension (album: Album)
    def name: String = album.id.lastPart

  object Id:
    def safe(input: String): Id = "/" + input.dropWhile(_ === '/').reverse.dropWhile(_ === '/').reverse
    def unsafe(input: String): Id = input
    def unapply(id: Id): Option[String] = Some(id.value)

  extension (id: Id)
    def value: String = id
    def relativePath: Path = Paths.get(id.stripPrefix("/"))
    def lastPart: String = id.reverse.takeWhile(_ != '/').reverse

  /** String possibly containing * wildcards, to match with multiple IDs.
    * Possibly starts with `tag:` to match tags
    */
  opaque type IdSelector = String

  object IdSelector:
    def fromString(str: String): IdSelector = str

    /** Loops over selectors and items in order so that items matched by the first selector come up
      * in the result first, but such that items don't appear more than once.
      */
    def findMatches(basePath: String)(
      selectors: Iterable[IdSelector],
      items: Iterable[AlbumEntry],
    ): List[AlbumEntry] = {

      @tailrec
      def matches(query: List[String], candidate: List[String]): Boolean = (query, candidate) match
        case (Nil, Nil)                          => true
        case ("*" :: _, _ :: _)                  => true
        case (q :: qtail, c :: ctail) if q === c => matches(qtail, ctail)
        case _                                   => false

      val indexedItems = items.toIndexedSeq
      val matched = Array.fill(indexedItems.length)(false)
      var result = List.empty[AlbumEntry]
      for
        selector <- selectors
        i <- indexedItems.indices if !matched(i)
        item = indexedItems(i)
        shouldMatch = if (selector.startsWith("tag:")) {
          val tag = selector.stripPrefix("tag:")
          item match
            case img: Image => img.info.tags.exists(_.contains(tag))
            case _          => false
        } else {
          val absoluteSelector = if (selector.startsWith("/")) selector else basePath.stripSuffix("/") + "/" + selector
          val query = absoluteSelector.split('/').toList
          matches(query, item.id.value.split('/').toList)
        }
      yield
        if (shouldMatch) {
          matched(i) = true
          result = item :: result
        }
      result.reverse

    }
