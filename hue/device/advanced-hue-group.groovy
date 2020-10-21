/**
 * AdvancedHueGroup
 * Download: https://github.com/apwelsh/hubitat
 * Description:
 * This is a child device for the Advanced Hue Bridge Integeration app.  This device is used to manage hue zones and rooms
 * as color lights.  The key difference between this and the built-in hueGroup device is that this device supports the
 * refresh capability -- if enabled -- to allow for fast device refresh, and to act as the parent device for the
 * AdvancedHueScene device.
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

@Field static final String  SCENE_MODE_TRIGGER       = 'trigger'
@Field static final String  SCENE_MODE_SWITCH        = 'switch'

@Field static final Boolean DEFAULT_SCENE            = ''
@Field static final String  DEFAULT_SCENE_MODE       = 'trigger'
@Field static final Boolean DEFAULT_SCENE_OFF        = false
@Field static final Boolean DEFAULT_AUTO_REFRESH     = false
@Field static final Number  DEFAULT_REFRESH_INTERVAL = 60
@Field static final Boolean DEFAULT_ANY_ON           = true
@Field static final Boolean DEFAULT_LOG_ENABLE       = false

@Field static final Map     EVENT_SWITCH_ON          = [name: 'switch', value: 'on']
@Field static final Map     EVENT_SWITCH_OFF         = [name: 'switch', value: 'off']

@Field static final Map     SCHEDULE_NON_PERSIST     = [overwrite: true, misfire:'ignore']

preferences {

    input name:         'defaultScene',
          type:         'string',
          defaultValue: DEFAULT_SCENE,
          title:        'Default Scene',
          description:  'Enter a scene name or number as define by the Hue Bridge to activate when this group is turned on.'

    input name:         'sceneMode',
          type:         'enum',
          defaultValue: DEFAULT_SCENE_MODE,
          title:        'Scene Child Device Behavior',
          options:      [SCENE_MODE_TRIGGER, SCENE_MODE_SWITCH],
          description:  'If set to switch, the scene can be used to turn off this group. Only one scene can be on at any one time.'

    if (sceneMode == SCENE_MODE_SWITCH) {
        input name:         'sceneOff',
              type:         'bool',
              defaultValue: DEFAULT_SCENE_OFF,
              title:        'Track Scene State',
              description:  'If enabled, any change to this group will turn off all child scenes.'
    }

    input name:         'autoRefresh',
          type:         'bool',
          defaultValue: DEFAULT_AUTO_REFRESH,
          title:        'Auto Refresh',
          description:  'Should this device support automatic refresh'

    if (autoRefresh) {
        input name:         'refreshInterval',
              type:         'number',
              defaultValue: DEFAULT_REFRESH_INTERVAL,
              title:        'Refresh Inteval',
              description:  'Number of seconds to refresh the group state'
    }

    input name:         'anyOn',
          type:         'bool',
          defaultValue: DEFAULT_ANY_ON,
          title:        'ANY on or ALL on',
          description:  'When ebabled, the group is considered on when any light is on'

    input name:         'logEnable',
          type:         'bool',
          defaultValue: DEFAULT_LOG_ENABLE,
          title:        'Enable informational logging'
}

metadata {
    definition (name:      'AdvancedHueGroup',
                namespace: 'apwelsh',
                author:    'Armand Welsh',
                importUrl: 'https://raw.githubusercontent.com/apwelsh/hubitat/master/hue/device/advanced-hue-group.groovy') {

        capability 'Light'
        capability 'ChangeLevel'
        capability 'Switch'
        capability 'SwitchLevel'
        capability 'Actuator'
        capability 'ColorControl'
        capability 'ColorMode'
        capability 'ColorTemperature'
        capability 'Refresh'

        command 'activateScene', [[name: 'Scene Identitier*', type: 'STRING', description: 'Enter a scene name or the scene number as defined by the Hue Bridge']]
    }
}

def installed() {
    parent.getDeviceState(this)
    updated()
}

def updated() {

    if (settings.scene           == null) { device.updateSetting('scene',           DEFAULT_SCENE) }
    if (settings.sceneMode       == null) { device.updateSetting('sceneMode',       DEFAULT_SCENE_MODE) }
    if (settings.sceneOff        == null) { device.updateSetting('sceneOff',        DEFAULT_SCENE_OFF) }
    if (settings.autoRefresh     == null) { device.updateSetting('autoRefresh',     DEFAULT_AUTO_REFRESH) }
    if (settings.refreshInterval == null) { device.updateSetting('refreshInterval', DEFAULT_REFRESH_INTERVAL) }
    if (settings.anyOn           == null) { device.updateSetting('anyOn',           DEFAULT_ANY_ON) }
    if (settings.logEnable       == null) { device.updateSetting('logEnable',       DEFAULT_LOG_ENABLE) }

    if (logEnable) { log.debug 'Preferences updated' }
    parent.getDeviceState(this)
    refresh()

}

/** Switch Commands **/

void on() {
    if (logEnable) { log.info "Group (${this}) turning on" }
    if (defaultScene?.trim()) {
        if (logEnable) { log.info "Using scene to turn on group (${this})" }
        activateScene(defaultScene)
    } else {
        parent.componentOn(this)
        attributeChanged()
    }
}

void off() {
    if (logEnable) { log.info "Group (${this}) turning off" }
    parent.componentOff(this)
    attributeChanged()
}

/** ColorControl Commands **/

void setColor(Map colormap) {
    if (logEnable) { log.info "Setting (${this}) mapped color: ${colormap}" }
    parent.componentSetColor(this, colormap)
    attributeChanged()
}

void setHue(hue) {
    if (logEnable) { log.info "Setting (${this}) hue: ${hue}" }
    parent.componentSetHue(this, hue)
    attributeChanged()
}

void setSaturation(saturation) {
    if (logEnable) { log.info "Setting (${this}) saturation: ${saturation}" }
    parent.componentSetSaturation(this, saturation)
    attributeChanged()
}

/** ColorTemperature Commands **/

void setColorTemperature(colortemperature) {
    if (logEnable) { log.info "Setting (${this}) color temp: ${colortemperature}" }
    parent.componentSetColorTemperature(this, colortemperature)
    attributeChanged()
}

/** SwitchLevel Commands **/

void setLevel(level, duration=null) {
    if (logEnable) { log.info "Setting (${this}) level: ${level}" }
    parent.componentSetLevel(this, level, duration)
    attributeChanged()
}

/** ChangeLevel Commands **/

void startLevelChange(String direction) {
    if (logEnable) { log.info "Starting (${this}) level change: ${direction}" }
    parent.componentStartLevelChange(this, direction)
    attributeChanged()
}

void stopLevelChange() {
    if (logEnable) { log.info "Stopping (${this}) level change" }
    parent.componentStopLevelChange(this)
    attributeChanged()
}

/** Refresh Commands **/
void refresh() {
    if (debug) { log.debug "Bridge (${this}) refreshing" }
    parent.getDeviceState(this)
    resetRefreshSchedule()
}

void resetRefreshSchedule() {
    unschedule()
    if (autoRefresh) { runIn(refreshInterval ?: DEFAULT_REFRESH_INTERVAL, refresh, SCHEDULE_NON_PERSIST) }
}

void attributeChanged() {
    if (sceneMode == 'switch' && sceneOff) { allOff() }
}

void setHueProperty(Map args) {
    if (args.name == (anyOn ? 'any_on' : 'all_on')) {
        parent.sendChildEvent(this, args.value ? EVENT_SWITCH_ON : EVENT_SWITCH_OFF)
    } else if (args.name == 'scene') {
        sceneEnabled(args.value)
    }
}

void sceneEnabled(String sceneId) {
    String groupId = parent.deviceIdNode(device.deviceNetworkId)
    def scene = getChildDevice(parent.networkIdForScene(groupId, sceneId))
    exclusiveOn(scene)
    if (scene) {
        scene.unschedule()
        if (sceneMode == SCENE_MODE_TRIGGER) { scene.runInMillis(400, 'off') }
    }
}

void activateScene(String scene) {
    if (logEnable) { log.info "Attempting to activate scene: ${scene}" }
    String sceneId = parent.findScene(parent.deviceIdNode(device.deviceNetworkId), scene)?.key
    if (sceneId) {
        if (logEnable) { log.info "Activating scene with Hue Scene ID: ${sceneId}" }
        parent.setDeviceState(this, ['scene': sceneId])
    } else {
        if (logEnable) { log.warning "Cannot locate Hue scene ${scene}; verify the scene id or name is correct." }
    }
}

void allOff() {
    childDevices.findAll { scene -> scene.currentValue('switch') == 'on' }.each { scene ->
        parent.sendChildEvent(scene, EVENT_SWITCH_OFF)
        log.info "Scene ($scene) turned off"
    }
}

void exclusiveOn(def child) {
    if (child) {
        String dni = child.device.deviceNetworkId
        childDevices.each { scene ->
            Map event = (scene.deviceNetworkId == dni) ? EVENT_SWITCH_ON : EVENT_SWITCH_OFF
            if (scene.currentValue('switch') != event.value) {
                parent.sendChildEvent(scene, event)
                log.info "Scene (${scene}) turned ${event.value}"
            }
        }
    }
    if (!(device.currentValue('switch') == 'on')) {
        parent.sendChildEvent(this, EVENT_SWITCH_ON)
    }
}

/*
 * Component Child Methods (used to capture actions generated on scenes)
 */

void componentOn(def child) {
    if (logEnable) { log.info "Scene (${child}) turning on" }
    String sceniId = parent.deviceIdNode(child.deviceNetworkId)
    parent.setDeviceState(this, ['scene':sceniId])
}

void componentOff(def child) {
    if (logEnable) { log.info "Scene (${child}) turning off" }
    // Only change the state to off, there is not action to actually be performed.
    if (sceneMode == 'switch') {
        if (child.currentValue('switch') == 'on') off()
    } else {
        parent.sendChildEvent(child, EVENT_SWITCH_OFF)
    }
}

void componentRefresh(def child) {
    if (logEnable) { log.info "Received refresh request from ${child.displayName} - ignored" }
}
