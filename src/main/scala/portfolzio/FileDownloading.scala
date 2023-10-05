package portfolzio

import zio.http.*
import zio.stream.ZStream

import java.nio.file.{Path, Paths}

class FileDownloading(config: WebsiteConfig):

  private val dataPath = Paths.get(config.data.directory)

  val app: Http[Any, Throwable, Request, Response] = Http.collectHttp[Request]:
    case Method.GET -> path if path.toString.startsWith("/download/") =>
      val filePath = dataPath.resolve(path.toString.stripPrefix("/download/"))
      Handler.fromStream(ZStream.fromPath(filePath)).toHttp
