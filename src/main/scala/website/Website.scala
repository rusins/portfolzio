import zio.http._
import zio.http.html.Html
import website.html.template
import zio.stream.ZStream

object Website:
  def apply(): Http[Any, Throwable, Request, Response] =
    Http.collect[Request]:
      case Method.GET -> Root =>
        Response.html(template(html.h1("Wassup! :D")))

      // TODO: These file retrieving routes fail in an ugly way if the file doesn't exist.
      case Method.GET -> Root / "css" / file =>
        Response(
          headers = Headers(
            Header.ContentType(MediaType.text.css),
            Header.ContentDisposition.attachment(file)
          ),
          body = Body.fromStream(ZStream.fromResource(s"css/$file"))
        )

      case Method.GET -> Root / "js" / file =>
        Response(
          headers = Headers(
            Header.ContentType(MediaType.text.javascript),
            Header.ContentDisposition.attachment(file)
          ),
          body = Body.fromStream(ZStream.fromResource(s"js/$file"))
        )
