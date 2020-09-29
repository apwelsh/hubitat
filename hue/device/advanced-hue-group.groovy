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
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation 
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, 
 * and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of 
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO 
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE 
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF 
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 *-------------------------------------------------------------------------------------------------------------------
 **/

preferences {
    input name: "defaultScene", type: "string", defaultValue: "", title: "Default Scene", options:scenes, description: "Enter a scene name or number as define by the Hue Bridge to activate when this group is turned on."
    input name: "sceneMode", type: "enum", defaultValue: "trigger", title: "Scene Child Device Behavior", options:["trigger", "switch"], description: "If set to switch, the scene can be used to turn off this group. Only one scene can be on at any one time."
    if (sceneMode == "switch")
        input name: "sceneOff", type: "bool", defaultValue: false, title: "Track Scene State", description: "If enabled, any change to this group will turn off all child scenes." 
    input name: "autoRefresh", type: "bool", defaultValue: false, title: "Auto Refresh",        description: "Should this device support automatic refresh" 
    if (autoRefresh)
        input name: "refreshInterval", type: "number", defaultValue: 60, title: "Refresh Inteval", description: "Number of seconds to refresh the group state" 
    input name: "anyOn",       type: "bool", defaultValue: true,  title: "ANY on or ALL on",    description: "When ebabled, the group is considered on when any light is on"
    input name: "logEnable",   type: "bool", defaultValue: false, title: "Enable informational logging"
}

metadata {
    definition (name:      "AdvancedHueGroup", 
                namespace: "apwelsh", 
                author:    "Armand Welsh", 
                importUrl: "https://raw.githubusercontent.com/apwelsh/hubitat/master/hue/device/advanced-hue-group.groovy") {
        
        capability "Light"
        capability "ChangeLevel"
        capability "Switch"
        capability "SwitchLevel"
        capability "Actuator"
        capability "ColorControl"
        capability "ColorMode"
        capability "ColorTemperature"
        capability "Refresh"
        
        command "activateScene", [[name: "Scene Identitier*", type: "STRING", description: "Enter a scene name or the scene number as defined by the Hue Bridge"]]
    }
}

def installed() {
    parent.getDeviceState(this)
    updated()
}

def updated() {

    if (settings.autoRefresh     == null) device.updateSetting("autoRefresh",     false)
    if (settings.refreshInterval == null) device.updateSetting("refreshInterval", 60)
    if (settings.anyOn           == null) device.updateSetting("anyOn",           true)
    if (settings.logEnable       == null) device.updateSetting("logEnable",       false)
    if (settings.sceneMode       == null) device.updateSetting("sceneMode",       "trigger")
    if (settings.sceneOff        == null) device.updateSetting("sceneOff",        false)

    if (logEnable) log.debug "Preferences updated"
    parent.getDeviceState(this)
    refresh()
    
}

/** Switch Commands **/

def on() {
    if (logEnable) log.info "Group (${this}) turning on"
    if (!(defaultScene?:"").trim().isEmpty()) {
        if (logEnable) log.info "Using scene to turn on group (${this})"
        activateScene(defaultScene)
    } else {
        parent.componentOn(this)
        if (sceneMode == "switch" && sceneOff) allOff()
    }
}

def off() {
    if (logEnable) log.info "Group (${this}) turning off"
    parent.componentOff(this)
    if (sceneMode == "switch") allOff()
}

/** ColorControl Commands **/

def setColor(colormap) {
    if (logEnable) log.info "Setting (${this}) mapped color: ${colormap}"
    parent.componentSetColor(this, colormap)
    if (sceneMode == "switch" && sceneOff) allOff()
}

def setHue(hue) {
    if (logEnable) log.info "Setting (${this}) hue: ${hue}"
    parent.componentSetHue(this, hue)
    if (sceneMode == "switch" && sceneOff) allOff()
}

def setSaturation(saturation) {
    if (logEnable) log.info "Setting (${this}) saturation: ${saturation}"
    parent.componentSetSaturation(this, saturation)
    if (sceneMode == "switch" && sceneOff) allOff()
}

/** ColorTemperature Commands **/

def setColorTemperature(colortemperature) {
    if (logEnable) log.info "Setting (${this}) color temp: ${colortemperature}"
    parent.componentSetColorTemperature(this, colortemperature)
    if (sceneMode == "switch" && sceneOff) allOff()
}

/** SwitchLevel Commands **/

def setLevel(level, duration=null) {
    if (logEnable) log.info "Setting (${this}) level: ${level}"
    parent.componentSetLevel(this, level, duration)
    if (sceneMode == "switch" && sceneOff) allOff()
}

/** ChangeLevel Commands **/

def startLevelChange(direction) {
    if (logEnable) log.info "Starting (${this}) level change: ${direction}"
    parent.componentStartLevelChange(this, direction)
    if (sceneMode == "switch" && sceneOff) allOff()
}

def stopLevelChange() {
    if (logEnable) log.info "Stopping (${this}) level change"
    parent.componentStopLevelChange(this)
    if (sceneMode == "switch" && sceneOff) allOff()
}

/** Refresh Commands **/
def refresh() {
    if (debug) log.debug "Bridge (${this}) refreshing"
    parent.getDeviceState(this)
    resetRefreshSchedule()
}

def autoRefresh() {
    if (autoRefesh) runIn(refreshInterval?:60, autoRefresh, [overwrite: true, misfire:"ignore"])
    refresh()
}

def resetRefreshSchedule() {
    unschedule()
    if (autoRefresh) runIn(refreshInterval?:60, autoRefresh, [overwrite: true, misfire:"ignore"])
}

def setHueProperty(name, value) {
    if (name == (anyOn?"any_on":"all_on")) {
        parent.sendChildEvent(this, "switch", value ? "on" : "off")
    } else if (name == "scene") {
        def child = getChildDevice(networkIdForScene(value))
        exclusiveOn(child)
        if (child) {
            child.unschedule()
            if (sceneMode == "trigger") child.runInMillis(400, "off")
        }
    }
}

def deviceIdNode(deviceNodeId) {
     parent.deviceIdNode(deviceNodeId)
}

def networkIdForScene(sceneId) {
    parent.networkIdForScene(deviceIdNode(device.deviceNetworkId), sceneId)
}

def activateScene(scene) {
    if (logEnable) log.info "Attempting to activate scene: ${scene}"
    def sceneId = parent.findScene(deviceIdNode(device.deviceNetworkId), scene)?.key
    if (sceneId) {
        if (logEnable) log.info "Activating scene with Hue Scene ID: ${sceneId}"
        parent.setDeviceState(this, ["scene": sceneId])
    } else {
        if (logEnable) log.warning "Cannot locate Hue scene ${scene}; verify the scene id or name is correct."
    }
}

def allOff() {
    getChildDevices().findAll { it.currentValue("switch") == "on" }.each { 
        parent.sendChildEvent(it, "switch", "off") 
        if (logEnable) log.info "Scene (${it}) turned off"
    }
}

def exclusiveOn(child) {
    if (child) {
        def dni = child.device.deviceNetworkId
        getChildDevices().each { 
            def value = (it.deviceNetworkId == dni) ? "on" : "off"
            if (it.device.currentValue("switch") != value) {
                parent.sendChildEvent(it, "switch", value) 
                if (logEnable) log.info "Scene (${it.label}) turned ${value}"
            }
        }
    }
    if (!(device.currentValue("switch") == "on")) {
        parent.sendChildEvent(this, "switch", "on")
    }
}

/*
 * Component Child Methods (used to capture actions generated on scenes)
 */

void componentOn(child) {
    if (logEnable) log.info "Scene (${child}) turning on"
    def sceniId = deviceIdNode(child.deviceNetworkId)
    parent.setDeviceState(this, ["scene":sceniId])
}

void componentOff(child) {
    if (logEnable) log.info "Scene (${child}) turning off"
    // Only change the state to off, there is not action to actually be performed.
    if (sceneMode == "switch") {
        if (child.currentValue("switch") == "on") off()
    } else {
        parent.sendChildEvent(child, "switch", "off")
    }
}

void componentRefresh(child){
    if (logEnable) log.info "Received refresh request from ${child.displayName} - ignored"
}
