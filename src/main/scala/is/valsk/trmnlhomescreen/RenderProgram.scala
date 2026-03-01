package is.valsk.trmnlhomescreen

import is.valsk.trmnlhomescreen.trmnl.TrmnlClient
import zio.*

trait RenderProgram extends Program

object RenderProgram:

  private class RenderProgramLive(
      config: ScreenConfig,
      extractors: List[PropertiesExtractor],
      screenRenderer: ScreenRenderer,
      trmnlClient: TrmnlClient,
  ) extends RenderProgram:

    def run: Task[Unit] =
      val interval = Duration.fromSeconds(config.renderIntervalSeconds.toLong)
      val loop = for
        allProperties <- ZIO.foreach(extractors)(_.extract)
        properties = allProperties.flatten.toMap
        rendered <- screenRenderer.render(properties)
        _ <- Console.printLine(rendered)
        _ <- trmnlClient.pushScreen(rendered)
      yield ()
      loop
        .catchAll(e => ZIO.logError(s"Failed to render screen: ${e.getMessage}"))
        .repeat(Schedule.fixed(interval))
        .unit

  val layer: URLayer[ScreenConfig & List[PropertiesExtractor] & ScreenRenderer & TrmnlClient, RenderProgram] = ZLayer {
    for
      config <- ZIO.service[ScreenConfig]
      extractors <- ZIO.service[List[PropertiesExtractor]]
      renderer <- ZIO.service[ScreenRenderer]
      trmnl <- ZIO.service[TrmnlClient]
    yield RenderProgramLive(config, extractors, renderer, trmnl)
  }

  val configuredLayer: RLayer[List[PropertiesExtractor] & ScreenRenderer & TrmnlClient, RenderProgram] =
    ScreenConfig.layer >>> layer
