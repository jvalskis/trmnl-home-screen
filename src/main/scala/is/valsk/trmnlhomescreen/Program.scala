package is.valsk.trmnlhomescreen

import zio.{Task, ZIO}

trait Program {

  def run: Task[Unit]

  def runIfEnabled[T](
      enabled: Boolean,
      disabledMessage: String,
  )(
      program: ZIO[T, Throwable, Unit],
  ): ZIO[T, Throwable, Unit] = if enabled then program else ZIO.logInfo(disabledMessage) *> ZIO.never

}
