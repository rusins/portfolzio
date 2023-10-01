package portfolzio.model

import zio.json.{DeriveJsonDecoder, JsonDecoder}

import java.time.LocalDateTime

case class ImageInfo(
  time: Option[LocalDateTime] = None,
  description: Option[String] = None,
  cameraModel: Option[String] = None,
  aperture: Option[String] = None,
  focalLength: Option[String] = None,
  tags: Option[List[String]] = None,
)

object ImageInfo:
  implicit val decoder: JsonDecoder[ImageInfo] =
    DeriveJsonDecoder.gen[ImageInfo]
