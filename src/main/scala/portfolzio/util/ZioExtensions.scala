package portfolzio.util

import zio.*
import zio.process.{Command, CommandError}

extension (c: Command)
  def requireSuccessLogErrors: IO[CommandError, ExitCode] =
    for
      process <- c.run
      _ <- process.stderr.linesStream.tap(errLine => ZIO.logError(errLine)).runDrain
      exitCode <- process.successfulExitCode
    yield exitCode
