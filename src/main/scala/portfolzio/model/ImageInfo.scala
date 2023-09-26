package portfolzio.model

import java.time.LocalDateTime

case class ImageInfo(
    time: Option[LocalDateTime],
    description: Option[String],
    cameraModel: Option[String],
    aperture: Option[String],
    focalLength: Option[String]
)
