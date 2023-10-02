package portfolzio

import portfolzio.model.{AlbumEntry, ImageInfo}
import zio.*
import zio.logging.LogFilter
import zio.prelude.NonEmptyList
import zio.test.Assertion.*
import zio.test.ZTestLogger.LogEntry
import zio.test.{test, *}

import java.io.File
import java.nio.file.{Files, Paths}

object DirectoryScannerSpec extends ZIOSpecDefault:

  private val DataDir = getClass.getResource("/example_data_dir").getPath

  override def spec = suite("DirectoryScannerSpec")(
    test("findAlbumEntries warns if info.json is malformed") {
      for
        entries <- DirectoryScanner.findAlbumEntries(DataDir)
        logOutput <- ZTestLogger.logOutput
      yield assert(logOutput)(
        exists(
          hasField("logLevel", (e: LogEntry) => e.logLevel, equalTo(LogLevel.Warning)) && hasField(
            "message",
            (e: LogEntry) => e.message(),
            containsString("info.json"),
          )
        )
      )
    },
    test("findAlbumEntries returns an image entry for and an album containing day-night") {
      val ExpectedImage = AlbumEntry.Image(
        AlbumEntry.Id.safe("day-night"),
        ImageInfo(description = Some("A composite photo of my backyard")),
        imageFiles = NonEmptyList(Paths.get("day-night/day-night.jpg")),
        rawFiles = List.empty,
      )
      val ExpectedAlbum = AlbumEntry.Album(
        AlbumEntry.Id.safe("random"),
        children = Vector("day-night"),
      )
      for entries <- DirectoryScanner.findAlbumEntries(DataDir)
        yield assert(entries)(contains(ExpectedImage)) && assert(entries)(contains(ExpectedAlbum))
    },
    test("monitoringProcess detects all changes within the data directory") {
      val smallPause = ZIO.succeed(Thread.sleep(100L))
      val tempDir = Files.createTempDirectory("data")
      for
        changeDetected <- Ref.make(false)
        process <- DirectoryScanner.monitoringProcess(tempDir, changeDetected.set(true)).fork
        _ <- smallPause
        dir1 = tempDir.resolve("dir1")
        _ <- ZIO.attempt(Files.createDirectory(dir1))
        _ <- smallPause
        update1 <- changeDetected.getAndSet(false)

        dir2 = dir1.resolve("dir2")
        _ <- ZIO.attempt(Files.createDirectory(dir2))
        _ <- smallPause
        update2 <- changeDetected.getAndSet(false)

        file = dir2.resolve("file")
        _ <- ZIO.attempt(Files.createFile(file))
        _ <- smallPause
        update3 <- changeDetected.getAndSet(false)

        _ <- (zio.process.Command("echo", "Hello") > File(file.toString)).exitCode
        _ <- smallPause
        update4 <- changeDetected.getAndSet(false)

        _ <- ZIO.attempt(Files.delete(file))
        _ <- smallPause
        update5 <- changeDetected.getAndSet(false)

        _ <- ZIO.attempt(Files.delete(dir2))
        _ <- smallPause
        update6 <- changeDetected.getAndSet(false)
      yield assertTrue(update1) && assertTrue(update2) && assertTrue(update3) && assertTrue(update4) &&
        assertTrue(update5) && assertTrue(update6)
    },
  ).provideLayer(
    Runtime.removeDefaultLoggers >>>
      ZTestLogger.default
  )
