package is.valsk.trmnlhomescreen.homeassistant.protocol.handlers

import is.valsk.trmnlhomescreen.homeassistant.{EntityRegistryEntry as AreaRepoEntry, HomeAssistantAreaRepository}
import is.valsk.trmnlhomescreen.homeassistant.message.model.responses.{EntityRegistryEntry, Result}
import is.valsk.trmnlhomescreen.homeassistant.message.{RequestRepository, Type}
import zio.*
import zio.json.*

object EntityRegistryHandler:

  val layer: URLayer[RequestRepository & HomeAssistantAreaRepository, HomeAssistantResultHandler] =
    ZLayer {
      for
        areaRepository <- ZIO.service[HomeAssistantAreaRepository]
        requestRepository <- ZIO.service[RequestRepository]
      yield new HomeAssistantResultHandler(
        requestRepository = requestRepository,
        handler = { case (_, result: Result, _) =>
          val entries = result.result.toSeq
            .flatMap(_.toJson.fromJson[List[EntityRegistryEntry]].toOption)
            .flatten
            .map(entry => entry.entityId -> AreaRepoEntry(entry.areaId, entry.deviceId))
            .toMap
          areaRepository.updateEntityRegistryEntries(entries) *>
            ZIO.logInfo(s"Updated entity registry with ${entries.size} entries")
        },
        supportedType = Type.GetEntityRegistry,
      )
    }
