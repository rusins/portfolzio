package portfolzio

import org.apache.commons.imaging.common.ImageMetadata
import org.apache.commons.imaging.{ImageReadException, Imaging}
import portfolzio.model.AlbumEntry.IdSelector
import portfolzio.model.{AlbumEntry, ImageInfo}
import portfolzio.util.ExifHelpers.*
import portfolzio.util.Regex.{AlbumRegex, ImageRegex, RawFileRegex}
import portfolzio.util.{DaemonRunner, Regex, Utf8Checker}
import zio.json.*
import zio.prelude.NonEmptyList
import zio.process.CommandError
import zio.{process, *}

import java.io.{File, IOException}
import java.nio.file.{Path, Paths}

trait DirectoryScanner:
  /** Never-ending, blocking effect that monitors changes in the configured data directory,
    * triggers a directory scan if anything changes,
    * and updates the global app state when done.
    *
    * zio.process should take care of terminating the inotifywait process when the effect is stopped
    */
  val monitor: IO[CommandError, Unit]

  /** Trigger a scan manually */
  val run: UIO[Unit]

object DirectoryScanner:

  private val inotifySchedule =
    Schedule.spaced(1.minute) && Schedule.recurWhile[CommandError] {
      case _: CommandError.ProgramNotFound  => false
      case _: CommandError.PermissionDenied => false
      case _                                => true
    }

  def monitoringProcess(directory: Path, onChange: UIO[Unit]): IO[process.CommandError, Unit] =
    ZIO.scoped {
      for
        process <- ZIO.acquireRelease(
          zio.process.Command(
            "inotifywait",
            "--recursive", // sets up watches in subdirectories
            "--monitor", // watch for changes indefinitely
            "-e",
            "create", // watch for new file creation
            "-e",
            "modify", // watch for file modification
            "-e",
            "delete", // watch for file deletion
            s"${ directory.toString }",
          ).run
        )(process => ZIO.log("Killed directory monitoring process") *> process.kill.ignoreLogged)
        // The process is uninterruptible, so in order to kill it through an interruption, we must fork it to another
        // fiber, and have this effect loop forever so that the resource doesn't get closed.
        _ <- process.stdout.linesStream.tap(line => ZIO.logDebug(line) *> onChange).retry(inotifySchedule).runDrain.fork
        _ <- ZIO.never
      yield ()
    }

  private class DirectoryScannerImpl(
    config: DirectoryScannerConfig,
    appStateManager: AppStateManager,
    scanRunner: DaemonRunner,
  ) extends DirectoryScanner {

    val monitor: UIO[Unit] = monitoringProcess(Paths.get(config.directory), scanRunner.run).fork.as(())

    val run: UIO[Unit] = scanRunner.run

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
          val imageId = AlbumEntry.Id.unsafe(pathPrefix.stripSuffix("/"))
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
            image: Option[AlbumEntry.Image] <- NonEmptyList.fromIterableOption(imageFiles).fold(
              ZIO.logWarning(s"No image files found for $imageId").as(None)
            )(imageFilesUnsorted =>
              val imageFilesSorted = NonEmptyList.fromIterableOption(imageFilesUnsorted
                .map(file => info.primaryImageFile.contains(file.getName) -> file)
                .sorted.reverse.map(_._2)).get
              for
                exifInfo: Option[ImageMetadata] <- ZIO.attemptBlockingIOUnsafe { _ =>
                  try
                    Right(Imaging.getMetadata(imageFiles.head))
                  catch
                    case e: ImageReadException => Left(e.getMessage)
                }.flatMap {
                  case Left(error)     => ZIO.logWarning(s"Failed to read Exif metadata for $imageId - $error").as(None)
                  case Right(metadata) => ZIO.succeed(Some(metadata))
                }
                infoWithExif = exifInfo.fold(info)(metadata => info.populateWithExifMetadata(metadata))
                image: AlbumEntry.Image = AlbumEntry.Image(
                  imageId,
                  infoWithExif,
                  imageFiles = imageFilesSorted.map(file => Paths.get(pathPrefix.stripPrefix("/") + file.getName)),
                  rawFiles =
                    rawFiles.toList.sortBy(_.getName).reverse.map(file =>
                      Paths.get(pathPrefix.stripPrefix("/") + file.getName)
                    ),
                )
              yield Some(image)
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
                        childSelectors = fileContents.split('\n').map(IdSelector.fromString).toList,
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
      filesInDir <- ZIO.attempt(dir.listFiles().toIndexedSeq)
      subDirEntries <- searchInSubDirs(filesInDir)
      localImage <- tryToResolveImage(filesInDir)
      localAlbums <- resolveAlbums(filesInDir)
    yield localImage.toList ++ localAlbums ++ subDirEntries).catchAll(t =>
      ZIO
        .logWarning(s"WARN: Failed to scan directory ${ dir.getPath } - ${ t.getMessage }")
        .map(_ => List.empty[AlbumEntry])
    )
  end findAlbumEntries

  def make(config: DirectoryScannerConfig)(appStateManager: AppStateManager): UIO[DirectoryScanner] = {
    def scan(initialScan: Ref[Boolean]): Task[Unit] =
      for
        initialScan <- initialScan.getAndSet(false)
        _ <- ZIO.unless(initialScan)(ZIO.sleep(config.minScanInterval.seconds)) // Add delay for file writes to complete
        _ <- ZIO.log("Starting data directory scan")
        albumEntries <- findAlbumEntries(config.directory)
        _ <- ZIO.log("Directory scan finished.")
        newAppState <- AppState.fromRawEntries(albumEntries)
        _ <- appStateManager.setState(newAppState)
      yield ()

    Ref.make(true).flatMap(initialScan =>
      DaemonRunner
        .make(scan(initialScan))
        .map(scanRunner => DirectoryScannerImpl(config, appStateManager, scanRunner))
    )
  }
