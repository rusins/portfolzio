package portfolzio

import zio.Config
import zio.config.*
import zio.config.magnolia.*

case class WebsiteConfig(
    port: Int,
    data: DirectoryScannerConfig,
    previews: PreviewConfig
)

/** @param directory path to the top level directory for albums
  * @param scanInterval seconds after which to rescan the data directory
  */
case class DirectoryScannerConfig(directory: String, scanInterval: Int)

/** @param size pixel length of the longest side of the image
  * @param quality 0 to 1.0 jpeg preview quality value
  */
case class PreviewConfig(size: Int, quality: Float)

lazy val websiteConfig: Config[WebsiteConfig] = deriveConfig[WebsiteConfig]
