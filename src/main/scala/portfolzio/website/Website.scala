package portfolzio.website

import portfolzio.model.AlbumEntry
import portfolzio.model.AlbumEntry.{Album, Image}
import portfolzio.util.*
import portfolzio.website.html.{Pages, Templates}
import portfolzio.{AppStateManager, WebsiteConfig}
import zio.*
import zio.http.*
import zio.http.html.Html
import zio.stream.ZStream

import java.nio.charset.StandardCharsets
import java.nio.file.{Path, Paths}

class Website(config: WebsiteConfig)(
    appStateManager: AppStateManager
):
  val previewPath: Path = Paths.get(config.previews.directory)
  private val cssPath = Paths.get("css")
  private val jsPath = Paths.get("js")
  private val imgPath = Paths.get("img")

  private val templates = Templates(config.title)
  private val pages = Pages(config.title, showLicense = config.licenseFile.isDefined)
  import templates.*
  import pages.*

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
    val concatenatedId = path.segments.drop(index).map(_.text).foldLeft("") { case (a, b) => s"$a/$b" }
    java.net.URLDecoder.decode(concatenatedId, StandardCharsets.UTF_8)

  object `/id`:
    def unapply(path: zio.http.Path): Option[(zio.http.Path, AlbumEntry.Id)] =
      Option.when(path.segments.length > 2)(path.take(2) -> AlbumEntry.Id.safe(albumEntryIdFromPath(path, index = 2)))

  val app: Http[Any, Throwable, Request, Response] =
    Http.collectZIO[Request]:
      case Method.GET -> Root | Method.GET -> Root / "recent" =>
        appStateManager.getState.map(state =>
          Response.html(
            pageWithNavigation(
              imageGrid(
                state.albumEntries.values.collect[Image] {
                  case img: Image => img
                }.toList.sortBy(_.info.time).reverse.take(18)
              )
            )
          )
        )

      case Method.GET -> Root / "albums" =>
        appStateManager.getState.map(state =>
          Response.html(
            pageWithNavigation(
              albumView(state)(
                rootAlbum = None,
                state.orphans.collect[Album] {
                  case alb: Album => alb
                }.toList.sortBy(_.name),
              )
            )
          )
        )

      case Method.GET -> Root / "tags" =>
        appStateManager.getState.map(state =>
          Response.html(
            tagsPage(
              state.albumEntries.collect {
                case (_, img: Image) => img.info.tags.getOrElse(List.empty)
              }.flatten.toSet.toList.sorted
            )
          )
        )

      case Method.GET -> Root / "image" `/id` imageId =>
        appStateManager.getState.map(state =>
          state.albumEntries.get(imageId) match
            case Some(image: Image) => Response.html(imagePage(image))
            case _                  => Response.html(notFoundPage(s"Image $imageId not found"), Status.NotFound)
        )

      case Method.GET -> Root / "album" `/id` albumId =>
        appStateManager.getState.map(state =>
          val (status: Status, content: Html) = state.albumEntries.get(albumId) match
            case Some(album: Album) => state.children.get(album.id) match {
              case None           =>
                Status.NotFound -> pageWithNavigation(html.h1(s"Album entries for $albumId not found! D:"))
              case Some(children) => Status.Ok -> pageWithNavigation(albumView(state)(Some(album), children))
            }
            case _                  => Status.NotFound -> pageWithNavigation(html.h1(s"Album $albumId not found! D:"))

          Response.html(content, status)
        )

      case Method.GET -> Root / "tag" / tag =>
        appStateManager.getState.map(state =>
          Response.html(tagPage(state)(java.net.URLDecoder.decode(tag, StandardCharsets.UTF_8)))
        )

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

      case Method.GET -> Root / "img" / file =>
        imgPath.safeResolve(Paths.get(file)).fold(
          ZIO.succeed(Response(Status.Forbidden))
        )(filePath =>
          if (file.endsWith(".png")) resourceResponse(filePath, MediaType.image.png)
          else if (file.endsWith(".jpg")) resourceResponse(filePath, MediaType.image.jpeg)
          else resourceResponse(filePath, MediaType.any)
        )

      case Method.GET -> Root / "preview" `/id` imageId =>
        val p = previewPath.safeResolve(Paths.get(imageId.value.stripPrefix("/")))
        p.fold(
          ZIO.succeed(Response(Status.Forbidden))
        )(filePath =>
          if (filePath.toFile.exists())
            fileResponse(filePath, MediaType.image.jpeg)
          else
            resourceResponse(imgPath.resolve("image-not-found.jpg"), MediaType.image.jpeg)
        )

      case Method.GET -> Root / "license" =>
        config.licenseFile.fold(ZIO.succeed(Response(Status.NotFound)))(licenseFile =>
          ZIO.readFile(licenseFile).map(licenseContents =>
            Response.html(licensePage(licenseContents))
          )
        )
