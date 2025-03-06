/**
 * Advanced Hue Bridge
 * Version 1.5.0
 * Download: https://github.com/apwelsh/hubitat
 * Description:
 * This is a child device handler for the Advance Hue Bridge Integration App.  This device manage the hub directly for
 * actions such as refresh.
 *-------------------------------------------------------------------------------------------------------------------
 * Copyright 2020 Armand Peter Welsh
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the 'Software'), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
 * and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED 'AS IS', WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 *-------------------------------------------------------------------------------------------------------------------
 **/

import groovy.transform.Field

@Field static final Boolean DEFAULT_WATCHDOG         = false
@Field static final Boolean DEFAULT_AUTO_REFRESH     = true
@Field static final Integer DEFAULT_REFRESH_INTERVAL = 600
@Field static final Boolean DEFAULT_ANY_ON           = true
@Field static final Boolean DEFAULT_LOG_ENABLE       = true
@Field static final Boolean DEFAULT_DBG_ENABLE       = false

@Field static final String SETTING_WATCHDOG          = 'watchdog'
@Field static final String SETTING_AUTO_REFRESH      = 'autoRefresh'
@Field static final String SETTING_REFRESH_INTERVAL  = 'refreshInterval'
@Field static final String SETTING_ANY_ON            = 'anyOn'
@Field static final String SETTING_LOG_ENABLE        = 'logEnable'
@Field static final String SETTING_DBG_ENABLE        = 'debug'
@Field static final Map OPTIONS_REFRESH_INTERVAL     = [   60: '1 Minute',
                                                          300: '5 Minutes',
                                                          600: '10 Minutes',
                                                          900: '15 Minutes',
                                                         1800: '30 Minutes',
                                                         3600: '1 Hour',
                                                        10800: '3 Hours']

@Field static final Map SCHEDULE_NON_PERSIST = [overwrite: true, misfire:'ignore']

metadata {
    definition(
        name:      'AdvancedHueBridge',
        namespace: 'apwelsh',
        author:    'Armand Welsh',
        importUrl: 'https://raw.githubusercontent.com/apwelsh/hubitat/master/hue/device/advanced-hue-bridge.groovy') {

        capability 'Switch'
        capability 'Refresh'
        capability 'Initialize'

        command 'connect'
        command 'disconnect'

        attribute 'networkStatus', 'string'
        }
}

preferences {

    input name: SETTING_AUTO_REFRESH,
          type: 'bool',
          defaultValue: DEFAULT_AUTO_REFRESH,
          title: 'Auto Refresh',
          description: 'Should this device support automatic refresh'
    if (this[SETTING_AUTO_REFRESH] == true) {
        input name: SETTING_REFRESH_INTERVAL,
              type: 'enum',
              defaultValue: DEFAULT_REFRESH_INTERVAL,
              title: 'Refresh Inteval',
              description: 'How often to poll the hue system for device state.',
              options: OPTIONS_REFRESH_INTERVAL,
              required: true
    }
    input name: SETTING_WATCHDOG,
        type: 'bool',
        defaultValue: DEFAULT_WATCHDOG,
        title: 'Connection Watchdog',
        description: 'If enabled, the system will agressively watch for connection state chagnes, and try to reconnect every 10 seconds'

    input name: SETTING_ANY_ON,
          type: 'bool',
          defaultValue: DEFAULT_ANY_ON,
          title: 'ANY on or ALL on',
          description: 'When ebabled, the group is considered on when any light is on'

    input name: SETTING_LOG_ENABLE,
          type: 'bool',
          defaultValue: DEFAULT_LOG_ENABLE,
          title: 'Enable informational logging'

    input name: SETTING_DBG_ENABLE,
          type: 'bool',
          defaultValue: DEFAULT_DBG_ENABLE,
          title: 'Enable debug logging'
}

/**
 * Hubitat DTH Lifecycle Functions
 **/
void installed() {
    sendEvent(name: 'networkStatus', value: 'offline')
}

void initialize() {
    sendEvent(name: 'networkStatus', value: 'offline')
    updated()
    runIn(1, connect)
}

void updateSetting(String name, Object value) {
    device.updateSetting(name, value)
    this[name] = value
}

/**
 * This method is called when the device's settings are updated.
 * It performs the following actions:
 * 1. Checks if certain settings are null and updates them to their default values if necessary.
 * 2. Validates the refresh interval setting and updates it to the default value if it is invalid.
 * 3. Resets the refresh schedule if auto-refresh is enabled, otherwise unschedules the refresh.
 * 4. Logs a message if logging is enabled.
 * 5. Schedules the watchdog timer based on the watchdog setting:
 *    - If the watchdog setting is enabled, schedules it to run every 10 seconds.
 *    - If the watchdog setting is disabled, schedules it to run every 1 minute.
 * 6. Initiates a connection after a delay of 1 second.
 */
void updated() {
    if (this[SETTING_WATCHDOG]         == null) { updateSetting(SETTING_WATCHDOG,         DEFAULT_WATCHDOG) }
    if (this[SETTING_AUTO_REFRESH]     == null) { updateSetting(SETTING_AUTO_REFRESH,     DEFAULT_AUTO_REFRESH) }
    if (this[SETTING_REFRESH_INTERVAL] == null) { updateSetting(SETTING_REFRESH_INTERVAL, DEFAULT_REFRESH_INTERVAL) }
    if (this[SETTING_ANY_ON]           == null) { updateSetting(SETTING_ANY_ON,           DEFAULT_ANY_ON) }
    if (this[SETTING_LOG_ENABLE]       == null) { updateSetting(SETTING_LOG_ENABLE,       DEFAULT_LOG_ENABLE) }

    if (!OPTIONS_REFRESH_INTERVAL[this[SETTING_REFRESH_INTERVAL] as int]) {
        log.warn "Refresh interval is invalid.  Changing refresh interval from ${this[SETTING_REFRESH_INTERVAL]} seconds to default interval of ${OPTIONS_REFRESH_INTERVAL[DEFAULT_REFRESH_INTERVAL]}"
        updateSetting(SETTING_REFRESH_INTERVAL, DEFAULT_REFRESH_INTERVAL)
    }
    if (this[SETTING_AUTO_REFRESH]) {
        resetRefreshSchedule()
    } else {
        unschedule(refresh)
    }

    if (this[SETTING_LOG_ENABLE]) { log.info 'Preferences updated' }

    scheduleWatchdog()
    runIn(1, connect)

}

/**
 * Sends an event with the specified properties and updates the cached values.
 *
 * This method overrides the default driver.sendEvent() to ensure that the cached values
 * are updated whenever an event is sent. If the current value of the property is different
 * from the new value, the event is sent and the cached value is updated.
 *
 * @param properties A map containing the properties of the event to be sent. The map should
 *                   include the following keys:
 *                   - name: The name of the property.
 *                   - value: The new value of the property.
 */
void sendEvent(Map properties) {

    // Overrides driver.sendEvent() to populate cached values
    if (currentValue(properties.name) != properties.value) {
        device.sendEvent(properties)
        parent.getAtomicState(this)[properties.name] = properties.value
    }
}

/**
 * Retrieves the current value of the specified attribute.
 *
 * This method first attempts to get the value from the parent's volatile atomic state.
 * If the value is not found, it retrieves the value from the device's current state and
 * updates the parent's volatile atomic state with this value.
 *
 * @param attributeName The name of the attribute whose value is to be retrieved.
 * @return The current value of the specified attribute, or null if not found.
 */
Object currentValue(String attributeName) {
    def result = parent.getAtomicState(this)[attributeName]
    if (result != null) {
        return result
    }
    result = device.currentValue(attributeName)
    if (result != null) {
        parent.getAtomicState(this)[attributeName] = result
    }
    return result
}

void connect() {
    disconnect()

    String url = "https://${parent.getBridgeHost()}/eventstream/clip/v2"

    String apiKey = parent.state.username.trim()
    Map headers = ['hue-application-key': apiKey, Accept: 'text/event-stream']

    if (this[SETTING_DBG_ENABLE]) { log.debug "Attempting to establish streaming connection to ${url}" }

    interfaces.eventStream.connect(url, [
        ignoreSSLIssues: true,
        rawData: true,
        pingInterval: 5,
        readTimeout: 3600,
        'headers': headers])

    scheduleWatchdog()
}

/**
 * Disconnects the event stream interface.
 *
 * This method closes the event stream interface and sets the desired state to unsubscribed,
 * ensuring that the watchdog is not in effect.
 */
void disconnect() {
    // Set desired state to unsubscribed, so watchdog is not in effect
    interfaces.eventStream.close()
    unschedule(watchdog)
}

/**
 * Schedules the watchdog timer based on the watchdog setting.
 *
 * This method schedules the watchdog timer to run every 10 seconds if the watchdog setting is enabled,
 * and every 1 minute if the watchdog setting is disabled. It logs an informational message indicating
 * the timer setting.
 */
void scheduleWatchdog() {
    if (this[SETTING_WATCHDOG]) {
        schedule('0/10 * * ? * *', 'watchdog', [overwrite: true])
        if (this[SETTING_LOG_ENABLE]) { log.info 'Watchdog timer set for every 10 seconds' }
    } else {
        schedule('0 * * ? * *', 'watchdog', [overwrite: true])
        if (this[SETTING_LOG_ENABLE]) { log.info 'Watchdog timer set for every 1 minute' }
    }
}
/**
 * Monitors the network status and attempts to reconnect if the status is offline.
 *
 * This method checks the current network status. If the status is not 'online',
 * it logs an informational message indicating that the event stream is offline
 * and attempts to reconnect by calling the `connect()` method.
 *
 * Logging is controlled by the `SETTING_LOG_ENABLE` setting.
 */
void watchdog() {
    if (currentValue('networkStatus') != 'online') {
        if (this[SETTING_LOG_ENABLE]) { log.info 'Watchdog event: Event Stream offline, attempting reconnect' }
        connect()
    }
}

/**
 * Handles the status of the event stream connection.
 *
 * @param message The status message received from the event stream.
 *                Expected values are:
 *                - "STOP:" to indicate the event stream has stopped.
 *                - "START:" to indicate the event stream has started.
 *                - Any other value will be logged as an unhandled message if debugging is enabled.
 *
 * When the event stream stops:
 * - Sets the WebSocketSubscribed state to false.
 * - Sends an event indicating the network status is offline.
 * - Logs the disconnection if logging is enabled.
 *
 * When the event stream starts:
 * - Sets the WebSocketSubscribed state to true.
 * - Sends an event indicating the network status is online.
 * - Logs the connection if logging is enabled.
 * - Calls the refresh method to synchronize all devices with the hub state.
 *
 * Any other message will be logged as an unhandled message if debugging is enabled.
 */
void eventStreamStatus(String message) {
    if (message.startsWith('STOP:')) {
        if (parent.getAtomicState(this).WebSocketSubscribed != false) {
            parent.getAtomicState(this).WebSocketSubscribed = false
            sendEvent(name: 'networkStatus', value: 'offline')
            if (this[SETTING_LOG_ENABLE]) { log.info 'Event Stream disconnected' }
        }

    } else if (message.startsWith('START:')) {
        if (parent.getAtomicState(this).WebSocketSubscribed != true) {
            parent.getAtomicState(this).WebSocketSubscribed = true
            // record that web socket connected and should be subscribed.
            sendEvent(name: 'networkStatus', value: 'online')
            if (this[SETTING_LOG_ENABLE]) { log.info 'Event Stream connected' }
            refresh()  // followed by a refresh to bring all devices back in-sync with hub state
        }
    } else {
        if (this[SETTING_DBG_ENABLE]) { log.debug "Received unhandled Event Stream status message: ${message}" }
    }
}

/**
 * Parses the given text message and processes it based on its type.
 *
 * @param text The text message to parse.
 *
 * The method handles the following types of messages:
 * - 'id': Logs the received message ID if debugging is enabled.
 * - 'data': Parses the JSON message and processes events such as 'add', 'delete', and 'update'.
 *           Updates the hub events map and refreshes devices as needed.
 *
 * If the message type is not recognized, it logs a debug message if debugging is enabled.
 *
 * The method also handles exceptions by logging the error and the original text message.
 */
void parse(String text) {

    if (!text) { return }

    if (text.startsWith(':')) {
        return
    }

    def (String type, String message) = text.split(':', 2)
    try {

        if (type == 'id') {
            if (this[SETTING_DBG_ENABLE]) { log.debug "Received message with ID ${message}" }
            return
        }
        if (type == 'data') {
            Map hubEvents = [:]
            List refreshList = []
            parseJson(message).each { event ->
                if (this[SETTING_DBG_ENABLE]) {
                    log.debug "Received EventStream: ${event}"
                }

                event.data.each { Map data ->
                    switch (event.type) {
                        case 'add':
                            addEventHandler(data)
                            break
                        case 'delete':
                            deleteEventHandler(data)
                            break
                        case 'update':

                            // Handles the update event by processing the provided data and updating the hubEvents map accordingly.
                            // Iterates over the properties returned by updateEventHandler(data).
                            // For each property, it updates the hubEvents map for the corresponding network ID (nid).
                            // If the property contains action, state, or config data, it appends them to the respective fields in hubEvents.
                            // If the property contains an ID, it sets the ID in hubEvents.
                            // Determines the data type and refreshes the list of network IDs for groups containing the specified light.
                            // If the data type is 'light', it finds the groups containing the light and adds their network IDs to the refresh list.
                            // If the data type is 'grouped_light', it adds the network ID to the refresh list to force a refresh of the group.
                            updateEventHandler(data).each { nid, props ->
                                // get current (or new) instance of hubEvents map for this nid
                                hubEvents[nid] = hubEvents[nid] ?: [action:[:],state:[:],config:[:]]
                                if (props.action) { hubEvents[nid].action << props.action }
                                if (props.state)  { hubEvents[nid].state << props.state }
                                if (props.config) { hubEvents[nid].config << props.config }
                                if (props.id)     { hubEvents[nid].id = props.id }

                                // Processes the data type and refreshes the list of network IDs for groups containing the specified light.
                                String dataType = data.type
                                if (data.type == 'light') {
                                    def id = parent.deviceIdNode(nid)
                                    refreshList << parent.state.groups.findAll { it.value.lights?.contains(id)
                                    }.keySet().collect { gid ->
                                        parent.networkIdForGroup(gid)
                                    }
                                }
                                // Refreshes the list of network IDs for groups affected by event.
                                if (data.type == 'grouped_light') {
                                    refreshList << nid  // force a refresh of the group (colors and scenes not sent)
                                }
                            }

                            break

                        default:
                            if (this[SETTING_DBG_ENABLE]) {
                                log.warn "Unhandeled event type: ${event.type}"
                                log.debug "$event"
                            }
                    }
                }
            }
            if (this[SETTING_DBG_ENABLE]) { log.debug "Received Events: $hubEvents" }

            // Iterates over each entry in the hubEvents map, retrieves the corresponding child device using the nid (node ID),
            // and sets the Hue properties for that child device by calling the setHueProperty method of the parent app.
            hubEvents.each { nid, props ->
                def child = parent.getChildDevice(nid)
                parent.setHueProperty(child, props)
            }
            // If there are devices that need to be refreshed, queue them for refresh
            if (refreshList) {
                parent.queueDeviceRefresh(refreshList.flatten().unique())
            }

            return
        }

        if (this[SETTING_DBG_ENABLE]) { log.debug "Received unhandled message: ${text}" }

    } catch (ex) {
        log.error(ex)
        log.info text
    }
}

private void addEventHandler(Map data) {
    String dataType = data.type

    if (parent.newEnable) {
        log.warn "Discovered new device added to Hue hub of type: ${dataType}"
        log.debug "$event"
    }
}

private void deleteEventHandler(Map data) {
// if (parent.newEnable) {
//     log.warn "Discovered a device was deleted from Hue hub of type: ${dataType}"
//     log.debug "$event"
// }
}

/**
 * Handles the update event by parsing the provided data and mapping it to the appropriate event structure.
 *
 * @param data A map containing the event data to be processed.
 * @return A map containing the parsed event data, or an empty map if the data is not parsable or relevant.
 *
 * The function processes different types of events based on the 'type' field in the provided data:
 * - 'light': Parses light events and maps the state and config.
 * - 'grouped_light': Parses grouped light events and maps the action and config.
 * - 'motion', 'temperature', 'light_level', 'device_power': Parses sensor events and maps the state and config.
 * - 'button': Parses button events and sets the Hue property on the child device.
 * - 'zigbee_connectivity': Ignores Zigbee connectivity events.
 * - Default: Logs a warning for unhandled event data types if debugging is enabled.
 *
 * The function also handles cases where the 'id_v1' field is missing or the corresponding child device is not found.
 */
private Map updateEventHandler(Map data) {
    Map event = [state:[:], action:[:], config:[:], id:""]

    if (!data.id_v1) {
        if (this[SETTING_DBG_ENABLE]) { log.debug "Received unparsable v2 only message; missing id_v1: ${data}" }
        return [:]
    }
    String idV1 = (data.id_v1.split('/') as List).last()
    event.id = data.id

    String dataType = data.type
    switch (dataType) {
        case 'light':
            String nid = parent.networkIdForLight(idV1)
            if (!parent.getChildDevice(nid)) { return [:] }

            event.state << mapEventState(data)
            event.config << event.state.config
            event.state.remove('config')

            return [(nid): event]

        case 'grouped_light':
            String nid = parent.networkIdForGroup(idV1)
            if (!parent.getChildDevice(nid)) { return [:] }
            Map pevent = mapEventState(data)
            event.action << pevent
            event.config << event.action.config
            event.action.remove('config')

            return [(nid): event]

        case 'motion':
        case 'temperature':
        case 'light_level':
        case 'device_power':
            String nid = parent.networkIdForSensor(idV1)
            if (!parent.getChildDevice(nid)) { return [:] }

            event.state << mapEventState(data)
            event.config << event.state.config
            event.state.remove('config')

            return [(nid): event]

        case 'button':
            String nid = parent.networkIdForSensor(idV1)
            def child = parent.getChildDevice(nid)
            if (!child) { return [:] }

            Map props = [
                id: data.id,
                last_event: data.button.last_event
            ]
            child.setHueProperty(props)
            return [:]

        case 'zigbee_connectivity':
            return [:]

        default:
            if (this[SETTING_DBG_ENABLE]) {
                log.warn "Unhandeled event data type: ${dataType}"
                log.debug "$data"
            }
            return [:]
    }
}

/**
 * Maps the event state data to a result map with specific attributes.
 *
 * @param data The input data containing various attributes such as on, dimming, color, color_temperature, motion, temperature, light, power_state, enabled, and status.
 * @return A map containing the mapped event state with the following possible keys:
 *         - config: A map containing device health attributes such as battery, on, and reachable.
 *         - colormode: The color mode, either 'xy' or 'ct'.
 *         - xy: The x and y coordinates for color in 'xy' mode.
 *         - mirek: The mirek value for color temperature in 'ct' mode.
 *         - presence: The motion presence state.
 *         - temperature: The temperature value multiplied by 100.0.
 *         - lightlevel: The light level value.
 */
private Map mapEventState(data) {

    Map result = [config:[:]]
    if (data.on)      { result << data.on }
    if (data.dimming) { result << data.dimming }
    if (data.color)   {
        result << [colormode: 'xy']
        result << [xy: [data.color.xy.x, data.color.xy.y]]
    }
    if (data.color_temperature && data.color_temperature.mirek_valid == true) {
        result << [colormode: 'ct']
        result << data.color_temperature.find { k, v -> k == 'mirek' }
    }

    if (data.motion?.motion_valid)           { result << [presence:    data.motion.motion] }
    if (data.temperature?.temperature_valid) { result << [temperature: data.temperature.temperature * 100.0] }
    if (data.light?.light_level_valid)       { result << [lightlevel:  data.light.light_level] }

    // device health attributes
    if (data.power_state)                    { result.config << [battery:   data.power_state.battery_level] }
    if (data.enabled != null)                { result.config << [on:        data.enabled] }
    if (data.status != null)                 { result.config << [reachable: data.status == 'connected'] }
    return result
}

/*
 * Device Capability Interface Functions
 */

/** Switch Commands **/

/**
 * Turns on all the lights of the managed bridge.
 * Calls the parent method to set the device state to 'on'.
 */
void on() {
    if (this[SETTING_LOG_ENABLE]) { log.info "Bridge (${this}) turning on" }
    parent.setDeviceState(this, ['on':true])
}
/**
 * Turns off all the lights of the managed bridge.
 * Calls the parent method to set the device state to 'off'.
 */
void off() {
    if (this[SETTING_LOG_ENABLE]) { log.info "Bridge (${this}) turning off" }
    parent.setDeviceState(this, ['on': false])
}


/**
 * Refreshes the state of the device by performing the following actions:
 * 1. Logs a trace message if debugging is enabled.
 * 2. Requests the device state from the parent.
 * 3. Refreshes the hub status from the parent.
 */
void refresh() {
    if (this[SETTING_DBG_ENABLE]) { log.trace "refreshing" }
    parent.getDeviceState(this)
    parent.refreshHubStatus()
}

/**
 * Resets the refresh schedule based on the auto-refresh setting and the specified refresh interval.
 * 
 * If the auto-refresh setting is enabled, this method schedules the 'refresh' method to run at 
 * intervals specified by the refresh interval setting. The possible intervals are:
 * 
 * - 60 seconds (1 minute)
 * - 300 seconds (5 minutes)
 * - 600 seconds (10 minutes)
 * - 900 seconds (15 minutes)
 * - 1800 seconds (30 minutes)
 * - 3600 seconds (1 hour)
 * - 10800 seconds (3 hours)
 * 
 * The 'refresh' method is scheduled with the 'overwrite' option set to true, ensuring that any 
 * existing schedule is replaced.
 * 
 * @throws IllegalArgumentException if the refresh interval is not one of the specified values.
 */
void resetRefreshSchedule() {
    if (this[SETTING_AUTO_REFRESH]) {
        switch (this[SETTING_REFRESH_INTERVAL] as int) {
            case 60:
                runEvery1Minute('refresh', [overwrite: true])
                break
            case 300:
                runEvery5Minutes('refresh', [overwrite: true])
                break
            case 600:
                runEvery10Minutes('refresh', [overwrite: true])
                break
            case 900:
                runEvery15Minutes('refresh', [overwrite: true])
                break
            case 1800:
                runEvery30Minutes('refresh', [overwrite: true])
                break
            case 3600:
                runEvery1Hour('refresh', [overwrite: true])
                break
            case 10800:
                runEvery3Hours('refresh', [overwrite: true])
                break
        }
    }
}

/**
 * Sets the hue property based on the provided arguments.
 *
 * @param args A map containing the following keys:
 *             - name: The name of the property to set.
 *             - value: The value to set for the property.
 *
 * If the name matches the condition (either 'any_on' or 'all_on' based on the SETTING_ANY_ON setting),
 * it sends a child event to the parent with the switch state ('on' or 'off').
 */
void setHueProperty(Map args) {
    if (args.name == (this[SETTING_ANY_ON] ? 'any_on' : 'all_on')) {
        parent.sendChildEvent(this, [name: 'switch', value: value ? 'on' : 'off'])
    }
}
