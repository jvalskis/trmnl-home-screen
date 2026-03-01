# TRMNL Home Screen

A self-hosted dashboard for [TRMNL](https://usetrmnl.com/) e-ink displays. Aggregates weather, calendar, and Home Assistant data, renders it via a Liquid template, and pushes it to your own [BYOS](https://docs.usetrmnl.com/go/diy/byos) server — no cloud services or third-party data sharing required.

All data flows directly between your infrastructure and the APIs you choose to enable. Each integration can be independently toggled on or off.

## Integrations

### General

Provides general-purpose properties such as the current date. These are always available and require no configuration.

### Weather (AccuWeather)

Polls AccuWeather for current conditions on a configurable interval. Provides temperature, precipitation status, observation time and others to the template.

### Calendar (CalDAV)

Connects to any CalDAV-compatible server (Nextcloud, Radicale, Baikal, etc.) to fetch upcoming events. Supports Basic and Bearer authentication.

### Home Assistant

Subscribes to entity state changes over WebSocket. Filter which entities appear on screen via a comma-separated ID list. Connects directly to your Home Assistant instance on your local network.

## Configuration

All configuration is defined in `application.conf`. Defaults and can be overridden with environment variables.

### Screen

| Variable | Default | Description |
|---|---|---|
| `SCREEN_TEMPLATE_FILE` | `/app/templates/screen.liquid` | Path to Liquid template |
| `SCREEN_RENDER_INTERVAL_SECONDS` | `30` | How often to render and push |

### TRMNL Device

| Variable | Default | Description |
|---|---|---|
| `TRMNL_BASE_URL` | | BYOS server URL |
| `TRMNL_TOKEN` | | Device API token |
| `TRMNL_DEVICE_ID` | | Device ID |

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
| `CALENDAR_ENABLED` | `false` | Enable/disable calendar |
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

### Logging

| Variable | Default | Description |
|---|---|---|
| `LOG_LEVEL` | `INFO` | Log level (`ALL`, `TRACE`, `DEBUG`, `INFO`, `WARN`, `ERROR`, `OFF`) |

## Running

### Docker Compose

Copy `.env.example` to `.env` and fill in your values, then:

```sh
docker compose up -d
```

### Helm

A Helm chart is available as an OCI package:

```sh
helm install trmnl-home-screen oci://ghcr.io/jvalskis/helm-charts/trmnl-home-screen-app
```

See [`values.yaml`](https://github.com/jvalskis/trmnl-home-screen/blob/master/charts/trmnl-home-screen-app/values.yaml) for configurable values.

### Building from source

Requires JDK 21+ and sbt:

```sh
sbt assembly
java -jar target/scala-3.3.7/trmnl-home-screen-app.jar
```

## Templating

The screen layout is defined by a [Liquid](https://shopify.github.io/liquid/) template (`screen.liquid`). The rendered HTML is pushed to the BYOS server, which converts it to an image for the e-ink display. Templates should use the [TRMNL Design System](https://trmnl.com/framework/docs) for layouts optimized for e-ink rendering.

Available template variables:

| Source | Variable | Type | Description |
|---|---|---|---|
| General | `today_date` | string | Current date (`yyyy-MM-dd`) |
| General | `today_day_of_week` | string | Day of the week (e.g. `Monday`) |
| General | `today_day_of_month` | number | Day of the month (e.g. `1`) |
| General | `today_month` | string | Month name (e.g. `March`) |
| General | `today_year` | number | Year (e.g. `2026`) |
| Weather | `has_weather` | boolean | Whether weather data is available |
| Weather | `weather_text` | string | Current conditions description |
| Weather | `temp_metric_value` | number | Temperature in metric |
| Weather | `temp_metric_unit` | string | Metric unit (e.g. `C`) |
| Weather | `temp_imperial_value` | number | Temperature in imperial |
| Weather | `temp_imperial_unit` | string | Imperial unit (e.g. `F`) |
| Weather | `has_precipitation` | boolean | Whether precipitation is occurring |
| Weather | `is_day_time` | boolean | Whether it is daytime |
| Weather | `observation_time` | string | Observation timestamp |
| Calendar | `has_calendar` | boolean | Whether calendar data is available |
| Calendar | `event_count` | number | Number of upcoming events |
| Calendar | `events[]` | array | List of events |
| Calendar | `events[].summary` | string | Event title |
| Calendar | `events[].start` | string | Start time (`yyyy-MM-dd HH:mm`) |
| Calendar | `events[].end` | string | End time (`yyyy-MM-dd HH:mm`), empty if none |
| Calendar | `events[].location` | string | Location, empty if none |
| Calendar | `events[].description` | string | Description, empty if none |
| Home Assistant | `has_entities` | boolean | Whether entity data is available |
| Home Assistant | `entity_count` | number | Number of entities |
| Home Assistant | `entities[]` | array | List of entities |
| Home Assistant | `entities[].entity_id` | string | Entity ID |
| Home Assistant | `entities[].friendly_name` | string | Display name |
| Home Assistant | `entities[].state` | string | Current state value |
| Home Assistant | `entities[].unit` | string | Unit of measurement, empty if none |
