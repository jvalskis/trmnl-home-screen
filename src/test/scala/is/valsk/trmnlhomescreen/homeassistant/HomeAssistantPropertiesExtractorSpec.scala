package is.valsk.trmnlhomescreen.homeassistant

import is.valsk.trmnlhomescreen.PropertiesExtractor
import is.valsk.trmnlhomescreen.homeassistant.message.model.responses.Event.{EntityAttributes, EntityState}
import zio.*
import zio.test.*

import scala.jdk.CollectionConverters.*

object HomeAssistantPropertiesExtractorSpec extends ZIOSpecDefault:

  private val enabledConfig = HomeAssistantConfig(
    enabled = true,
    webSocketUrl = "ws://localhost:8123/api/websocket",
    accessToken = "test-token",
    subscribedEntityIds = "sensor.temp,sensor.humidity",
    maxFrameSizeKb = 1024,
  )

  private val testLayer =
    HomeAssistantStateRepository.layer ++ ZLayer.succeed(enabledConfig) >>>
      (HomeAssistantStateRepository.layer ++ HomeAssistantPropertiesExtractor.layer)

  private def withEntities(entities: Map[String, EntityState]) =
    for
      repo <- ZIO.service[HomeAssistantStateRepository]
      _ <- repo.updateEntityStates(entities)
      extractor <- ZIO.service[PropertiesExtractor]
      result <- extractor.extract
    yield result

  private def entity(
      entityId: String,
      state: String,
      friendlyName: Option[String] = None,
      unit: Option[String] = None,
  ): EntityState =
    EntityState(
      entityId = entityId,
      state = state,
      attributes = EntityAttributes(
        friendlyName = friendlyName,
        unitOfMeasurement = unit,
        deviceClass = None,
      ),
    )

  def spec = suite("HomeAssistantPropertiesExtractor")(
    test("includes homeassistant_enabled from config") {
      (for
        extractor <- ZIO.service[PropertiesExtractor]
        result <- extractor.extract
      yield assertTrue(result("homeassistant_enabled") == true))
        .provide(testLayer)
    },
    test("homeassistant_enabled is false when config disabled") {
      val disabledConfig = enabledConfig.copy(enabled = false)
      val layer = HomeAssistantStateRepository.layer ++ ZLayer.succeed(disabledConfig) >>>
        (HomeAssistantStateRepository.layer ++ HomeAssistantPropertiesExtractor.layer)
      (for
        extractor <- ZIO.service[PropertiesExtractor]
        result <- extractor.extract
      yield assertTrue(result("homeassistant_enabled") == false))
        .provide(layer)
    },
    test("returns empty entities when repository is empty") {
      (for
        extractor <- ZIO.service[PropertiesExtractor]
        result <- extractor.extract
        entities = result("entities").asInstanceOf[java.util.List[?]]
      yield assertTrue(entities.isEmpty))
        .provide(testLayer)
    },
    test("extracts entity_id and state") {
      val tempSensor = entity("sensor.temp", "21.5")
      withEntities(Map("sensor.temp" -> tempSensor))
        .map { result =>
          val entities = result("entities").asInstanceOf[java.util.List[(String, java.util.Map[String, Any])]]
          val (id, props) = entities.asScala.head
          assertTrue(
            id == "sensor.temp",
            props.get("entity_id") == "sensor.temp",
            props.get("state") == "21.5",
          )
        }
        .provide(testLayer)
    },
    test("uses friendly_name when available") {
      val tempSensor = entity("sensor.temp", "21.5", friendlyName = Some("Living Room Temperature"))
      withEntities(Map("sensor.temp" -> tempSensor))
        .map { result =>
          val entities = result("entities").asInstanceOf[java.util.List[(String, java.util.Map[String, Any])]]
          val (_, props) = entities.asScala.head
          assertTrue(props.get("friendly_name") == "Living Room Temperature")
        }
        .provide(testLayer)
    },
    test("falls back to entity_id when friendly_name is None") {
      val tempSensor = entity("sensor.temp", "21.5", friendlyName = None)
      withEntities(Map("sensor.temp" -> tempSensor))
        .map { result =>
          val entities = result("entities").asInstanceOf[java.util.List[(String, java.util.Map[String, Any])]]
          val (_, props) = entities.asScala.head
          assertTrue(props.get("friendly_name") == "sensor.temp")
        }
        .provide(testLayer)
    },
    test("extracts unit of measurement") {
      val tempSensor = entity("sensor.temp", "21.5", unit = Some("°C"))
      withEntities(Map("sensor.temp" -> tempSensor))
        .map { result =>
          val entities = result("entities").asInstanceOf[java.util.List[(String, java.util.Map[String, Any])]]
          val (_, props) = entities.asScala.head
          assertTrue(props.get("unit") == "°C")
        }
        .provide(testLayer)
    },
    test("unit defaults to empty string when None") {
      val tempSensor = entity("sensor.temp", "21.5", unit = None)
      withEntities(Map("sensor.temp" -> tempSensor))
        .map { result =>
          val entities = result("entities").asInstanceOf[java.util.List[(String, java.util.Map[String, Any])]]
          val (_, props) = entities.asScala.head
          assertTrue(props.get("unit") == "")
        }
        .provide(testLayer)
    },
    test("extracts multiple entities keyed by entity_id") {
      val entities = Map(
        "sensor.temp" -> entity("sensor.temp", "21.5", friendlyName = Some("Temperature"), unit = Some("°C")),
        "sensor.humidity" -> entity("sensor.humidity", "65", friendlyName = Some("Humidity"), unit = Some("%")),
      )
      withEntities(entities)
        .map { result =>
          val extracted = result("entities").asInstanceOf[java.util.List[(String, java.util.Map[String, Any])]]
          val byId = extracted.asScala.toMap
          assertTrue(
            byId.size == 2,
            byId("sensor.temp").get("state") == "21.5",
            byId("sensor.humidity").get("state") == "65",
          )
        }
        .provide(testLayer)
    },
  )
