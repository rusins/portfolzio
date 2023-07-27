import zio.*
import zio.http.*
import zio.config.*
import zio.config.typesafe.TypesafeConfigProvider

import java.io.IOException
object Main extends ZIOAppDefault:

  val getConfig = for
    args <- getArgs
    configFileName <- args.size match
      case 0 => ZIO.succeed("config.json")
      case 1 => ZIO.succeed(args.head)
      case _ => ZIO.fail(new RuntimeException(
        "Too many arguments passed! Please pass only a single (optional) config file to load."
      ))
    rawConfig <- ZIO.readFile(configFileName).mapError(ioErr => new IOException(s"Unable to load config file: ${ ioErr.getMessage }"))
    config <- TypesafeConfigProvider.fromHoconString(rawConfig).load(websiteConfig)
  yield config


  def run = getConfig.foldZIO(failure => Console.printLineError(failure.getMessage), config =>
    for
      _ <- Console.printLine(s"Starting server at http://localhost:${ config.port }")
      httpApps = Website(config) ++ FileDownloading()
      _ <- Server
        .serve(httpApps.withDefaultErrorResponse)
        .provide(
          Server.defaultWithPort(config.port)
        )
    yield ()
  )
