package portfolzio.model

import zio.prelude.NonEmptyList

enum AlbumEntry(id: String):
  /** @param id directory path + album file name, begins with `/` */
  case Album(
      id: String,
      children: Vector[AlbumEntry],
  ) extends AlbumEntry(id)

  /** @param id path of image's parent directory, begins with `/`
    * @param imageFiles image file names
    * @param rawFiles raw file names
    */
  case Image(
      id: String,
      info: ImageInfo,
      imageFiles: NonEmptyList[String],
      rawFiles: List[String],
  ) extends AlbumEntry(id)
