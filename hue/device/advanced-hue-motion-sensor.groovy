/**
 * Advanced Hue Motion Sensor 
 * Version 1.0.4
 * Download: https://github.com/apwelsh/hubitat
 * Description:
 * This is a child device handler for the Advance Hue Bridge Integration App.  This device reports light level
 * and battery level from a Hue connected sensor.  Although this can work in poll mode, it is highly recommended to
 * use the event stream based push notifications
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

@Field static final Boolean DEFAULT_LOG_ENABLE       = true
@Field static final Boolean DEFAULT_DBG_ENABLE       = false

@Field static final String SETTING_LOG_ENABLE        = 'logEnable'
@Field static final String SETTING_DBG_ENABLE        = 'debug'
@Field static final String SETTING_SENSITIVITY       = 'sensitivity'

metadata {
    definition (
        name:      'AdvancedHueMotionSensor',
        namespace: 'apwelsh',
        author:    'Armand Welsh',
        importUrl: 'https://raw.githubusercontent.com/apwelsh/hubitat/master/hue/device/advanced-hue-motion-sensor.groovy') {

        capability 'MotionSensor'
        capability 'Battery'
        capability 'Refresh'
    
        attribute  'status', 'string'  // expect enabled/disabled
        attribute  'health', 'string'  // reachable/unreachable
    }
}

preferences {

    input name: SETTING_LOG_ENABLE,
          type: 'bool',
          defaultValue: DEFAULT_LOG_ENABLE,
          title: 'Enable informational logging'

    input name: SETTING_DBG_ENABLE,
          type: 'bool',
          defaultValue: DEFAULT_DBG_ENABLE,
          title: 'Enable debug logging'
    
    def sensitivitymax = 1
    try {
        Map config = parent.stateForNetworkId(deviceInstance.deviceNetworkId)?.config
        sensitivitymax = config?.sensitivitymax?:1
        int s = config?.sensitivity == null ? sensitivitymax : config.sensitivity
        if (this[SETTING_SENSITIVITY] != s) {
            updateSetting(SETTING_SENSITIVITY, s)
        }
    } catch (ex) {
        log.error "${ex}"
    }
    
    input name: SETTING_SENSITIVITY,
          type: 'number',
          defaultValue: sensitivitymax,
          range: 0..sensitivitymax,
          title: 'Motion Sensitivity',
          description: "Enter a value from 0 to ${sensitivitymax}"

}

private getDeviceInstance() {
    parent.getChildDeviceById(device.deviceId)
}

void updateSetting(String name, Object value) {
    device.updateSetting(name, value)
    this[name] = value
}

/**
 * Hubitat DTH Lifecycle Functions
 **/
def installed() {
    updated()
    refresh()
}

def updated() {
    if (this[SETTING_LOG_ENABLE] == null) { updateSetting(SETTING_LOG_ENABLE,       DEFAULT_LOG_ENABLE) }
    if (this[SETTING_DBG_ENABLE] == null) { updateSetting(SETTING_DBG_ENABLE,       DEFAULT_DBG_ENABLE) }
    if (this[SETTING_LOG_ENABLE]) { log.debug 'Preferences updated' }

    Map configs = [:]
    Map devcfg = parent.stateForNetworkId(device.deviceNetworkId)?.config

    if (this[SETTING_SENSITIVITY] != devcfg[SETTING_SENSITIVITY]) { configs[SETTING_SENSITIVITY] = this[SETTING_SENSITIVITY] }
    
    parent.setDeviceConfig(this, configs)
}

/*
 * Device Capability Interface Functions
 */


void refresh() {
    if (this[SETTING_DBG_ENABLE]) { log.debug "Sensor (${this}) refreshing" }
    parent.getDeviceState(this)
}

void setHueProperty(Map args) {
}

