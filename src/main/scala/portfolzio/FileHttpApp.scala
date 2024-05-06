package portfolzio

import zio.http.*
import zio.http.codec.PathCodec
import zio.stream.ZStream

import java.nio.charset.StandardCharsets
import java.nio.file.Paths

class FileHttpApp(config: WebsiteConfig):

  private val dataPath = Paths.get(config.data.directory)

  /** Serves image downloads from the data directory for paths starting with /download/ */
  val app: HttpApp[Any] = Routes.empty.toHttpApp @@ Middleware.serveDirectory(Path.empty / "download", dataPath.toFile)
