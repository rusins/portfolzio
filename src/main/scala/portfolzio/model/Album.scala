package portfolzio.model

/** Represents either an image or an album.
  * @param uid directory path + name of this album or image
  * @param children sub-albums or sub-images
  * @param previewFile path to preview image / album cover
  * @param ImageInfo Some if this album is a single image
  * @param rawFile path to raw file if this is an image
  */
case class Album(
    uid: String,
    children: Vector[Album],
    previewFile: Option[String],
    ImageInfo: Option[ImageInfo],
    rawFile: Option[String]
)
