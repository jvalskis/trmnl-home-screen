package is.valsk.trmnlhomescreen.homeassistant

import zio.{Ref, UIO, ULayer, ZLayer}

final case class EntityRegistryEntry(areaId: Option[String], deviceId: Option[String])

trait HomeAssistantAreaRepository:
  def updateAreas(areas: Map[String, String]): UIO[Unit]
  def updateEntityRegistryEntries(entries: Map[String, EntityRegistryEntry]): UIO[Unit]
  def updateDeviceAreas(deviceAreas: Map[String, String]): UIO[Unit]
  def getAreaForEntity(entityId: String): UIO[Option[String]]

object HomeAssistantAreaRepository:

  private class Live(
      areasRef: Ref[Map[String, String]],
      entityRegistryRef: Ref[Map[String, EntityRegistryEntry]],
      deviceAreasRef: Ref[Map[String, String]],
  ) extends HomeAssistantAreaRepository:

    def updateAreas(areas: Map[String, String]): UIO[Unit] =
      areasRef.set(areas)

    def updateEntityRegistryEntries(entries: Map[String, EntityRegistryEntry]): UIO[Unit] =
      entityRegistryRef.set(entries)

    def updateDeviceAreas(deviceAreas: Map[String, String]): UIO[Unit] =
      deviceAreasRef.set(deviceAreas)

    def getAreaForEntity(entityId: String): UIO[Option[String]] =
      for
        entityRegistry <- entityRegistryRef.get
        areas <- areasRef.get
        deviceAreas <- deviceAreasRef.get
      yield
        entityRegistry.get(entityId).flatMap { entry =>
          val areaId = entry.areaId
            .orElse(entry.deviceId.flatMap(deviceAreas.get))
          areaId.flatMap(areas.get)
        }

  val layer: ULayer[HomeAssistantAreaRepository] = ZLayer {
    for
      areasRef <- Ref.make(Map.empty[String, String])
      entityRegistryRef <- Ref.make(Map.empty[String, EntityRegistryEntry])
      deviceAreasRef <- Ref.make(Map.empty[String, String])
    yield Live(areasRef, entityRegistryRef, deviceAreasRef)
  }
