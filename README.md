# What is Isthmus

Head over to https://isthmus.want.ch to get a better picture.

# Code structure

## Setup

The installation's configuration is stored in a YML file, the location of which is defined in the internal application.yml property "internal.userproperties-path". The classes `UserProperties.java` and `UserPropertiesManager.java` are responsible for keeping the loaded state and YML file in sync. (beware that the user might edit the YML file directly, changes then need to get picked up by isthmus)

## Rule engine

There are basically two event triggers

    1. CRON, handled by `ScheduleEngine.java`
    2. Webhook, received by `WebhookController.java` and then handled by `WebhookRuleEngine.java`

The former issues a GET request to retrieve a payload, while the latter receives the payload as part of the incoming request. Both then

    * Check if the payload passes the filter check (if any filter is defined)
    * Convert payload into a data map
    * Run the provided freemarker template against the data map
    * Send the resulting payload to the configured URL     
    
## UI

The (very basic) UI is defined in resources/static (default Spring Boot), backed by REST methods defined in the `HubSettingsController.java`

