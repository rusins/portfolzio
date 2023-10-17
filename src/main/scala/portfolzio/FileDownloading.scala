package portfolzio

import zio.http.*
import zio.stream.ZStream

import java.nio.charset.StandardCharsets
import java.nio.file.Paths

class FileDownloading(config: WebsiteConfig):

  private val dataPath = Paths.get(config.data.directory)

  val app: Http[Any, Throwable, Request, Response] = Http.collectHttp[Request]:
    case Method.GET -> path if path.toString.startsWith("/download/") =>
      val decodedPath = java.net.URLDecoder.decode(path.toString, StandardCharsets.UTF_8)
      val filePath = dataPath.resolve(decodedPath.stripPrefix("/download/"))
      Handler.fromStream(ZStream.fromPath(filePath)).toHttp
