package portfolzio

import portfolzio.Main.getArgs
import portfolzio.website.Website
import zio.config.typesafe.TypesafeConfigProvider
import zio.http.Server
import zio.logging.consoleLogger
import zio.{Console, Runtime, Scope, ZIO, ZIOAppArgs, ZIOAppDefault, ZLayer}

import java.io.IOException

//noinspection TypeAnnotation
object Main extends ZIOAppDefault:

  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] =
    Runtime.removeDefaultLoggers >>> consoleLogger()

  private val getConfig = for
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
        appStateManager <- AppStateManager.make
        httpApps = Website(config, appStateManager) ++ FileDownloading()
        webServer = Server
          .serve(httpApps.withDefaultErrorResponse)
          .provide(
            Server.defaultWithPort(config.port)
          )
        // directoryScanner = DirectoryScanner(config.data, appStateManager, )
        _ <- webServer
      yield (),
  )
