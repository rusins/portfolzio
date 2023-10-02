package portfolzio

import portfolzio.model.AlbumEntry.Image
import portfolzio.util.*
import zio.*
import zio.process.Command

import java.io.{File, IOException}
import java.nio.file.{Files, Path, Paths}
import scala.collection.immutable.HashMap

trait PreviewGenerator:
  /** Trigger a background task to fetch all images from current app state and generate previews for images with them missing or outdated */
  def run: UIO[Unit]

object PreviewGenerator:

  def make(
    config: PreviewConfig,
    dataDir: Path,
  )(appStateManager: AppStateManager): IO[IOException, PreviewGenerator] = {
    val root = Paths.get(config.directory)

    def generatePreviewsForAllImages: Task[Unit] =
      for
        _ <- ZIO.log("Generating preview images...")
        images <- appStateManager.getState.map(_.albumEntries.values.collect { case i: Image => i })
        _ <- ZIO.collectAll(images.map(generatePreviewFor))
        _ <- ZIO.log("Done generating preview images.")
      yield ()

    def generatePreviewFor(image: Image): Task[Unit] = {
      val imagePath = dataDir.resolve(image.imageFiles.head)
      val previewPath = root.resolve(image.id.relativePath)
      val hashPath = root.resolve(image.id.relativePath.toString + ".sha1")
      (for
        previewExists <- ZIO.attemptBlockingIO(Files.exists(previewPath))
        hashExists <- ZIO.attemptBlockingIO(Files.exists(hashPath))
        hashMatches <-
          ZIO.when(hashExists)(
            Command("sha1sum", "-c", hashPath.toString).exitCode.flatMap {
              case ExitCode.success => ZIO.succeed(true)
              case ExitCode.failure => ZIO.log(s"Hash changed for ${ imagePath.toString }").as(false)
            }
          ).map(_.getOrElse(false))
        _ <- ZIO.when(!previewExists || !hashMatches)(
          ZIO.attemptBlockingIO(Files.createDirectories(previewPath.getParent)) *>
            (Command("sha1sum", imagePath.toString) > new File(hashPath.toString)).requireSuccessLogErrors *>
            Command(
              "convert", // from imagemagick
              imagePath.toString,
              "-resize",
              s"${ config.size }x${ config.size }>", // specifies maximum dimensions, keeping aspect ratio intact
              "-auto-orient",
              "-quality",
              s"${ 100 * config.quality }",
              previewPath.toString,
            ).requireSuccessLogErrors *>
            ZIO.log(s"Generated preview image for $imagePath")
        )
      yield ()).catchAll(e => ZIO.logError(e.getMessage))
    }

    for
    // Create directory for previews if it does not exist already
      _ <- ZIO.attemptBlockingIO(Files.createDirectories(root))
      runner <- DaemonRunner.make(generatePreviewsForAllImages)
    yield new PreviewGenerator {
      override def run: UIO[Unit] = runner.run
    }
  }
