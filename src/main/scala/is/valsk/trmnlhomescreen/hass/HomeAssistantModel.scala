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

