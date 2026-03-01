package is.valsk.trmnlhomescreen.homeassistant.message.model.responses

import zio.json.{DeriveJsonDecoder, JsonDecoder, jsonField}

final case class AreaRegistryEntry(
    @jsonField("area_id")
    areaId: String,
    name: String,
)

object AreaRegistryEntry:
  given JsonDecoder[AreaRegistryEntry] = DeriveJsonDecoder.gen[AreaRegistryEntry]

final case class EntityRegistryEntry(
    @jsonField("entity_id")
    entityId: String,
    @jsonField("area_id")
    areaId: Option[String],
    @jsonField("device_id")
    deviceId: Option[String],
)

object EntityRegistryEntry:
  given JsonDecoder[EntityRegistryEntry] = DeriveJsonDecoder.gen[EntityRegistryEntry]

final case class DeviceRegistryEntry(
    id: String,
    @jsonField("area_id")
    areaId: Option[String],
)

object DeviceRegistryEntry:
  given JsonDecoder[DeviceRegistryEntry] = DeriveJsonDecoder.gen[DeviceRegistryEntry]
