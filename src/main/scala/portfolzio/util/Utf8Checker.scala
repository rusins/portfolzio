package portfolzio.util

import zio.*
import zio.stream.{ZPipeline, ZStream}

import java.io.IOException
import java.nio.file.{Files, Path}

object Utf8Checker {

  private final val BufferSize = 1024; // Check only first 1KB for efficiency

  /** Checks if file is UTF-8 encoded */
  def checkFile(path: String): IO[IOException, Boolean] = ZIO.scoped(for {
    inputStream <- ZIO.readFileInputStream(path)
    buffer <- inputStream.readN(BufferSize).catchAll {
      // None indicates that the input stream ended, so nothing was read
      case None              => ZIO.succeed(Chunk.empty[Byte])
      case Some(ioException) => ZIO.fail(ioException)
    }
    isUtf8 <- ZStream
      .fromChunk(buffer)
      .via(ZPipeline.utf8Decode)
      .runDrain
      .foldCause(_ => false, _ => true)
  } yield isUtf8)
}
