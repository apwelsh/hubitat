/**
 * Advanced Hue Bridge 
 * Version 1.3.10
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

@Field static final Boolean DEFAULT_AUTO_REFRESH     = false
@Field static final Integer DEFAULT_REFRESH_INTERVAL = 60
@Field static final Boolean DEFAULT_ANY_ON           = true
@Field static final Boolean DEFAULT_LOG_ENABLE       = true
@Field static final Boolean DEFAULT_DBG_ENABLE       = false

@Field static final String SETTING_AUTO_REFRESH      = 'autoRefresh'
@Field static final String SETTING_REFRESH_INTERVAL  = 'refreshInterval'
@Field static final String SETTING_ANY_ON            = 'anyOn'
@Field static final String SETTING_LOG_ENABLE        = 'logEnable'
@Field static final String SETTING_DBG_ENABLE        = 'debug'

@Field static final Map SCHEDULE_NON_PERSIST = [overwrite: true, misfire:'ignore']

metadata {
    definition (
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
              type: 'number',
              defaultValue: DEFAULT_REFRESH_INTERVAL,
              title: 'Refresh Inteval',
              description: 'Number of seconds to refresh the group state'
    }

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
def installed() {
    disconnect()
    updated()
}

def initialize() {
    disconnect()
    updated()
    runIn(1, refresh)
}

void updateSetting(String name, Object value) {
    device.updateSetting(name, value)
    this[name] = value
}

def updated() {
    if (this[SETTING_AUTO_REFRESH]     == null) { updateSetting(SETTING_AUTO_REFRESH,     DEFAULT_AUTO_REFRESH) }
    if (this[SETTING_REFRESH_INTERVAL] == null) { updateSetting(SETTING_REFRESH_INTERVAL, DEFAULT_REFRESH_INTERVAL) }
    if (this[SETTING_ANY_ON]           == null) { updateSetting(SETTING_ANY_ON,           DEFAULT_ANY_ON) }
    if (this[SETTING_LOG_ENABLE]       == null) { updateSetting(SETTING_LOG_ENABLE,       DEFAULT_LOG_ENABLE) }

    if (this[SETTING_LOG_ENABLE]) { log.debug 'Preferences updated' }
    runIn(1, connect)
    log.info "Parent logging enable? ${parent.newEnable}"
}

void connect() {
    if (parent.getVolatileAtomicState(this).WebSocketSubscribed == true) {
        return
    }
    String url = "https://${parent.getBridgeHost()}/eventstream/clip/v2"

    String apiKey = parent.state.username.trim()
    Map headers = ['hue-application-key': apiKey, Accept: 'text/event-stream']

    if (this[SETTING_DBG_ENABLE]) { log.debug "Attempting to establish streaming connection to ${url}" }

    interfaces.eventStream.connect(url, [
        ignoreSSLIssues: true,
        rawData: true,
        readTimeout: 3600,
        'headers': headers])
}

void disconnect() {
    interfaces.eventStream.close()
    parent.getVolatileAtomicState(this).WebSocketSubscribed = false
}

void parse(String text) {
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
            parseJson(message).each { event ->
                if (this[SETTING_DBG_ENABLE]) { 
                    log.debug "Parsing event: ${event}"
                }

                def data = event.data[0]
                String idV1 = (data.id_v1.split('/') as List).last()
                String dataType = data.type
                Map events = [state:[:], action:[:]]
                switch (event.type) {
                    case 'add':
                        if (parent.newEnable) {
                            log.warn "Discovered new device added to Hue hub of type: ${dataType}"
                            log.debug "$event"
                        }
                        break
                    case 'delete':
                        // if (parent.newEnable) {
                        //     log.warn "Discovered a device was deleted from Hue hub of type: ${dataType}"
                        //     log.debug "$event"
                        // }
                        break
                    case 'update':

                        switch (dataType) {
                            case 'light':
                                // log.info "Parsing event: ${event}"

                                def light = parent.getChildDevice(parent.networkIdForLight(idV1))
                                if (!light) { break }

                                events.state << mapEventState(data)

                                parent.setHueProperty(light, events)
                                runInMillis(500, refresh, [overwrite: true, misfire:'ignore']) // temporary.  Need to add logic to force refresh on affected groups
                                break

                            case 'grouped_light':
                                // log.info "Parsing event: ${event}"

                                def group = parent.getChildDevice(parent.networkIdForGroup(idV1))
                                if (!group) { break }
                                
                                events.action << mapEventState(data)

                                parent.setHueProperty(group, events)
                                runInMillis(500, refresh, [overwrite: true, misfire:'ignore'])  // temporary.  Need to add logic to force refresh on affected groups
                                break

                            case 'motion':
                            case 'temperature':
                            case 'light_level':
                            case 'device_power':
                                //log.info "Parsing event: ${event}"

                                def sensor = parent.getChildDevice(parent.networkIdForSensor(idV1))
                                if (!sensor) { break }

                                events.state << mapEventState(data)

                                parent.setHueProperty(sensor, events)
                                break

                            case 'button':
                                // log.info 'Unhandled button event'
                                break

                            case 'zigbee_connectivity':
                                break

                            default:
                                if (this[SETTING_DBG_ENABLE]) {
                                    log.warn "Unhandeled event data type: ${dataType}"
                                    log.debug "$event"
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
            return
        }

        if (this[SETTING_DBG_ENABLE]) { log.debug "Received unhandled message: ${text}" }

    } catch (ex) {
        log.error(ex)
        log.info text
    }
}

private Map mapEventState(data) {

    Map result = [:]
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
    if (data.power_state)                    { result << [battery:     data.power_state.battery_level]}

    return result
}

void eventStreamStatus(String message) {
    // log.trace message
    if (message ==~ /STOP:.*/) {
        parent.getVolatileAtomicState(this).WebSocketSubscribed = false
        resetRefreshSchedule()
        sendEvent(name: 'networkStatus', value: 'offline')
        if (this[SETTING_DBG_ENABLE]) { log.info 'Event Stream disconnected' }
    } else if (message ==~ /START:.*/) {
        parent.getVolatileAtomicState(this).WebSocketSubscribed = true
        sendEvent(name: 'networkStatus', value: 'online')
        unschedule(refresh)
        if (this[SETTING_DBG_ENABLE]) { log.info 'Event Stream connected' }
    } else {
        if (this[SETTING_DBG_ENABLE]) { log.debug "Received unhandled Event Stream status message: ${message}" }
    }
}


/*
 * Device Capability Interface Functions
 */

/** Switch Commands **/

void on() {
    if (this[SETTING_LOG_ENABLE]) { log.info "Bridge (${this}) turning on" }
    parent.setDeviceState(this, ['on':true])
}

void off() {
    if (this[SETTING_LOG_ENABLE]) { log.info "Bridge (${this}) turning off" }
    parent.setDeviceState(this, ['on': false])
}

void refresh() {
    unschedule(refresh)
    if (this[SETTING_DBG_ENABLE]) { log.debug "Bridge (${this}) refreshing" }
    parent.getDeviceState(this)
    parent.refreshHubStatus()
    connect()
}

void resetRefreshSchedule() {
    unschedule(refresh)
    if (this[SETTING_AUTO_REFRESH] && !parent.getVolatileAtomicState(this).WebSocketSubscribed) {
        runIn(this[SETTING_REFRESH_INTERVAL] ?: DEFAULT_REFRESH_INTERVAL, refresh, SCHEDULE_NON_PERSIST)
    }
}

void setHueProperty(Map args) {
    if (args.name == (this[SETTING_ANY_ON] ? 'any_on' : 'all_on')) {
        parent.sendChildEvent(this, [name: 'switch', value: value ? 'on' : 'off'])
    }
}
