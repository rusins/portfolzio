import zio._
import zio.http._

object Main extends ZIOAppDefault:

  val httpApps = Website() ++ FileDownloading()

  val port = 8080

  def run =
    Console.printLine(s"Starting server at http://localhost:$port") *>
      Server
        .serve(httpApps.withDefaultErrorResponse)
        .provide(
          Server.defaultWithPort(port)
        )
