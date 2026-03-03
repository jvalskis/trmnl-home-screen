package is.valsk.trmnlhomescreen.homeassistant.protocol.handlers

import is.valsk.trmnlhomescreen.homeassistant.HomeAssistantAreaRepository
import is.valsk.trmnlhomescreen.homeassistant.message.model.responses.{DeviceRegistryEntry, Result}
import is.valsk.trmnlhomescreen.homeassistant.message.{RequestRepository, Type}
import zio.*
import zio.json.*

object DeviceRegistryHandler:

  val layer: URLayer[RequestRepository & HomeAssistantAreaRepository, HomeAssistantResultHandler] =
    ZLayer {
      for
        areaRepository <- ZIO.service[HomeAssistantAreaRepository]
        requestRepository <- ZIO.service[RequestRepository]
      yield new HomeAssistantResultHandler(
        requestRepository = requestRepository,
        handler = { case (_, result: Result, _) =>
          val deviceAreas = result.result.toSeq
            .flatMap(_.toJson.fromJson[List[DeviceRegistryEntry]].toOption)
            .flatten
            .collect { case entry if entry.areaId.isDefined => entry.id -> entry.areaId.get }
            .toMap
          areaRepository.updateDeviceAreas(deviceAreas) *>
            ZIO.logInfo(s"Updated device registry with ${deviceAreas.size} device-area mappings")
        },
        supportedType = Type.GetDeviceRegistry,
      )
    }
