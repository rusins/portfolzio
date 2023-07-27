import zio.Config
import zio.config.*
import zio.config.magnolia.*

case class WebsiteConfig(port: Int, dataDirectory: String, previewConfig: PreviewConfig)

case class PreviewConfig(size: Int, quality: Float)

lazy val websiteConfig: Config[WebsiteConfig] = deriveConfig[WebsiteConfig]