package is.valsk.trmnlhomescreen.homeassistant.protocol.handlers

import is.valsk.trmnlhomescreen.homeassistant.HomeAssistantAreaRepository
import is.valsk.trmnlhomescreen.homeassistant.message.model.responses.{AreaRegistryEntry, Result}
import is.valsk.trmnlhomescreen.homeassistant.message.{RequestRepository, Type}
import zio.*
import zio.json.*

object AreaRegistryHandler:

  val layer: URLayer[RequestRepository & HomeAssistantAreaRepository, HomeAssistantResultHandler] =
    ZLayer {
      for
        areaRepository <- ZIO.service[HomeAssistantAreaRepository]
        requestRepository <- ZIO.service[RequestRepository]
      yield new HomeAssistantResultHandler(
        requestRepository = requestRepository,
        handler = { case (_, result: Result, _) =>
          val areas = result.result.toSeq
            .flatMap(_.toJson.fromJson[List[AreaRegistryEntry]].toOption)
            .flatten
            .map(entry => entry.areaId -> entry.name)
            .toMap
          areaRepository.updateAreas(areas) *>
            ZIO.logDebug(s"Updated area registry with ${areas.size} areas")
        },
        supportedType = Type.GetAreaRegistry,
      )
    }
