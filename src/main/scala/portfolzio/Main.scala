package portfolzio

import portfolzio.Main.getArgs
import portfolzio.website.Website
import zio.config.typesafe.TypesafeConfigProvider
import zio.http.Server
import zio.logging.consoleLogger
import zio.{Console, Runtime, Scope, ZIO, ZIOAppArgs, ZIOAppDefault, ZLayer}

import java.io.IOException
import java.nio.file.{Path, Paths}

//noinspection TypeAnnotation
object Main extends ZIOAppDefault:

  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] =
    Runtime.removeDefaultLoggers >>> consoleLogger()

  private val getConfig =
    for
      args <- getArgs
      configFileName <- args.size match
        case 0 => ZIO.succeed("config.json")
        case 1 => ZIO.succeed(args.head)
        case _ =>
          ZIO.fail(
            new RuntimeException(
              "Too many arguments passed! Please pass only a single (optional) config file to load."
            )
          )
      rawConfig <- ZIO
        .readFile(configFileName)
        .mapError(ioErr => new IOException(s"Unable to load config file: ${ ioErr.getMessage }"))
      config <- TypesafeConfigProvider
        .fromHoconString(rawConfig)
        .load(websiteConfig)
    yield config

  def run = getConfig.foldZIO(
    failure => ZIO.logError(failure.getMessage),
    config =>
      for
        _ <- ZIO.log(
          s"Starting server at http://localhost:${ config.port }"
        )
        _ <- ZIO.log(s"Data directory: ${ config.data.directory }")
        _ <- ZIO.log(s"Preview directory: ${ config.previews.directory }")
        appStateManager <- AppStateManager.make
        httpApps = Website(config)(appStateManager).http ++ FileDownloading()
        webServer = Server
          .serve(httpApps.withDefaultErrorResponse)
          .provide(
            Server.defaultWithPort(config.port)
          )
        directoryScanner <- DirectoryScanner.make(config.data)(appStateManager)
        previewGenerator <- PreviewGenerator.make(config.previews, Paths.get(config.data.directory))(appStateManager)
        _ <- appStateManager.subscribeToUpdates("PreviewGenerator", previewGenerator.run)
        _ <- directoryScanner.run // Trigger initial data directory scan
        _ <- webServer zipPar directoryScanner.monitor
      yield (),
  )
