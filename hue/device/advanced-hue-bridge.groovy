/**
 * Advanced Hue Bridge
 * Download: https://github.com/apwelsh/hubitat
 * Description:
 * This is a child device handler for the Advance Hue Bridge Integration App.  This device manage the hub directly for
 * actions such as refresh.
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
    input name: "autoRefresh", type: "bool", defaultValue: false, title: "Auto Refresh",        description: "Should this device support automatic refresh" 
    if (autoRefresh)
        input name: "refreshInterval", type: "number", defaultValue: 60, title: "Refresh Inteval", description: "Number of seconds to refresh the group state" 
    input name: "anyOn",       type: "bool", defaultValue: true,  title: "ANY on or ALL on",    description: "When ebabled, the group is considered on when any light is on"
    input name: "logEnable",   type: "bool", defaultValue: false, title: "Enable informational logging"
    input name: "debug",       type: "bool", defaultValue: false, title: "Enable debug logging"

}

metadata {
    definition (
        name:      "AdvancedHueBridge", 
		namespace: "apwelsh", 
		author:    "Armand Welsh", 
		importUrl: "https://raw.githubusercontent.com/apwelsh/hubitat/master/hue/device/advanced-hue-bridge.groovy") {
		
        capability "Switch"
        capability "Refresh"

    }
}


/**
 * Hubitat DTH Lifecycle Functions
 **/
def installed() {
    updated()
}

def updated() {

    if (settings.autoRefresh     == null) device.updateSetting("autoRefresh", false)
    if (settings.refreshInterval == null) device.updateSetting("refreshInterval", 30)
    if (settings.anyOn           == null) device.updateSetting("anyOn", true)
    if (settings.logEnable       == null) device.updateSetting("logEnable", false)

    if (logEnable) log.debug "Preferences updated"
	refresh()
}

/*
 * Device Capability Interface Functions
 */

/** Switch Commands **/

def on() {
    if (logEnable) log.info "Bridge (${this}) turning on"
    parent.setDeviceState(this, ["on":true])
}

def off() {
    if (logEnable) log.info "Bridge (${this}) turning off"
    parent.setDeviceState(this, ["on": false])
}


def refresh() {
    if (debug) log.debug "Bridge (${this}) refreshing"
    parent.getDeviceState(this)
    parent.getHubStatus()
    resetRefreshSchedule()
}

def autoRefresh() {
    if (autoRefresh) runIn(refreshInterval?:60, autoRefresh, [overwrite: true, misfire:"ignore"])
    refresh()
}

def resetRefreshSchedule() {
    unschedule()
    if (autoRefresh) runIn(refreshInterval?:60, autoRefresh, [overwrite: true, misfire:"ignore"])
}

def setHueProperty(name, value) {
    if (name == (anyOn?"any_on":"all_on")) {
        parent.sendChildEvent(this, "switch", value ? "on" : "off")
    } 
}

def deviceIdNode(deviceNodeId) {
     parent.deviceIdNode(deviceNodeId)
}

def networkIdForScene(sceneId) {
    parent.networkIdForScene(deviceIdNode(device.deviceNetworkId), sceneId)
}
