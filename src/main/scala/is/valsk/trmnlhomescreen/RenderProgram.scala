package is.valsk.trmnlhomescreen

import zio.*

trait RenderProgram extends Program

object RenderProgram:

  private class RenderProgramLive(
      config: ScreenConfig,
      screenStateRepository: ScreenStateRepository,
      screenRenderer: ScreenRenderer,
  ) extends RenderProgram:

    def run: Task[Unit] =
      val interval = Duration.fromSeconds(config.renderIntervalSeconds.toLong)
      val loop = for
        state <- screenStateRepository.get
        rendered <- screenRenderer.render(state)
        _ <- Console.printLine(rendered)
      yield ()
      loop
        .catchAll(e => ZIO.logError(s"Failed to render screen: ${e.getMessage}"))
        .repeat(Schedule.fixed(interval))
        .unit

  val layer: URLayer[ScreenConfig & ScreenStateRepository & ScreenRenderer, RenderProgram] = ZLayer {
    for
      config <- ZIO.service[ScreenConfig]
      repo <- ZIO.service[ScreenStateRepository]
      renderer <- ZIO.service[ScreenRenderer]
    yield RenderProgramLive(config, repo, renderer)
  }

  val configuredLayer: RLayer[ScreenStateRepository & ScreenRenderer, RenderProgram] = ScreenConfig.layer >>> layer
