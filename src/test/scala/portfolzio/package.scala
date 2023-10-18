import portfolzio.model.AlbumEntry.IdSelector
import portfolzio.model.{AlbumEntry, ImageInfo}
import zio.prelude.NonEmptyList

import java.nio.file.Paths

package object portfolzio {
  def img(id: String) = AlbumEntry.Image(
    AlbumEntry.Id.unsafe(id),
    ImageInfo(),
    imageFiles = NonEmptyList(Paths.get("/")),
    rawFiles = List.empty,
  )

  def album(id: String, children: List[String]) = AlbumEntry.Album(
    AlbumEntry.Id.unsafe(id),
    children.map(IdSelector.fromString),
  )
}
