package portfolzio

import zio.Config
import zio.config.*
import zio.config.magnolia.*

/** @param port        network port for the webserver to listen to requests from
  * @param title       website title displayed on the website
  * @param url         website domain, used for the URL in open graph
  * @param licenseFile optional path to a license file for the images hosted on the website
  */
case class WebsiteConfig(
  port: Int,
  title: String = "",
  url: Option[String],
  data: DirectoryScannerConfig,
  previews: PreviewConfig,
  licenseFile: Option[String],
)

/** @param directory       path to the top level directory for albums
  * @param minScanInterval minimum seconds to sleep before rescanning the data directory
  */
case class DirectoryScannerConfig(directory: String, minScanInterval: Int)

/** @param size      pixel length of the longest side of the image
  * @param quality   0 to 1.0 jpeg preview quality value
  * @param directory path to the directory to use for generating preview images. Will be created if absent.
  */
case class PreviewConfig(size: Int, quality: Float, directory: String = "/tmp/portfolzio-previews")

lazy val websiteConfig: Config[WebsiteConfig] = deriveConfig[WebsiteConfig]
