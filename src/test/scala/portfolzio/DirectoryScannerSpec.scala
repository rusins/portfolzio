package portfolzio

import portfolzio.model.{AlbumEntry, ImageInfo}
import zio.*
import zio.logging.LogFilter
import zio.prelude.NonEmptyList
import zio.test.Assertion.*
import zio.test.ZTestLogger.LogEntry
import zio.test.{test, *}

object DirectoryScannerSpec extends ZIOSpecDefault:

  val DataDir = getClass.getResource("/example_data_dir").getPath

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
        imageFiles = NonEmptyList("/day-night/day-night.jpg"),
        rawFiles = List.empty,
      )
      val ExpectedAlbum = AlbumEntry.Album(
        AlbumEntry.Id.safe("random"),
        children = Vector("day-night"),
      )
      for entries <- DirectoryScanner.findAlbumEntries(DataDir)
        yield assert(entries)(contains(ExpectedImage)) && assert(entries)(contains(ExpectedAlbum))
    },
  ).provideLayer(
    Runtime.removeDefaultLoggers >>>
      ZTestLogger.default
  )
