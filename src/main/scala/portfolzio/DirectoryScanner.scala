package portfolzio

import portfolzio.model.{AlbumEntry, ImageInfo}
import portfolzio.util.Regex.{AlbumRegex, ImageRegex, RawFileRegex}
import portfolzio.util.{DaemonRunner, Regex, Utf8Checker}
import zio.*
import zio.json.*
import zio.prelude.NonEmptyList
import zio.process.CommandError

import java.io.{File, FileFilter, IOException}

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

    // TODO:
    // only generate preview images for new files
    // add temp directory to dirscannerconfig
    // ask gpt how to scale images
    // lookup heic image support in web browsers since it's faster

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
    * @param pathPrefix    Will always start and end with '/'
    * @return all data (album entries) stored in the data directory
    */
  def findAlbumEntries(
    dataDirectory: String,
    pathPrefix: String = "/",
  ): UIO[List[AlbumEntry]] =
    def tryToResolveImage(
      filesInDir: Seq[File]
    ): Task[Option[AlbumEntry.Image]] =
      filesInDir.find(_.getName.contains("info.json") && pathPrefix != "/") match
        case None           => ZIO.succeed(None)
        case Some(infoFile) =>
          for
            info <- ZIO
              .readFile(infoFile.getPath)
              .flatMap(
                _.fromJson[ImageInfo].fold(
                  decodingError => ZIO.fail(RuntimeException(s"Failed to decode ${ infoFile.getPath } - $decodingError")),
                  ZIO.succeed(_),
                )
              )
            imageFiles = filesInDir.filter(file => file.getName.matches(ImageRegex.regex) && file.isFile)
            rawFiles = filesInDir.filter(file => file.getName.matches(RawFileRegex.regex) && file.isFile)
            image: Option[AlbumEntry.Image] = NonEmptyList
              .fromIterableOption(imageFiles)
              .map(imageFiles =>
                AlbumEntry.Image(
                  AlbumEntry.Id.unsafe(pathPrefix.dropRight(1)), // Remove trailing `/`
                  info,
                  imageFiles = imageFiles.map(pathPrefix + _.getName),
                  rawFiles = rawFiles.map(pathPrefix + _.getName).toList,
                )
              )
          yield image

    def parseAlbum(pathPrefix: String, file: File): UIO[Option[AlbumEntry.Album]] =
      Utf8Checker
        .checkFile(file.getPath)
        .flatMap {
          case false => ZIO.logWarning(s"Album file ${ file.getPath } is not UTF-8 encoded!").as(None)
          case true  =>
            ZIO
              .readFile(file.getPath)
              .flatMap(fileContents =>
                ZIO.succeed(
                  Some[AlbumEntry.Album](
                    AlbumEntry
                      .Album(
                        AlbumEntry.Id.safe(pathPrefix + file.getName.stripSuffix(".album")),
                        children = fileContents.split('\n').toVector,
                      )
                  )
                ),
              )
        }
        .catchAll((e: IOException) => ZIO.logError(s"Failed to open file ${ file.getPath } - ${ e.getMessage }").as(None))

    def resolveAlbums(filesInDir: Seq[File]): UIO[List[AlbumEntry]] =
      ZIO
        .collectAll(
          filesInDir
            .filter(file => file.isFile && file.getName.matches(AlbumRegex.regex))
            .map(file => parseAlbum(pathPrefix, file))
            .toList
        )
        .map(_.flatten)

    def searchInSubDirs(filesInDir: Seq[File]): UIO[List[AlbumEntry]] =
      ZIO
        .collectAll(
          filesInDir
            .filter(_.isDirectory)
            .map(dir => findAlbumEntries(dataDirectory, pathPrefix + dir.getName + "/"))
            .toList
        )
        .map(_.flatten)

    val dir = new File(dataDirectory + pathPrefix)
    (for
      filesInDir <- ZIO.attempt(dir.listFiles())
      subDirEntries <- searchInSubDirs(filesInDir)
      localImage <- tryToResolveImage(filesInDir)
      localAlbums <- resolveAlbums(filesInDir)
    yield localImage.toList ++ localAlbums ++ subDirEntries).catchAll(t =>
      ZIO
        .logWarning(s"WARN: Failed to scan directory ${ dir.getPath } - ${ t.getMessage }")
        .map(_ => List.empty[AlbumEntry])
    )
  end findAlbumEntries

  def make(
    config: DirectoryScannerConfig,
    appStateManager: AppStateManager,
  ): UIO[DirectoryScanner] = {
    def scan: Task[Unit] = for
      _ <- ZIO.log("Starting data directory scan")
      albums <- findAlbumEntries(config.directory)
      _ <- ZIO.log(
        "Directory scan finished. Generating previews for new images..."
      )
      // _ <- generatePreviews(albums)
      _ <- ZIO.log("Preview generation finished.")
    yield ()

    DaemonRunner
      .make(scan)
      .map(scanRunner => DirectoryScannerImpl(config, appStateManager, scanRunner))
  }
