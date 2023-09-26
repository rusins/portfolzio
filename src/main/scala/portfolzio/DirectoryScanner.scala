package portfolzio

import portfolzio.util.DaemonRunner
import zio.*
import zio.process.CommandError

trait DirectoryScanner:
  /**
    * Never-ending, blocking effect that monitors changes in the configured data directory,
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
        s"-r -m -e create -e modify -e delete ${ config.directory }"
      )
      .linesStream
      .retry(inotifySchedule)
      .tap(_ => scanRunner.run)
      .runDrain

  }


  def make(config: DirectoryScannerConfig,
    appStateManager: AppStateManager,
  ): IO[Nothing, DirectoryScanner] = {
    def scan: Task[Unit] = ???

    DaemonRunner.make(scan).map(scanRunner => DirectoryScannerImpl(config, appStateManager, scanRunner))
  }