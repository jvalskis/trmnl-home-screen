# TRMNL Home Screen

A self-hosted dashboard for [TRMNL](https://usetrmnl.com/) e-ink displays. Aggregates weather, calendar, and Home Assistant data, renders it via a Liquid template, and pushes it to your own [BYOS](https://docs.usetrmnl.com/go/diy/byos) server — no cloud services or third-party data sharing required.

All data flows directly between your infrastructure and the APIs you choose to enable. Each integration can be independently toggled on or off.

## Integrations

### Weather (AccuWeather)

Polls AccuWeather for current conditions on a configurable interval. Provides temperature (metric/imperial), precipitation status, and observation time to the template.

### Calendar (CalDAV)

Connects to any CalDAV-compatible server (Nextcloud, Radicale, Baikal, etc.) to fetch upcoming events. Supports Basic and Bearer authentication. You control which calendar server your data lives on.

### Home Assistant

Subscribes to entity state changes over WebSocket with automatic reconnection. Filter which entities appear on screen via a comma-separated ID list. Connects directly to your Home Assistant instance on your local network.

## Configuration

All configuration is driven by environment variables with sensible defaults in `application.conf`.

### Screen

| Variable | Default | Description |
|---|---|---|
| `SCREEN_TEMPLATE_FILE` | `screen.liquid` | Path to Liquid template |
| `SCREEN_RENDER_INTERVAL_SECONDS` | `30` | How often to render and push |

### TRMNL Device

| Variable | Default | Description |
|---|---|---|
| `TRMNL_BASE_URL` | `http://localhost` | BYOS server URL |
| `TRMNL_API_KEY` | | Device API key |
| `TRMNL_MAC_ADDRESS` | | Device MAC address |

### Weather

| Variable | Default | Description |
|---|---|---|
| `WEATHER_ENABLED` | `true` | Enable/disable weather |
| `ACCUWEATHER_API_KEY` | | AccuWeather API key |
| `WEATHER_CITY` | `London` | City name for lookup |
| `WEATHER_FETCH_INTERVAL_MINUTES` | `60` | Poll interval |

### Calendar

| Variable | Default | Description |
|---|---|---|
| `CALENDAR_ENABLED` | `true` | Enable/disable calendar |
| `CALDAV_CALENDAR_URL` | | CalDAV endpoint URL |
| `CALDAV_AUTH_TYPE` | `basic` | `basic` or `bearer` |
| `CALDAV_USERNAME` | | CalDAV username |
| `CALDAV_PASSWORD` | | CalDAV password or token |
| `CALDAV_FETCH_INTERVAL_MINUTES` | `30` | Poll interval |
| `CALDAV_DAYS_AHEAD` | `7` | Days of events to fetch |

### Home Assistant

| Variable | Default | Description                                                                                                                                                |
|---|---|------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `HOME_ASSISTANT_ENABLED` | `false` | Enable/disable HA                                                                                                                                          |
| `HOME_ASSISTANT_WEBSOCKET_URL` | | WebSocket endpoint                                                                                                                                         |
| `HOME_ASSISTANT_ACCESS_TOKEN` | | Long-lived access token                                                                                                                                    |
| `HOME_ASSISTANT_SUBSCRIBED_ENTITY_IDS` | | Comma-separated entity IDs                                                                                                                                 |
| `HOME_ASSISTANT_MAX_FRAME_SIZE_KB` | `1024` | Max WebSocket frame size. Initial `get_state` call may returns very large payloads (It returns all entities) and this may need to be adjusted accordingly. |

## Running

### Docker Compose

Create a `.env` file with your secrets, then:

```sh
docker compose up -d
```

### Building from source

Requires JDK 21+ and sbt:

```sh
sbt assembly
java -jar target/scala-3.3.7/trmnl-home-screen-app.jar
```

## Templating

The screen layout is defined by a [Liquid](https://shopify.github.io/liquid/) template (`screen.liquid`). The rendered HTML is pushed to the BYOS server, which converts it to an image for the e-ink display.

Available template variables:

- **Weather:** `has_weather`, `weather_text`, `temp_metric_value`, `temp_metric_unit`, `temp_imperial_value`, `temp_imperial_unit`, `has_precipitation`, `is_day_time`, `observation_time`
- **Calendar:** `has_calendar`, `event_count`, `events[]` (each with `summary`, `start`, `end`, `location`, `description`)
- **Home Assistant:** `has_entities`, `entity_count`, `entities[]` (each with `entity_id`, `friendly_name`, `state`, `unit`)
