package portfolzio.model

import zio.json.{DeriveJsonDecoder, JsonDecoder}

import java.time.LocalDateTime

case class ImageInfo(
    time: Option[LocalDateTime],
    description: Option[String],
    cameraModel: Option[String],
    aperture: Option[String],
    focalLength: Option[String],
    tags: List[String],
)

object ImageInfo:
  implicit val decoder: JsonDecoder[ImageInfo] =
    DeriveJsonDecoder.gen[ImageInfo]
