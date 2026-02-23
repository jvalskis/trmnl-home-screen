package is.valsk.trmnlhomescreen.hass

import zio.json.*

// === REST API models (GET /api/states) ===

final case class EntityAttributes(
    @jsonField("friendly_name")
    friendlyName: Option[String],
    @jsonField("unit_of_measurement")
    unitOfMeasurement: Option[String],
    @jsonField("device_class")
    deviceClass: Option[String],
)

object EntityAttributes:
  given JsonDecoder[EntityAttributes] = DeriveJsonDecoder.gen[EntityAttributes]

final case class EntityState(
    @jsonField("entity_id")
    entityId: String,
    state: String,
    attributes: EntityAttributes,
)

object EntityState:
  given JsonDecoder[EntityState] = DeriveJsonDecoder.gen[EntityState]

// === WebSocket registry models ===

final case class HaArea(
    @jsonField("area_id")
    areaId: String,
    name: String,
)

object HaArea:
  given JsonDecoder[HaArea] = DeriveJsonDecoder.gen[HaArea]

final case class HaEntityRegistryEntry(
    @jsonField("entity_id")
    entityId: String,
    @jsonField("area_id")
    areaId: Option[String],
    @jsonField("device_id")
    deviceId: Option[String],
)

object HaEntityRegistryEntry:
  given JsonDecoder[HaEntityRegistryEntry] = DeriveJsonDecoder.gen[HaEntityRegistryEntry]

final case class HaDeviceRegistryEntry(
    id: String,
    @jsonField("area_id")
    areaId: Option[String],
)

object HaDeviceRegistryEntry:
  given JsonDecoder[HaDeviceRegistryEntry] = DeriveJsonDecoder.gen[HaDeviceRegistryEntry]

// === Grouped output for rendering ===

final case class EntityWithArea(
    entityId: String,
    friendlyName: String,
    state: String,
    unitOfMeasurement: String,
    areaName: String,
)

final case class AreaGroup(
    areaName: String,
    entities: List[EntityWithArea],
)
