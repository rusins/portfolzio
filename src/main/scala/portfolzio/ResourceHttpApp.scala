package portfolzio

import zio.http.*
import zio.http.codec.PathCodec
import zio.stream.ZStream

import java.nio.charset.StandardCharsets
import java.nio.file.Paths

class ResourceHttpApp:
  val app: HttpApp[Any] = Routes(
  case Method.GET / "license" ->
    config.licenseFile.fold(ZIO.succeed(Response(Status.NotFound)))(licenseFile =>
      ZIO.readFile(licenseFile).map(licenseContents =>
        Response.html(licensePage(licenseContents))
      )
    )

  case Method.GET / "robots.txt" ->
    resourceResponse(Paths.get("robots.txt"), MediaType.text.plain)
    ).toHttpApp
      @@ Middleware.serveResources(Path.empty / "js")
      @@ Middleware.serveResources(Path.empty / "css")
      @@ Middleware.serveResources(Path.empty / "img")
