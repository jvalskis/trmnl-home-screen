# TRMNL Home Screen

A self-hosted dashboard for [TRMNL](https://usetrmnl.com/) e-ink displays. Aggregates weather, calendar, and Home Assistant data, renders it via a Liquid template, and pushes it to your own [BYOS](https://docs.usetrmnl.com/go/diy/byos) server — no cloud services or third-party data sharing required.

All data flows directly between your infrastructure and the APIs you choose to enable. Each integration can be independently toggled on or off.

## Integrations

### Weather (AccuWeather)

Polls AccuWeather for current conditions on a configurable interval. Provides temperature, precipitation status, observation time and others to the template.

### Weather (WeatherAPI)

An alternative weather provider using [WeatherAPI](https://www.weatherapi.com/). Provides current temperature, feels-like temperature, wind speed/direction, humidity, UV index, visibility and more. Can run alongside or instead of AccuWeather — each provider is independently toggled.

### Calendar (CalDAV)

Connects to any CalDAV-compatible server (Nextcloud, Radicale, Baikal, etc.) to fetch upcoming events. Supports Basic and Bearer authentication.

### Home Assistant

Subscribes to entity state changes over WebSocket. Filter which entities appear on screen via a comma-separated ID list. Connects directly to your Home Assistant instance on your local network.

## Configuration

All configuration is defined in `application.conf`. Defaults and can be overridden with environment variables.

### Screen

| Variable | Default                     | Description |
|---|-----------------------------|---|
| `SCREEN_TEMPLATE_FILE` | `/app/config/screen.liquid` | Path to Liquid template |
| `SCREEN_RENDER_INTERVAL_SECONDS` | `30`                        | How often to render and push |

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

### WeatherAPI

| Variable | Default | Description |
|---|---|---|
| `WEATHERAPI_ENABLED` | `false` | Enable/disable WeatherAPI |
| `WEATHERAPI_API_KEY` | | WeatherAPI API key |
| `WEATHERAPI_CITY` | `London` | City name for lookup |
| `WEATHERAPI_FETCH_INTERVAL_MINUTES` | `60` | Poll interval |

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
| AccuWeather | `accuweather.enabled` | boolean | Whether AccuWeather is enabled |
| AccuWeather | `accuweather.weather_text` | string | Current conditions description |
| AccuWeather | `accuweather.weather_icon` | number | Weather icon code |
| AccuWeather | `accuweather.temp_metric_value` | number | Temperature in metric |
| AccuWeather | `accuweather.temp_metric_unit` | string | Metric unit (e.g. `C`) |
| AccuWeather | `accuweather.temp_imperial_value` | number | Temperature in imperial |
| AccuWeather | `accuweather.temp_imperial_unit` | string | Imperial unit (e.g. `F`) |
| AccuWeather | `accuweather.has_precipitation` | boolean | Whether precipitation is occurring |
| AccuWeather | `accuweather.is_day_time` | boolean | Whether it is daytime |
| AccuWeather | `accuweather.observation_time` | string | Observation timestamp |
| AccuWeather | `accuweather.relative_humidity` | number | Relative humidity percentage |
| AccuWeather | `accuweather.cloud_cover` | number | Cloud cover percentage |
| AccuWeather | `accuweather.uv_index` | number | UV index |
| AccuWeather | `accuweather.uv_index_text` | string | UV index description |
| AccuWeather | `accuweather.wind_speed_metric_value` | number | Wind speed in metric |
| AccuWeather | `accuweather.wind_speed_metric_unit` | string | Metric wind unit |
| AccuWeather | `accuweather.wind_speed_imperial_value` | number | Wind speed in imperial |
| AccuWeather | `accuweather.wind_speed_imperial_unit` | string | Imperial wind unit |
| AccuWeather | `accuweather.wind_direction` | string | Wind direction |
| AccuWeather | `accuweather.wind_direction_degrees` | number | Wind direction in degrees |
| AccuWeather | `accuweather.visibility_metric_value` | number | Visibility in metric |
| AccuWeather | `accuweather.visibility_metric_unit` | string | Metric visibility unit |
| AccuWeather | `accuweather.visibility_imperial_value` | number | Visibility in imperial |
| AccuWeather | `accuweather.visibility_imperial_unit` | string | Imperial visibility unit |
| AccuWeather | `accuweather.real_feel_metric_value` | number | Real feel temperature in metric |
| AccuWeather | `accuweather.real_feel_metric_unit` | string | Metric real feel unit |
| AccuWeather | `accuweather.real_feel_imperial_value` | number | Real feel temperature in imperial |
| AccuWeather | `accuweather.real_feel_imperial_unit` | string | Imperial real feel unit |
| WeatherAPI | `weatherapi.enabled` | boolean | Whether WeatherAPI is enabled |
| WeatherAPI | `weatherapi.weather_text` | string | Current conditions description |
| WeatherAPI | `weatherapi.weather_icon` | number | Weather condition code |
| WeatherAPI | `weatherapi.temp_c` | number | Temperature in Celsius |
| WeatherAPI | `weatherapi.temp_f` | number | Temperature in Fahrenheit |
| WeatherAPI | `weatherapi.feelslike_c` | number | Feels-like temperature in Celsius |
| WeatherAPI | `weatherapi.feelslike_f` | number | Feels-like temperature in Fahrenheit |
| WeatherAPI | `weatherapi.wind_kph` | number | Wind speed in km/h |
| WeatherAPI | `weatherapi.wind_mph` | number | Wind speed in mph |
| WeatherAPI | `weatherapi.wind_dir` | string | Wind direction (e.g. `NW`) |
| WeatherAPI | `weatherapi.wind_degree` | number | Wind direction in degrees |
| WeatherAPI | `weatherapi.humidity` | number | Humidity percentage |
| WeatherAPI | `weatherapi.cloud` | number | Cloud cover percentage |
| WeatherAPI | `weatherapi.uv` | number | UV index |
| WeatherAPI | `weatherapi.vis_km` | number | Visibility in km |
| WeatherAPI | `weatherapi.vis_miles` | number | Visibility in miles |
| WeatherAPI | `weatherapi.precip_mm` | number | Precipitation in mm |
| WeatherAPI | `weatherapi.is_day` | number | 1 if daytime, 0 if night |
| WeatherAPI | `weatherapi.last_updated` | string | Last updated timestamp |
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
