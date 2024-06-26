package portfolzio.website

import portfolzio.model.AlbumEntry
import portfolzio.model.AlbumEntry.{Album, Image}
import portfolzio.util.*
import portfolzio.website.SiteMap.genSiteMap
import portfolzio.website.html.CustomTags.OGTags
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

  private val templates = Templates(titleText = config.title, rootUrl = config.url)
  private val pages = Pages(titleText = config.title, showLicense = config.licenseFile.isDefined, rootUrl = config.url)
  import templates.*
  import pages.*

  private def notFoundResponse(text: String) = Response.html(notFoundPage(text), Status.NotFound)

  private def streamResponse(stream: ZStream[Any, Throwable, Byte], mediaType: MediaType) =
    stream
      .runCollect
      .map(chunk =>
        Response(
          headers = Headers(
            Header.ContentType(mediaType),
            Header.ContentDisposition.attachment,
            Header.CacheControl.MaxAge(7 * 24 * 60 * 60), // Cache for 1 week
          ),
          body = Body.fromChunk(chunk),
        )
      ).tapError(e => ZIO.logError(e.toString))

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
      case Method.GET -> Root =>
        appStateManager.getState.map(state =>
          Response.html(
            pageWithNavigation(config.url.map(rootUrl =>
              val bestPic =
                for
                  bestAlbum <- state.bestAlbum
                  children <- state.children.get(bestAlbum.id)
                  child <- children.headOption
                yield child.id
              OGTags(
                title = config.title,
                staticUrl = rootUrl,
                imageUrl = rootUrl + "/preview" + bestPic.getOrElse("unknown"),
              )
            ))(imageGrid(
              state.bestAlbum.flatMap(bestAlbum => state.children.get(bestAlbum.id)).fold(
                // Take 12 random photos from the entire collection
                state.images.sortBy(_ => Math.random()).take(12)
              )(bestAlbumEntries =>
                bestAlbumEntries.collect[Image] {
                  case img: Image => img
                }
              )
            ))
          )
        )

      case Method.GET -> Root / "random" =>
        appStateManager.getState.map(state =>
          Response.html(
            pageWithNavigation()(
              imageGrid(
                state.images.sortBy(_ => Math.random()).take(12)
              )
            )
          )
        )

      case Method.GET -> Root / "recent" =>
        appStateManager.getState.map(state =>
          Response.html(
            pageWithNavigation()(
              imageGrid(
                state.images.sortBy(_.info.time).reverse.take(12).sortBy(_ => Math.random())
              )
            )
          )
        )

      case Method.GET -> Root / "albums" =>
        appStateManager.getState.map(state =>
          Response.html(
            pageWithNavigation()(
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
              state.tags
            )
          )
        )

      case Method.GET -> Root / "image" `/id` imageId =>
        appStateManager.getState.map(state =>
          state.albumEntries.get(imageId) match
            case Some(image: Image) => Response.html(imagePage(image))
            case _ => notFoundResponse(s"Image $imageId not found")
        )

      case Method.GET -> Root / "album" `/id` albumId =>
        appStateManager.getState.map(state =>
          val (status: Status, content: Html) = state.albumEntries.get(albumId) match
            case Some(album: Album) => state.children.get(album.id) match {
              case None           =>
                Status.NotFound -> pageWithNavigation()(
                  html.h1(s"Album entries for $albumId not found! D:")
                )
              case Some(children) =>
                Status.Ok -> pageWithNavigation(config.url.map(rootUrl =>
                  OGTags(
                    title = album.name + " | " + config.title,
                    staticUrl = rootUrl + "/album" + albumId,
                    imageUrl = rootUrl + "/preview" + children.headOption.map(_.id).getOrElse("unknown"),
                  )
                ))(albumView(state)(Some(album), children))
            }
            case _                  => Status.NotFound -> pageWithNavigation()(html.h1(s"Album $albumId not found! D:"))

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
        val p = previewPath.safeResolve(Paths.get(imageId.value.stripPrefix("/") + ".jpg"))
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

      case Method.GET -> Root / "robots.txt" =>
        config.url match
          case None          => ZIO.succeed(notFoundResponse("Unable to generate robots.txt. Missing URL in config file."))
          case Some(baseUrl) => ZIO.succeed(Response.text(s"Sitemap: $baseUrl/sitemap.xml"))

      case Method.GET -> Root / "sitemap.xml" =>
        config.url match
          case None          => ZIO.succeed(notFoundResponse("Unable to generate sitemap. Missing URL in config file."))
          case Some(baseUrl) => appStateManager.getState.map(state =>
            Response.text(genSiteMap(baseUrl)(state))
          )
