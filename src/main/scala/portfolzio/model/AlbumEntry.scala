package portfolzio.model

import portfolzio.model.AlbumEntry.Id
import zio.prelude.NonEmptyList

import java.nio.file.{Path, Paths}

enum AlbumEntry(val id: Id):
  /** @param id directory path + album file name */
  case Album(
    override val id: Id,
    children: Vector[String],
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
