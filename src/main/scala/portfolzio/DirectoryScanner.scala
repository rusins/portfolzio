package portfolzio

import portfolzio.model.{AlbumEntry, ImageInfo}
import portfolzio.util.Regex.{ImageRegex, RawFileRegex}
import portfolzio.util.{DaemonRunner, Regex}
import zio.*
import zio.json.*
import zio.prelude.NonEmptyList
import zio.process.CommandError

import java.io.{File, FileFilter}

trait DirectoryScanner:
  /** Never-ending, blocking effect that monitors changes in the configured data directory,
    * and triggers a directory scan if anything changes, generates new preview images,
    * and updates the global app state when done.
    *
    * zio.process should take care of terminating the inotifywait process when the effect is stopped
    */
  val monitor: IO[CommandError, Unit]

object DirectoryScanner:

  private val inotifySchedule =
    Schedule.spaced(1.minute) && Schedule.recurWhile[CommandError] {
      case _: CommandError.ProgramNotFound  => false
      case _: CommandError.PermissionDenied => false
      case _                                => true
    }

  private class DirectoryScannerImpl(
      config: DirectoryScannerConfig,
      appStateManager: AppStateManager,
      scanRunner: DaemonRunner,
  ) extends DirectoryScanner {

    // only generate preview images for new files
    // add temp directory to dirscannerconfig
    // ask gpt how to scale images
    // lookup heic image support in web browsers since it's faster
    //
    /*
    def run = loop {
      log that refresh started
      scan for changes
        generate previews for new images
        create new app state, reading info from all files
      // later can consider updating state instead of creating from scratch
      set app appState
      log that we finished refresh
    }*/

    val monitor: IO[CommandError, Unit] = zio.process
      .Command(
        "inotifywait",
        s"-r -m -e create -e modify -e delete ${config.directory}",
      )
      .linesStream
      .retry(inotifySchedule)
      .tap(_ => scanRunner.run)
      .runDrain

  }

  /** @param dataDirectory path to the data directory on the filesystem
    * @param pathPrefix Will always start and end with '/'
    * @return all data (album entries) stored in the data directory
    */
  private def findAllAlbums(
      dataDirectory: String,
      pathPrefix: String = "/",
  ): UIO[List[AlbumEntry]] =
    val dir = new File(dataDirectory + pathPrefix)
    val files = dir.listFiles().map(f => f.getName -> f).toMap
    val currentImage =
      if (pathPrefix != "/" && files.contains("info.json"))
        val infoFilePath = dataDirectory + pathPrefix + "info.json"
        for
          info <- ZIO
            .readFile(infoFilePath)
            .flatMap(
              _.fromJson[ImageInfo].fold(
                decodingError =>
                  ZIO.fail(s"Failed to decode $infoFilePath - $decodingError"),
                ZIO.succeed(_),
              )
            )
          imageFiles = files.filter((name, file) =>
            name.matches(ImageRegex) && file.isFile
          )
          rawFiles = files
            .filter((name, file) => name.matches(RawFileRegex) && file.isFile)
          image = NonEmptyList
            .fromIterableOption(imageFiles.keys)
            .map(imageFiles =>
              AlbumEntry.Image(
                id = pathPrefix,
                info,
                imageFiles = imageFiles.map(pathPrefix + _),
                rawFiles = rawFiles.keys.map(pathPrefix + _).toList,
              )
            )
        yield image
      else ZIO.succeed(None)
      // val albums = files.values.filter(_.)
    ZIO.succeed(List.empty[AlbumEntry])

  def make(
      config: DirectoryScannerConfig,
      appStateManager: AppStateManager,
  ): IO[Nothing, DirectoryScanner] = {
    def scan: Task[Unit] = for
      _ <- Console.printLine("Starting data directory scan")
      albums <- findAllAlbums(config.directory)
      _ <- Console.printLine(
        "Directory scan finished. Generating previews for new images..."
      )
      // _ <- generatePreviews(albums)
      _ <- Console.printLine("Preview generation finished.")
    yield ()

    DaemonRunner
      .make(scan)
      .map(scanRunner =>
        DirectoryScannerImpl(config, appStateManager, scanRunner)
      )
  }
