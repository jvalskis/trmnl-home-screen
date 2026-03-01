package is.valsk.trmnlhomescreen

import liqp.TemplateParser
import zio.*
import zio.test.*
import zio.test.Assertion.*

import scala.jdk.CollectionConverters.*

object ScreenRendererSpec extends ZIOSpecDefault:

  private def rendererFromTemplate(templateString: String): ScreenRenderer =
    val parser = new TemplateParser.Builder().build()
    val template = parser.parse(templateString)
    new ScreenRenderer:
      def render(properties: Map[String, Any]): Task[String] =
        ZIO.attempt(template.render(properties.asJava))

  def spec = suite("ScreenRenderer")(
    test("renders simple variables") {
      val renderer = rendererFromTemplate("Hello {{ name }}!")
      for
        result <- renderer.render(Map("name" -> "World"))
      yield assertTrue(result == "Hello World!")
    },
    test("renders empty properties without errors") {
      val renderer = rendererFromTemplate("Static content")
      for
        result <- renderer.render(Map.empty)
      yield assertTrue(result == "Static content")
    },
    test("renders missing variables as empty strings") {
      val renderer = rendererFromTemplate("Hello {{ name }}!")
      for
        result <- renderer.render(Map.empty)
      yield assertTrue(result == "Hello !")
    },
    test("renders conditional blocks - true") {
      val renderer = rendererFromTemplate("{% if show %}visible{% endif %}")
      for
        result <- renderer.render(Map("show" -> true))
      yield assertTrue(result == "visible")
    },
    test("renders conditional blocks - false") {
      val renderer = rendererFromTemplate("{% if show %}visible{% endif %}")
      for
        result <- renderer.render(Map("show" -> false))
      yield assertTrue(result == "")
    },
    test("renders iterable collections") {
      val renderer = rendererFromTemplate("{% for event in events %}{{ event.summary }},{% endfor %}")
      val events = List(
        Map[String, Any]("summary" -> "Meeting").asJava,
        Map[String, Any]("summary" -> "Lunch").asJava,
      ).asJava
      for
        result <- renderer.render(Map("events" -> events))
      yield assertTrue(result == "Meeting,Lunch,")
    },
    test("renders nested map access") {
      val renderer = rendererFromTemplate("""{{ entities["sensor.temp"].state }}""")
      val entities = Map[String, Any](
        "sensor.temp" -> Map[String, Any]("state" -> "21.5").asJava,
      ).asJava
      for
        result <- renderer.render(Map("entities" -> entities))
      yield assertTrue(result == "21.5")
    },
    test("renders numeric values") {
      val renderer = rendererFromTemplate("{{ temp }}° {{ icon }}")
      for
        result <- renderer.render(Map("temp" -> 23.5, "icon" -> 1))
      yield assertTrue(result == "23.5° 1")
    },
    test("renders a realistic calendar + weather + entities template") {
      val template =
        """{% if calendar_enabled %}{% for event in events %}{{ event.summary }}{% endfor %}{% endif %}""" +
          """ {{ temp_metric_value }}°""" +
          """ {{ entities["sensor.temp"].state }}"""

      val renderer = rendererFromTemplate(template)
      val events = List(
        Map[String, Any]("summary" -> "Standup").asJava,
      ).asJava
      val entities = Map[String, Any](
        "sensor.temp" -> Map[String, Any]("state" -> "19.2").asJava,
      ).asJava

      for
        result <- renderer.render(Map(
          "calendar_enabled" -> true,
          "events" -> events,
          "temp_metric_value" -> 5.0,
          "entities" -> entities,
        ))
      yield assertTrue(result == "Standup 5.0° 19.2")
    },
    suite("layer")(
      test("creates renderer from template file") {
        for
          tempDir <- ZIO.attempt(java.nio.file.Files.createTempDirectory("screen-renderer-test"))
          templatePath = tempDir.resolve("test.liquid")
          _ <- ZIO.attempt(java.nio.file.Files.writeString(templatePath, "Hello {{ name }}!"))
          config = ScreenConfig(templateFile = templatePath.toString, renderIntervalSeconds = 60)
          renderer <- ZLayer.succeed(config) >>> ScreenRenderer.layer match
            case layer => layer.build.map(_.get[ScreenRenderer]).provideLayer(Scope.default)
          result <- renderer.render(Map("name" -> "ZIO"))
          _ <- ZIO.attempt {
            java.nio.file.Files.deleteIfExists(templatePath)
            java.nio.file.Files.deleteIfExists(tempDir)
          }
        yield assertTrue(result == "Hello ZIO!")
      },
      test("fails when template file does not exist") {
        val config = ScreenConfig(templateFile = "/nonexistent/template.liquid", renderIntervalSeconds = 60)
        val result = (ZLayer.succeed(config) >>> ScreenRenderer.layer).build.provideLayer(Scope.default)
        assertZIO(result.exit)(fails(anything))
      },
    ),
  )
