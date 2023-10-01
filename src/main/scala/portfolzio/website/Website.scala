package portfolzio.website

import portfolzio.website.html.template
import portfolzio.{AppStateManager, WebsiteConfig}
import zio.*
import zio.http.*
import zio.http.html.Html
import zio.stream.ZStream

import java.io.IOException

object Website:
  def apply(
    config: WebsiteConfig,
    appStateManager: AppStateManager
  ): Http[Any, IOException, Request, Response] =
    Http.collectZIO[Request]:
      case Method.GET -> Root =>
        appStateManager.getState.map(state =>
          Response.html(
            template(
              html.h1(s"Wassup! :D State: " + state.albumEntries.keySet.mkString(" "))
            )
          )
        )

      case Method.GET -> Root / "css" / file =>
        ZStream
          .fromResource(s"css/$file")
          .runCollect
          .map(chunk =>
            Response(
              headers = Headers(
                Header.ContentType(MediaType.text.css),
                Header.ContentDisposition.attachment(file)
              ),
              body = Body.fromChunk(chunk)
            )
          )

      case Method.GET -> Root / "js" / file =>
        ZStream
          .fromResource(s"js/$file")
          .runCollect
          .map(chunk =>
            Response(
              headers = Headers(
                Header.ContentType(MediaType.text.javascript),
                Header.ContentDisposition.attachment(file)
              ),
              body = Body.fromChunk(chunk)
            )
          )
