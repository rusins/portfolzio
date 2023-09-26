package portfolzio.util

import zio._
import zio.test.{test, _}
import zio.test.Assertion._

object DaemonRunnerSpec extends ZIOSpecDefault {

  // TODO: This test suite is nondeterministic because we sleep to assert behavior of forked fibers.
  //       I don't know what the idiomatic way of testing this would be in Zio.
  //       Using schedules with retries would be really ugly I think.
  private val smallPause = ZIO.succeed(Thread.sleep(100L))

  override def spec = suite("DaemonRunnerSpec")(
    test("daemonRunner runs the task only once after being requested once") {
      for
        timesRun <- Ref.make(0)
        task = timesRun.update(_ + 1)
        dr <- DaemonRunner.make(task)
        _ <- smallPause
        timesRunBeforeRequested <- timesRun.get
        _ <- dr.run
        _ <- smallPause
        timesRunAfterRequested <- timesRun.get
      yield assertTrue(timesRunBeforeRequested == 0) &&
        assertTrue(timesRunAfterRequested == 1)
    },
    test(
      "daemonRunner runs the task only twice despite multiple requests happening during the first run"
    ) {
      for
        timesRun <- Ref.make(0)
        task = timesRun.update(_ + 1) *> ZIO.sleep(10.seconds)
        dr <- DaemonRunner.make(task)
        _ <- dr.run
        _ <- dr.run
        _ <- smallPause
        _ <- dr.run
        _ <- dr.run
        _ <- TestClock.adjust(10.seconds)
        _ <- smallPause
        assertion <- assertZIO(timesRun.get)(Assertion.equalTo(2))
      yield assertion
    },
    test("daemonRunner can be requested to run after it has stopped") {
      for
        timesRun <- Ref.make(0)
        task = timesRun.update(_ + 1)
        dr <- DaemonRunner.make(task)
        _ <- dr.run
        _ <- smallPause
        _ <- dr.run
        _ <- smallPause
        _ <- dr.run
        _ <- smallPause
        assertion <- assertZIO(timesRun.get)(Assertion.equalTo(3))
      yield assertion
    }
  )

}
