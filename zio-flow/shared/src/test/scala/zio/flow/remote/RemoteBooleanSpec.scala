package zio.flow.remote

import zio.ZIO
import zio.flow.utils.RemoteAssertionSyntax.RemoteAssertionOps
import zio.flow.{Remote, RemoteContext}
import zio.test.TestResult

object RemoteBooleanSpec extends RemoteSpecBase {
  override def spec =
    suite("RemoteBooleanSpec")(
      test("And") {
        ZIO
          .collectAll(
            List(
              (Remote(true) && Remote(false)) <-> false,
              (Remote(true) && Remote(true)) <-> true,
              (Remote(false) && Remote(false)) <-> false
            )
          )
          .map(TestResult.all(_: _*))
      },
      test("Or") {
        ZIO
          .collectAll(
            List(
              (Remote(true) || Remote(false)) <-> true,
              (Remote(true) || Remote(true)) <-> true,
              (Remote(false) || Remote(false)) <-> false
            )
          )
          .map(TestResult.all(_: _*))
      },
      test("Not") {
        ZIO
          .collectAll(
            List(
              !Remote(true) <-> false,
              !Remote(false) <-> true
            )
          )
          .map(TestResult.all(_: _*))
      },
      test("IfThenElse") {
        ZIO
          .collectAll(
            List(
              Remote(false).ifThenElse(Remote(1), Remote(12)) <-> 12,
              Remote(true).ifThenElse(Remote(1), Remote(12)) <-> 1
            )
          )
          .map(TestResult.all(_: _*))
      }
    ).provide(RemoteContext.inMemory)

}
