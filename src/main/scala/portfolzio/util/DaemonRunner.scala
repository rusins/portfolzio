package portfolzio.util

import zio.*

/** Helper to run a long running task at least once after every time someone requests it to be run. */
trait DaemonRunner:
  val run: URIO[Any, Unit]

object DaemonRunner:

  private case class State(running: Boolean, runAgain: Boolean)

  private class DaemonRunnerImpl(task: Task[Any], stateRef: Ref[State]) extends DaemonRunner:
    /** Loop and run the task as long as someone has requested it to runAgain */
    private val daemon: Task[Unit] = for
      _ <- stateRef.set(State(running = true, runAgain = false))
      _ <- task
      loop <- stateRef.modify(state =>
        (state.runAgain, state.copy(running = false))
      )
      _ <- if (loop) daemon else ZIO.succeed(())
    yield ()

    val run: URIO[Any, Unit] = for
      prevState <- stateRef.modify(state => (state, state.copy(runAgain = true)))
      _ <-
        if (!prevState.running && !prevState.runAgain)
        // We are the first party to request the task to run, so we should start the daemon
          daemon.forkDaemon
        else
          ZIO.succeed(())
    yield ()

  def make(task: Task[Any]): UIO[DaemonRunner] =
    Ref.make(State(running = false, runAgain = false))
      .map(state => new DaemonRunnerImpl(task, state))
