package portfolzio.website

import portfolzio.model.AlbumEntry
import portfolzio.model.AlbumEntry.Image
import portfolzio.util.*
import portfolzio.website.html.{albumView, headerTemplate, imageGrid, navigationTemplate}
import portfolzio.{AppStateManager, WebsiteConfig}
import zio.*
import zio.http.*
import zio.http.Path.Segment
import zio.http.html.Html
import zio.stream.ZStream

import java.io.IOException
import java.nio.file.{Path, Paths}

class Website(config: WebsiteConfig)(
  appStateManager: AppStateManager
):
  val previewPath: Path = Paths.get(config.previews.directory)
  private val cssPath = Paths.get("css")
  private val jsPath = Paths.get("js")

  private def streamResponse(stream: ZStream[Any, Throwable, Byte], mediaType: MediaType) =
    stream
      .runCollect
      .map(chunk =>
        Response(
          headers = Headers(
            Header.ContentType(mediaType),
            Header.ContentDisposition.attachment,
          ),
          body = Body.fromChunk(chunk),
        )
      ).tapError(e => ZIO.logError(e.getMessage))

  private def fileResponse(filePath: Path, mediaType: MediaType) =
    streamResponse(ZStream.fromFile(filePath.toFile), mediaType)

  private def resourceResponse(filePath: Path, mediaType: MediaType) =
    streamResponse(ZStream.fromResource(filePath.toString), mediaType)

  private def albumEntryIdFromPath(path: zio.http.Path, index: Int) =
    path.segments.drop(index).map(_.text).foldLeft("") { case (a, b) => s"$a/$b" }

  object `/id`:
    def unapply(path: zio.http.Path): Option[(zio.http.Path, AlbumEntry.Id)] =
      Option.when(path.segments.length > 2)((path.take(2) -> AlbumEntry.Id.safe(albumEntryIdFromPath(path, index = 2))))

  def http: Http[Any, Throwable, Request, Response] =
    Http.collectZIO[Request]:
      case Method.GET -> Root | Method.GET -> Root / "recent" =>
        appStateManager.getState.map(state =>
          Response.html(
            navigationTemplate(
              imageGrid(
                state.albumEntries.values.collect {
                  case img: Image => img
                }.take(18).toList
              )
            )
          )
        )

      case Method.GET -> Root / "albums" =>
        appStateManager.getState.map(state =>
          Response.html(
            navigationTemplate(
              albumView(
                List.empty
              )
            )
          )
        )

      case Method.GET -> Root / "image" `/id` imageId =>
        ZIO.succeed(Response.html(
          headerTemplate(html.h1(imageId.toString), html.img(html.srcAttr := "/preview" + imageId))
        ))

      case Method.GET -> Root / "css" / file =>
        cssPath.safeResolve(Paths.get(file)).fold(
          ZIO.succeed(Response(Status.Forbidden))
        )(filePath =>
          resourceResponse(filePath, MediaType.text.css)
        )

      case Method.GET -> Root / "js" / file =>
        jsPath.safeResolve(Paths.get(file)).fold(
          ZIO.succeed(Response(Status.Forbidden))
        )(filePath =>
          resourceResponse(filePath, MediaType.text.javascript)
        )

      case Method.GET -> Root / "preview" `/id` imageId =>
        val p = previewPath.safeResolve(Paths.get(imageId.toString.stripPrefix("/")))
        p.fold(
          ZIO.succeed(Response(Status.Forbidden))
        )(filePath =>
          fileResponse(filePath, MediaType.image.jpeg)
        )
